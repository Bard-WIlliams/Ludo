package com.example

import com.example.domain.AiDifficulty
import com.example.domain.BoardGeometry
import com.example.domain.GameMode
import com.example.domain.LudoAiAdvisor
import com.example.domain.LudoEngine
import com.example.domain.PlayerColor
import com.example.domain.PlayerSlot
import com.example.domain.TeamId
import com.example.domain.TurnPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class LudoEngineTest {

    private fun defaultFourPlayers(): List<PlayerSlot> = PlayerColor.entries.map {
        PlayerSlot(color = it, name = it.displayName, isActive = true)
    }

    @Test
    fun blueGets665EveryTimeWhenSoundIsTwelvePercent() {
        var state = LudoEngine.createGame(GameMode.FREE_FOR_ALL, defaultFourPlayers())
            .copy(activeColor = PlayerColor.BLUE, turnPhase = TurnPhase.WAITING_FOR_ROLL)

        val observedRolls = mutableListOf<Int?>()
        repeat(6) {
            state = LudoEngine.rollDice(state, isSoundTwelvePercent = true, random = Random(1))
            observedRolls += state.lastDiceValue
            state = state.copy(activeColor = PlayerColor.BLUE, turnPhase = TurnPhase.WAITING_FOR_ROLL)
        }

        assertEquals(listOf(6, 6, 5, 6, 6, 5), observedRolls)
    }

    @Test
    fun redIsNotRiggedWhenSoundIsTwelvePercent() {
        val state = LudoEngine.createGame(GameMode.FREE_FOR_ALL, defaultFourPlayers())
            .copy(activeColor = PlayerColor.RED, turnPhase = TurnPhase.WAITING_FOR_ROLL)

        val rolled = LudoEngine.rollDice(state, isSoundTwelvePercent = true, random = Random(42))
        val expectedNormal = Random(42).nextInt(1, 7)
        assertEquals(expectedNormal, rolled.lastDiceValue)
        assertEquals(0, rolled.blueEasterEggStep)
    }

    @Test
    fun secureRandomDiceCoversAllSixFacesWithinRange() {
        var state = LudoEngine.createGame(GameMode.FREE_FOR_ALL, defaultFourPlayers())
        val seen = mutableSetOf<Int>()
        repeat(300) {
            state = state.copy(activeColor = PlayerColor.RED, turnPhase = TurnPhase.WAITING_FOR_ROLL)
            state = LudoEngine.rollDice(state, isSoundTwelvePercent = false, random = null)
            val v = state.lastDiceValue!!
            assertTrue(v in 1..6)
            seen += v
        }
        assertEquals(setOf(1, 2, 3, 4, 5, 6), seen)
    }

    @Test
    fun yardTokensOnlyMoveOnSix() {
        val state = LudoEngine.createGame(GameMode.FREE_FOR_ALL, defaultFourPlayers())
        assertEquals(emptyList<Int>(), LudoEngine.getLegalTokenIds(state.tokens, PlayerColor.RED, 4))
        assertEquals(listOf(0, 1, 2, 3), LudoEngine.getLegalTokenIds(state.tokens, PlayerColor.RED, 6))
    }

    @Test
    fun captureGrantsBonusRollAndResetsOpponent() {
        val base = LudoEngine.createGame(GameMode.FREE_FOR_ALL, defaultFourPlayers())
        val tokens = base.tokens.map { token ->
            when {
                token.color == PlayerColor.RED && token.id == 0 -> token.copy(progress = 4)
                token.color == PlayerColor.GREEN && token.id == 0 -> token.copy(progress = 44)
                else -> token
            }
        }
        val state = base.copy(
            tokens = tokens,
            activeColor = PlayerColor.RED,
            lastDiceValue = 1,
            turnPhase = TurnPhase.SELECTING_TOKEN,
            legalTokenIds = listOf(0)
        )

        val afterMove = LudoEngine.applyMove(state, 0)
        val greenToken = afterMove.tokens.first { it.color == PlayerColor.GREEN && it.id == 0 }
        assertTrue(greenToken.isInYard)
        assertEquals(PlayerColor.RED, afterMove.activeColor)
        assertEquals(1, afterMove.players.first { it.color == PlayerColor.RED }.captures)
    }

    @Test
    fun team2v2SharedSafeZoneProtectsTeammatesFromCapture() {
        val base = LudoEngine.createGame(GameMode.TEAM_2V2, defaultFourPlayers())
        val tokens = base.tokens.map { token ->
            when {
                token.color == PlayerColor.RED && token.id == 0 -> token.copy(progress = 10)
                token.color == PlayerColor.GREEN && token.id == 0 -> token.copy(progress = 49)
                token.color == PlayerColor.BLUE && token.id == 0 -> token.copy(progress = 22)
                else -> token
            }
        }
        val state = base.copy(
            tokens = tokens,
            activeColor = PlayerColor.BLUE,
            lastDiceValue = 1,
            turnPhase = TurnPhase.SELECTING_TOKEN,
            legalTokenIds = listOf(0)
        )

        assertTrue(LudoEngine.isTileSharedTeamSafe(state, 10))
        val afterMove = LudoEngine.applyMove(state, 0)

        val redToken = afterMove.tokens.first { it.color == PlayerColor.RED && it.id == 0 }
        val greenToken = afterMove.tokens.first { it.color == PlayerColor.GREEN && it.id == 0 }
        assertFalse(redToken.isInYard)
        assertFalse(greenToken.isInYard)
        assertEquals(0, afterMove.players.first { it.color == PlayerColor.BLUE }.captures)
    }

    @Test
    fun team2v2RequiresBothAdjacentTeammatesToFinish() {
        val base = LudoEngine.createGame(GameMode.TEAM_2V2, defaultFourPlayers())
        val tokens = base.tokens.map { token ->
            when {
                token.color == PlayerColor.RED -> token.copy(progress = BoardGeometry.MAX_PROGRESS)
                token.color == PlayerColor.GREEN && token.id < 3 -> token.copy(progress = BoardGeometry.MAX_PROGRESS)
                token.color == PlayerColor.GREEN && token.id == 3 -> token.copy(progress = 54)
                else -> token
            }
        }
        val state = base.copy(
            tokens = tokens,
            activeColor = PlayerColor.GREEN,
            lastDiceValue = 2,
            turnPhase = TurnPhase.SELECTING_TOKEN,
            legalTokenIds = listOf(3)
        )

        val afterWin = LudoEngine.applyMove(state, 3)
        assertEquals(TurnPhase.GAME_OVER, afterWin.turnPhase)
        assertEquals(TeamId.NORTH, afterWin.winnerTeam)
    }

    @Test
    fun teammatesNeverCaptureEachOther() {
        val base = LudoEngine.createGame(GameMode.TEAM_2V2, defaultFourPlayers())
        val tokens = base.tokens.map { token ->
            when {
                token.color == PlayerColor.RED && token.id == 0 -> token.copy(progress = 5)
                token.color == PlayerColor.GREEN && token.id == 0 -> token.copy(progress = 43)
                else -> token
            }
        }
        val state = base.copy(
            tokens = tokens,
            activeColor = PlayerColor.GREEN,
            lastDiceValue = 1,
            turnPhase = TurnPhase.SELECTING_TOKEN,
            legalTokenIds = listOf(0)
        )

        val afterMove = LudoEngine.applyMove(state, 0)
        val redToken = afterMove.tokens.first { it.color == PlayerColor.RED && it.id == 0 }
        assertEquals(5, redToken.progress)
        assertEquals(0, afterMove.players.first { it.color == PlayerColor.GREEN }.captures)
    }

    @Test
    fun tacticalAiPrioritizesCaptureOverPlainAdvance() {
        val base = LudoEngine.createGame(GameMode.FREE_FOR_ALL, defaultFourPlayers())
        // Token 0 at progress 4 -> rolling 1 lands on global 5 and captures Green (progress 44 -> global 5).
        // Token 1 at progress 10 -> rolling 1 moves to empty non-safe tile 11.
        val tokens = base.tokens.map { token ->
            when {
                token.color == PlayerColor.RED && token.id == 0 -> token.copy(progress = 4)
                token.color == PlayerColor.RED && token.id == 1 -> token.copy(progress = 10)
                token.color == PlayerColor.GREEN && token.id == 0 -> token.copy(progress = 44)
                else -> token
            }
        }
        val state = base.copy(
            tokens = tokens,
            activeColor = PlayerColor.RED,
            lastDiceValue = 1,
            turnPhase = TurnPhase.SELECTING_TOKEN,
            legalTokenIds = listOf(0, 1)
        )

        val chosen = LudoAiAdvisor.chooseToken(state, AiDifficulty.TACTICAL)
        assertEquals(0, chosen)
    }
}
