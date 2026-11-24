package com.example.domain

import java.security.SecureRandom
import kotlin.random.Random

object LudoEngine {

    private val BLUE_RIGGED_SEQUENCE = intArrayOf(6, 6, 5)
    private val secureRandom = SecureRandom()

    fun createGame(
        mode: GameMode,
        playerConfigs: List<PlayerSlot>
    ): GameState {
        val normalizedPlayers = PlayerColor.entries.map { color ->
            val configured = playerConfigs.find { it.color == color }
            when (mode) {
                GameMode.TEAM_2V2 -> PlayerSlot(
                    color = color,
                    name = configured?.name?.ifBlank { color.displayName } ?: color.displayName,
                    isActive = true,
                    isAi = configured?.isAi ?: false,
                    aiDifficulty = configured?.aiDifficulty ?: AiDifficulty.TACTICAL
                )
                GameMode.FREE_FOR_ALL -> PlayerSlot(
                    color = color,
                    name = configured?.name?.ifBlank { color.displayName } ?: color.displayName,
                    isActive = configured?.isActive ?: false,
                    isAi = configured?.isAi ?: false,
                    aiDifficulty = configured?.aiDifficulty ?: AiDifficulty.TACTICAL
                )
            }
        }

        val activePlayers = normalizedPlayers.filter { it.isActive }
        require(activePlayers.size >= 2) { "At least 2 active players are required" }

        val initialTokens = activePlayers.flatMap { slot ->
            (0..3).map { id -> Token(color = slot.color, id = id, progress = -1) }
        }

        val firstSlot = activePlayers.first()
        val firstActionPrompt = if (firstSlot.isAi) {
            "${firstSlot.name} is thinking…"
        } else {
            "${firstSlot.name}'s turn — tap die to roll"
        }

        return GameState(
            mode = mode,
            players = normalizedPlayers,
            tokens = initialTokens,
            activeColor = firstSlot.color,
            lastDiceValue = null,
            turnPhase = TurnPhase.WAITING_FOR_ROLL,
            legalTokenIds = emptyList(),
            turnCount = 1,
            blueEasterEggStep = 0,
            statusMessage = firstActionPrompt
        )
    }

    /**
     * Rolls the die using a cryptographically strong OS entropy RNG (SecureRandom)
     * unless a deterministic Random is explicitly supplied for testing, or the secret
     * 12% sound-volume Blue easter egg is active.
     */
    fun rollDice(
        state: GameState,
        isSoundTwelvePercent: Boolean,
        random: Random? = null
    ): GameState {
        if (state.turnPhase != TurnPhase.WAITING_FOR_ROLL) return state

        val useBlueEasterEgg = state.activeColor == PlayerColor.BLUE && isSoundTwelvePercent
        val rolled = if (useBlueEasterEgg) {
            BLUE_RIGGED_SEQUENCE[state.blueEasterEggStep % BLUE_RIGGED_SEQUENCE.size]
        } else {
            random?.nextInt(1, 7) ?: (secureRandom.nextInt(6) + 1)
        }
        val nextBlueStep = if (useBlueEasterEgg) {
            (state.blueEasterEggStep + 1) % BLUE_RIGGED_SEQUENCE.size
        } else {
            state.blueEasterEggStep
        }

        val legalIds = getLegalTokenIds(state.tokens, state.activeColor, rolled)
        val activeSlot = state.players.firstOrNull { it.color == state.activeColor }
        val activeName = activeSlot?.name ?: state.activeColor.displayName

        return if (legalIds.isEmpty()) {
            state.copy(
                lastDiceValue = rolled,
                turnPhase = TurnPhase.SELECTING_TOKEN,
                legalTokenIds = emptyList(),
                blueEasterEggStep = nextBlueStep,
                statusMessage = "$activeName rolled $rolled — no legal moves"
            )
        } else {
            val prompt = if (activeSlot?.isAi == true) {
                "$activeName rolled $rolled"
            } else {
                "$activeName rolled $rolled — choose a token"
            }
            state.copy(
                lastDiceValue = rolled,
                turnPhase = TurnPhase.SELECTING_TOKEN,
                legalTokenIds = legalIds,
                blueEasterEggStep = nextBlueStep,
                statusMessage = prompt
            )
        }
    }

    fun getLegalTokenIds(
        tokens: List<Token>,
        color: PlayerColor,
        diceValue: Int
    ): List<Int> {
        return tokens
            .filter { it.color == color }
            .filter { token ->
                when {
                    token.isFinished -> false
                    token.isInYard -> diceValue == 6
                    else -> token.progress + diceValue <= BoardGeometry.MAX_PROGRESS
                }
            }
            .map { it.id }
    }

    fun passTurnIfNoMoves(state: GameState): GameState {
        if (state.turnPhase != TurnPhase.SELECTING_TOKEN || state.legalTokenIds.isNotEmpty()) {
            return state
        }
        val nextColor = nextActiveColor(state, state.activeColor)
        val nextSlot = state.players.firstOrNull { it.color == nextColor }
        val nextName = nextSlot?.name ?: nextColor.displayName
        return state.copy(
            activeColor = nextColor,
            lastDiceValue = null,
            turnPhase = TurnPhase.WAITING_FOR_ROLL,
            legalTokenIds = emptyList(),
            turnCount = state.turnCount + 1,
            statusMessage = if (nextSlot?.isAi == true) {
                "$nextName is thinking…"
            } else {
                "$nextName's turn — tap die to roll"
            }
        )
    }

    fun applyMove(state: GameState, tokenId: Int): GameState {
        if (state.turnPhase != TurnPhase.SELECTING_TOKEN) return state
        val dice = state.lastDiceValue ?: return state
        if (tokenId !in state.legalTokenIds) return state

        val movingToken = state.tokens.firstOrNull {
            it.color == state.activeColor && it.id == tokenId
        } ?: return state

        val fromProgress = movingToken.progress
        val toProgress = if (movingToken.isInYard) 0 else fromProgress + dice
        val movedToken = movingToken.copy(progress = toProgress)

        val updatedTokens = state.tokens.map {
            if (it.color == movingToken.color && it.id == movingToken.id) movedToken else it
        }.toMutableList()

        // Capture evaluation on perimeter
        val capturedList = mutableListOf<Token>()
        val targetPerimeter = movedToken.globalPerimeterIndex()
        if (targetPerimeter != null && targetPerimeter !in BoardGeometry.SAFE_PERIMETER_INDICES) {
            val tokensAtTarget = updatedTokens.filter {
                it.globalPerimeterIndex() == targetPerimeter && it.color != movedToken.color
            }

            // In 2v2 mode, teammates never capture each other, and a tile occupied by BOTH teammates
            // of an opposing team forms a Shared Safe Zone that shields them from capture.
            val enemies = if (state.mode == GameMode.TEAM_2V2) {
                tokensAtTarget.filter { it.color.teamId != movedToken.color.teamId }
            } else {
                tokensAtTarget
            }

            val enemyColorsPresent = enemies.map { it.color }.toSet()
            val isSharedTeamSafeZone = state.mode == GameMode.TEAM_2V2 && enemyColorsPresent.size >= 2

            if (!isSharedTeamSafeZone && enemies.isNotEmpty()) {
                enemies.forEach { enemy ->
                    val idx = updatedTokens.indexOfFirst {
                        it.color == enemy.color && it.id == enemy.id
                    }
                    if (idx >= 0) {
                        capturedList += enemy
                        updatedTokens[idx] = enemy.copy(progress = -1)
                    }
                }
            }
        }

        val updatedPlayers = if (capturedList.isNotEmpty()) {
            state.players.map { slot ->
                if (slot.color == state.activeColor) {
                    slot.copy(captures = slot.captures + capturedList.size)
                } else {
                    slot
                }
            }
        } else {
            state.players
        }

        val reachedHome = toProgress == BoardGeometry.MAX_PROGRESS
        val bonusRoll = (dice == 6) || capturedList.isNotEmpty() || reachedHome

        val moveEvent = MoveEvent(
            moverColor = state.activeColor,
            tokenId = tokenId,
            fromProgress = fromProgress,
            toProgress = toProgress,
            capturedTokens = capturedList,
            reachedHome = reachedHome,
            grantedBonusRoll = bonusRoll
        )

        // Check victory
        val winningTeam = checkTeamWinner(state.mode, updatedTokens)
        val winningColor = checkSingleWinner(state.mode, updatedPlayers, updatedTokens)

        if (winningTeam != null || winningColor != null) {
            val winnerLabel = when {
                winningTeam != null -> "${winningTeam.displayName} wins!"
                winningColor != null -> {
                    val winnerName = updatedPlayers.firstOrNull { it.color == winningColor }?.name
                        ?: winningColor.displayName
                    "$winnerName wins!"
                }
                else -> "Game Over"
            }
            return state.copy(
                players = updatedPlayers,
                tokens = updatedTokens,
                turnPhase = TurnPhase.GAME_OVER,
                legalTokenIds = emptyList(),
                winnerColor = winningColor,
                winnerTeam = winningTeam,
                statusMessage = winnerLabel,
                lastMoveEvent = moveEvent,
                moveSequenceId = state.moveSequenceId + 1
            )
        }

        // Determine next turn
        val activeStillHasMoves = updatedTokens.any {
            it.color == state.activeColor && !it.isFinished
        }
        val keepTurn = bonusRoll && activeStillHasMoves
        val nextColor = if (keepTurn) state.activeColor else nextActiveColor(
            state.copy(players = updatedPlayers, tokens = updatedTokens),
            state.activeColor
        )
        val nextSlot = updatedPlayers.firstOrNull { it.color == nextColor }
        val nextName = nextSlot?.name ?: nextColor.displayName

        val status = when {
            keepTurn && capturedList.isNotEmpty() -> "$nextName captured! Bonus roll"
            keepTurn && reachedHome -> "$nextName reached home! Bonus roll"
            keepTurn && dice == 6 -> "$nextName rolled a 6 — roll again"
            nextSlot?.isAi == true -> "$nextName is thinking…"
            else -> "$nextName's turn — tap die to roll"
        }

        return state.copy(
            players = updatedPlayers,
            tokens = updatedTokens,
            activeColor = nextColor,
            lastDiceValue = null,
            turnPhase = TurnPhase.WAITING_FOR_ROLL,
            legalTokenIds = emptyList(),
            turnCount = if (keepTurn) state.turnCount else state.turnCount + 1,
            statusMessage = status,
            lastMoveEvent = moveEvent,
            moveSequenceId = state.moveSequenceId + 1
        )
    }

    fun isTileSharedTeamSafe(state: GameState, perimeterIndex: Int): Boolean {
        if (state.mode != GameMode.TEAM_2V2) return false
        val colorsOnTile = state.tokens
            .filter { it.globalPerimeterIndex() == perimeterIndex }
            .map { it.color }
            .toSet()
        val northProtected = PlayerColor.RED in colorsOnTile && PlayerColor.GREEN in colorsOnTile
        val southProtected = PlayerColor.BLUE in colorsOnTile && PlayerColor.YELLOW in colorsOnTile
        return northProtected || southProtected
    }

    private fun checkTeamWinner(mode: GameMode, tokens: List<Token>): TeamId? {
        if (mode != GameMode.TEAM_2V2) return null
        return TeamId.entries.firstOrNull { team ->
            val teamTokens = tokens.filter { it.color.teamId == team }
            teamTokens.size == 8 && teamTokens.all { it.isFinished }
        }
    }

    private fun checkSingleWinner(
        mode: GameMode,
        players: List<PlayerSlot>,
        tokens: List<Token>
    ): PlayerColor? {
        if (mode != GameMode.FREE_FOR_ALL) return null
        return players
            .filter { it.isActive }
            .map { it.color }
            .firstOrNull { color ->
                val colorTokens = tokens.filter { it.color == color }
                colorTokens.size == 4 && colorTokens.all { it.isFinished }
            }
    }

    private fun nextActiveColor(state: GameState, current: PlayerColor): PlayerColor {
        val ordered = PlayerColor.entries
        val startIdx = ordered.indexOf(current)
        for (offset in 1..ordered.size) {
            val candidate = ordered[(startIdx + offset) % ordered.size]
            val slot = state.players.firstOrNull { it.color == candidate }
            val hasUnfinished = state.tokens.any { it.color == candidate && !it.isFinished }
            if (slot?.isActive == true && hasUnfinished) {
                return candidate
            }
        }
        return current
    }
}
