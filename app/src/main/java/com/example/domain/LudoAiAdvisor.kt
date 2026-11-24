package com.example.domain

import java.security.SecureRandom

object LudoAiAdvisor {

    private val rng = SecureRandom()

    fun chooseToken(
        state: GameState,
        difficulty: AiDifficulty
    ): Int? {
        val legalIds = state.legalTokenIds
        if (legalIds.isEmpty()) return null
        if (legalIds.size == 1) return legalIds.first()

        val dice = state.lastDiceValue ?: return legalIds.first()

        return when (difficulty) {
            AiDifficulty.CASUAL -> chooseCasual(state, legalIds, dice)
            AiDifficulty.TACTICAL -> chooseHeuristic(state, legalIds, dice, deepThreatScan = false)
            AiDifficulty.MASTER -> chooseHeuristic(state, legalIds, dice, deepThreatScan = true)
        }
    }

    private fun chooseCasual(state: GameState, legalIds: List<Int>, dice: Int): Int {
        // Casual: prefers finishing or deploying from yard, otherwise picks randomly
        val tokens = state.tokens.filter { it.color == state.activeColor && it.id in legalIds }
        val finisher = tokens.firstOrNull { !it.isInYard && it.progress + dice == BoardGeometry.MAX_PROGRESS }
        if (finisher != null) return finisher.id
        val yardToken = tokens.firstOrNull { it.isInYard }
        if (yardToken != null && rng.nextBoolean()) return yardToken.id
        return legalIds[rng.nextInt(legalIds.size)]
    }

    private fun chooseHeuristic(
        state: GameState,
        legalIds: List<Int>,
        dice: Int,
        deepThreatScan: Boolean
    ): Int {
        val myColor = state.activeColor
        val myTokens = state.tokens.filter { it.color == myColor && it.id in legalIds }

        val scored = myTokens.map { token ->
            val score = evaluateMove(state, token, dice, deepThreatScan)
            // Small jitter for ties so games feel natural
            token.id to (score + rng.nextInt(4))
        }
        return scored.maxByOrNull { it.second }?.first ?: legalIds.first()
    }

    private fun evaluateMove(
        state: GameState,
        token: Token,
        dice: Int,
        deepThreatScan: Boolean
    ): Int {
        val from = token.progress
        val to = if (token.isInYard) 0 else from + dice
        var score = 0

        // 1. Exact home finish
        if (to == BoardGeometry.MAX_PROGRESS) {
            score += 140
        }

        // 2. Entering private safe home stretch
        if (from in 0..50 && to in 51..55) {
            score += 95
        }

        // 3. Deploying a token out of the yard on a 6
        if (token.isInYard && to == 0) {
            score += 70
        }

        // 4. Perimeter capture / safe-zone / threat evaluation
        val fromGlobal = token.globalPerimeterIndex()
        val toGlobal = if (to in 0..50) {
            (token.color.startPerimeterIndex + to) % BoardGeometry.PERIMETER_SIZE
        } else {
            null
        }

        if (toGlobal != null) {
            val othersAtTarget = state.tokens.filter {
                it.globalPerimeterIndex() == toGlobal && it.color != token.color
            }

            if (toGlobal !in BoardGeometry.SAFE_PERIMETER_INDICES) {
                val enemies = if (state.mode == GameMode.TEAM_2V2) {
                    othersAtTarget.filter { it.color.teamId != token.color.teamId }
                } else {
                    othersAtTarget
                }
                val isSharedEnemyZone = state.mode == GameMode.TEAM_2V2 &&
                    enemies.map { it.color }.toSet().size >= 2

                if (!isSharedEnemyZone && enemies.isNotEmpty()) {
                    // Big reward for capture (plus bonus roll!)
                    val leadBonus = if (deepThreatScan) enemies.maxOf { it.progress } else 0
                    score += 130 + enemies.size * 30 + leadBonus
                }
            } else {
                // Universal safe star / start square
                score += 48
            }

            // 2v2 Shared Safe Zone creation with teammate
            if (state.mode == GameMode.TEAM_2V2) {
                val partnerAtTarget = othersAtTarget.any { it.color.teamId == token.color.teamId }
                if (partnerAtTarget) {
                    score += 68
                }
            }

            // Threat scanning on landing tile
            if (toGlobal !in BoardGeometry.SAFE_PERIMETER_INDICES) {
                val threatsAfter = countThreatsBehind(state, token.color, toGlobal)
                score -= threatsAfter * if (deepThreatScan) 26 else 14
            }
        }

        // 5. Threat escape bonus & safe-spot sacrifice penalty
        if (fromGlobal != null) {
            val wasSafe = fromGlobal in BoardGeometry.SAFE_PERIMETER_INDICES ||
                (state.mode == GameMode.TEAM_2V2 && LudoEngine.isTileSharedTeamSafe(state, fromGlobal))
            if (wasSafe) {
                score -= if (deepThreatScan) 24 else 14
            } else {
                val threatsBefore = countThreatsBehind(state, token.color, fromGlobal)
                if (threatsBefore > 0) {
                    score += threatsBefore * if (deepThreatScan) 34 else 20
                }
            }
        }

        // 6. Prefer advancing lead tokens steadily when safe
        if (to > 0) {
            score += (to / 3)
        }

        return score
    }

    private fun countThreatsBehind(
        state: GameState,
        myColor: PlayerColor,
        perimeterIndex: Int
    ): Int {
        return state.tokens.count { other ->
            if (other.color == myColor) return@count false
            if (state.mode == GameMode.TEAM_2V2 && other.color.teamId == myColor.teamId) {
                return@count false
            }
            val otherIdx = other.globalPerimeterIndex() ?: return@count false
            val dist = (perimeterIndex - otherIdx + BoardGeometry.PERIMETER_SIZE) % BoardGeometry.PERIMETER_SIZE
            dist in 1..6 && other.progress + dist <= 50
        }
    }
}
