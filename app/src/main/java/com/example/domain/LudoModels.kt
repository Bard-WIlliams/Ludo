package com.example.domain

import com.squareup.moshi.JsonClass

enum class PlayerColor(
    val displayName: String,
    val startPerimeterIndex: Int,
    val teamId: TeamId
) {
    RED("Red", 0, TeamId.NORTH),
    GREEN("Green", 13, TeamId.NORTH),
    YELLOW("Yellow", 26, TeamId.SOUTH),
    BLUE("Blue", 39, TeamId.SOUTH);

    fun partner(): PlayerColor = when (this) {
        RED -> GREEN
        GREEN -> RED
        YELLOW -> BLUE
        BLUE -> YELLOW
    }
}

enum class TeamId(val displayName: String) {
    NORTH("Team North"),
    SOUTH("Team South")
}

enum class GameMode {
    FREE_FOR_ALL,
    TEAM_2V2
}

enum class AiDifficulty(val displayName: String) {
    CASUAL("Casual"),
    TACTICAL("Tactical"),
    MASTER("Master")
}

enum class TurnPhase {
    WAITING_FOR_ROLL,
    SELECTING_TOKEN,
    GAME_OVER
}

@JsonClass(generateAdapter = true)
data class Token(
    val color: PlayerColor,
    val id: Int, // 0..3
    val progress: Int = -1 // -1 = yard, 0..50 = perimeter, 51..55 = home stretch, 56 = finished
) {
    val isInYard: Boolean get() = progress == -1
    val isFinished: Boolean get() = progress == 56
    val isOnPerimeter: Boolean get() = progress in 0..50

    fun globalPerimeterIndex(): Int? {
        if (!isOnPerimeter) return null
        return (color.startPerimeterIndex + progress) % BoardGeometry.PERIMETER_SIZE
    }
}

@JsonClass(generateAdapter = true)
data class PlayerSlot(
    val color: PlayerColor,
    val name: String,
    val isActive: Boolean = true,
    val captures: Int = 0,
    val isAi: Boolean = false,
    val aiDifficulty: AiDifficulty = AiDifficulty.TACTICAL
)

@JsonClass(generateAdapter = true)
data class MoveEvent(
    val moverColor: PlayerColor,
    val tokenId: Int,
    val fromProgress: Int,
    val toProgress: Int,
    val capturedTokens: List<Token> = emptyList(),
    val reachedHome: Boolean = false,
    val grantedBonusRoll: Boolean = false
)

@JsonClass(generateAdapter = true)
data class GameState(
    val mode: GameMode = GameMode.FREE_FOR_ALL,
    val players: List<PlayerSlot> = emptyList(),
    val tokens: List<Token> = emptyList(),
    val activeColor: PlayerColor = PlayerColor.RED,
    val lastDiceValue: Int? = null,
    val turnPhase: TurnPhase = TurnPhase.WAITING_FOR_ROLL,
    val legalTokenIds: List<Int> = emptyList(),
    val winnerColor: PlayerColor? = null,
    val winnerTeam: TeamId? = null,
    val turnCount: Int = 1,
    val blueEasterEggStep: Int = 0,
    val statusMessage: String = "Roll the die to begin",
    val lastMoveEvent: MoveEvent? = null,
    val moveSequenceId: Long = 0L
)

data class GridCoord(val col: Float, val row: Float)

object BoardGeometry {
    const val PERIMETER_SIZE = 52
    const val MAX_PROGRESS = 56

    val SAFE_PERIMETER_INDICES = setOf(0, 8, 13, 21, 26, 34, 39, 47)
    val STAR_SAFE_INDICES = setOf(8, 21, 34, 47)
    val START_SAFE_INDICES = setOf(0, 13, 26, 39)

    // 52 perimeter squares clockwise starting at RED start (1, 6)
    val PERIMETER_COORDS: List<GridCoord> = listOf(
        // 0..4: Red arm top row moving right
        GridCoord(1f, 6f), GridCoord(2f, 6f), GridCoord(3f, 6f), GridCoord(4f, 6f), GridCoord(5f, 6f),
        // 5..10: Top arm left column moving up
        GridCoord(6f, 5f), GridCoord(6f, 4f), GridCoord(6f, 3f), GridCoord(6f, 2f), GridCoord(6f, 1f), GridCoord(6f, 0f),
        // 11..12: Top bridge
        GridCoord(7f, 0f), GridCoord(8f, 0f),
        // 13..17: Top arm right column moving down (13 = GREEN START)
        GridCoord(8f, 1f), GridCoord(8f, 2f), GridCoord(8f, 3f), GridCoord(8f, 4f), GridCoord(8f, 5f),
        // 18..23: Right arm top row moving right
        GridCoord(9f, 6f), GridCoord(10f, 6f), GridCoord(11f, 6f), GridCoord(12f, 6f), GridCoord(13f, 6f), GridCoord(14f, 6f),
        // 24..25: Right bridge
        GridCoord(14f, 7f), GridCoord(14f, 8f),
        // 26..30: Right arm bottom row moving left (26 = YELLOW START)
        GridCoord(13f, 8f), GridCoord(12f, 8f), GridCoord(11f, 8f), GridCoord(10f, 8f), GridCoord(9f, 8f),
        // 31..36: Bottom arm right column moving down
        GridCoord(8f, 9f), GridCoord(8f, 10f), GridCoord(8f, 11f), GridCoord(8f, 12f), GridCoord(8f, 13f), GridCoord(8f, 14f),
        // 37..38: Bottom bridge
        GridCoord(7f, 14f), GridCoord(6f, 14f),
        // 39..43: Bottom arm left column moving up (39 = BLUE START)
        GridCoord(6f, 13f), GridCoord(6f, 12f), GridCoord(6f, 11f), GridCoord(6f, 10f), GridCoord(6f, 9f),
        // 44..49: Left arm bottom row moving left
        GridCoord(5f, 8f), GridCoord(4f, 8f), GridCoord(3f, 8f), GridCoord(2f, 8f), GridCoord(1f, 8f), GridCoord(0f, 8f),
        // 50..51: Left bridge
        GridCoord(0f, 7f), GridCoord(0f, 6f)
    )

    private val HOME_STRETCH: Map<PlayerColor, List<GridCoord>> = mapOf(
        PlayerColor.RED to listOf(
            GridCoord(1f, 7f), GridCoord(2f, 7f), GridCoord(3f, 7f), GridCoord(4f, 7f), GridCoord(5f, 7f)
        ),
        PlayerColor.GREEN to listOf(
            GridCoord(7f, 1f), GridCoord(7f, 2f), GridCoord(7f, 3f), GridCoord(7f, 4f), GridCoord(7f, 5f)
        ),
        PlayerColor.YELLOW to listOf(
            GridCoord(13f, 7f), GridCoord(12f, 7f), GridCoord(11f, 7f), GridCoord(10f, 7f), GridCoord(9f, 7f)
        ),
        PlayerColor.BLUE to listOf(
            GridCoord(7f, 13f), GridCoord(7f, 12f), GridCoord(7f, 11f), GridCoord(7f, 10f), GridCoord(7f, 9f)
        )
    )

    private val FINISH_COORDS: Map<PlayerColor, GridCoord> = mapOf(
        PlayerColor.RED to GridCoord(6.25f, 7f),
        PlayerColor.GREEN to GridCoord(7f, 6.25f),
        PlayerColor.YELLOW to GridCoord(7.75f, 7f),
        PlayerColor.BLUE to GridCoord(7f, 7.75f)
    )

    private val YARD_POCKETS: Map<PlayerColor, List<GridCoord>> = mapOf(
        PlayerColor.RED to listOf(
            GridCoord(1.5f, 1.5f), GridCoord(3.5f, 1.5f),
            GridCoord(1.5f, 3.5f), GridCoord(3.5f, 3.5f)
        ),
        PlayerColor.GREEN to listOf(
            GridCoord(10.5f, 1.5f), GridCoord(12.5f, 1.5f),
            GridCoord(10.5f, 3.5f), GridCoord(12.5f, 3.5f)
        ),
        PlayerColor.YELLOW to listOf(
            GridCoord(10.5f, 10.5f), GridCoord(12.5f, 10.5f),
            GridCoord(10.5f, 12.5f), GridCoord(12.5f, 12.5f)
        ),
        PlayerColor.BLUE to listOf(
            GridCoord(1.5f, 10.5f), GridCoord(3.5f, 10.5f),
            GridCoord(1.5f, 12.5f), GridCoord(3.5f, 12.5f)
        )
    )

    fun coordForToken(color: PlayerColor, tokenId: Int, progress: Int): GridCoord {
        return when {
            progress < 0 -> YARD_POCKETS.getValue(color)[tokenId.coerceIn(0, 3)]
            progress in 0..50 -> {
                val idx = (color.startPerimeterIndex + progress) % PERIMETER_SIZE
                PERIMETER_COORDS[idx]
            }
            progress in 51..55 -> HOME_STRETCH.getValue(color)[progress - 51]
            else -> FINISH_COORDS.getValue(color)
        }
    }
}
