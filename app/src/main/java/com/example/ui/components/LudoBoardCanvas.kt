package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.BoardGeometry
import com.example.domain.GameMode
import com.example.domain.GameState
import com.example.domain.GridCoord
import com.example.domain.LudoEngine
import com.example.domain.PlayerColor
import com.example.domain.Token
import com.example.domain.TurnPhase
import com.example.ui.theme.BirchLinen
import com.example.ui.theme.ChalkWhite
import com.example.ui.theme.CharcoalInk
import com.example.ui.theme.DarkBirch
import com.example.ui.theme.DarkOat
import com.example.ui.theme.DarkStoneHairline
import com.example.ui.theme.MutedAsh
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.NordicBlue
import com.example.ui.theme.NordicGreen
import com.example.ui.theme.NordicRed
import com.example.ui.theme.NordicYellow
import com.example.ui.theme.SoftOat
import com.example.ui.theme.StoneHairline
import com.example.ui.theme.WarmSand
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@Composable
fun LudoBoardCanvas(
    gameState: GameState,
    animatingPositions: Map<Pair<PlayerColor, Int>, GridCoord>,
    canInteract: Boolean,
    onTokenSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val bezelColor = if (isDark) DarkOat else SoftOat
    val boardSurface = if (isDark) DarkBirch else ChalkWhite
    val gridLineColor = if (isDark) DarkStoneHairline else StoneHairline
    val inkColor = if (isDark) ChalkWhite else CharcoalInk
    val starColor = if (isDark) MutedAsh else MutedSlate

    val infiniteTransition = rememberInfiniteTransition(label = "legalTokenPulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.24f,
        animationSpec = infiniteRepeatable(
            animation = tween(650),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tokenRingPulse"
    )

    val visualTokens = remember(gameState.tokens, animatingPositions) {
        computeTokenRenderList(gameState.tokens, animatingPositions)
    }

    val sharedSafeIndices = remember(gameState.tokens, gameState.mode) {
        if (gameState.mode == GameMode.TEAM_2V2) {
            (0 until BoardGeometry.PERIMETER_SIZE).filter { idx ->
                LudoEngine.isTileSharedTeamSafe(gameState, idx)
            }.toSet()
        } else {
            emptySet()
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .testTag("ludo_board_canvas"),
        shape = RoundedCornerShape(24.dp),
        color = bezelColor,
        border = BorderStroke(1.5.dp, gridLineColor),
        shadowElevation = 6.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(7.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(18.dp),
                color = boardSurface,
                border = BorderStroke(1.dp, gridLineColor)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp)
                        .pointerInput(gameState, visualTokens, canInteract) {
                            detectTapGestures { tapOffset ->
                                if (!canInteract || gameState.turnPhase != TurnPhase.SELECTING_TOKEN) {
                                    return@detectTapGestures
                                }
                                val cell = size.width / 15f
                                val legalIds = gameState.legalTokenIds.toSet()
                                val candidates = visualTokens.filter {
                                    it.token.color == gameState.activeColor && it.token.id in legalIds
                                }
                                if (candidates.isEmpty()) return@detectTapGestures

                                val best = candidates.minByOrNull { item ->
                                    val cx = (item.coord.col + 0.5f) * cell
                                    val cy = (item.coord.row + 0.5f) * cell
                                    hypot(tapOffset.x - cx, tapOffset.y - cy)
                                } ?: return@detectTapGestures

                                val bestCx = (best.coord.col + 0.5f) * cell
                                val bestCy = (best.coord.row + 0.5f) * cell
                                val dist = hypot(tapOffset.x - bestCx, tapOffset.y - bestCy)
                                if (dist <= cell * 1.15f) {
                                    onTokenSelected(best.token.id)
                                }
                            }
                        }
                ) {
                    val boardPx = size.minDimension
                    val cell = boardPx / 15f

                    // 1. Track Grid Cells (15x15)
                    drawTrackBackground(
                        cell = cell,
                        isDark = isDark,
                        gridLineColor = gridLineColor,
                        starColor = starColor
                    )

                    // 2. Four 6x6 Corner Yards with active-turn subtle highlight
                    drawCornerYard(0, 0, NordicRed, boardSurface, gridLineColor, cell, gameState.activeColor == PlayerColor.RED)
                    drawCornerYard(9, 0, NordicGreen, boardSurface, gridLineColor, cell, gameState.activeColor == PlayerColor.GREEN)
                    drawCornerYard(9, 9, NordicYellow, boardSurface, gridLineColor, cell, gameState.activeColor == PlayerColor.YELLOW)
                    drawCornerYard(0, 9, NordicBlue, boardSurface, gridLineColor, cell, gameState.activeColor == PlayerColor.BLUE)

                    // 3. Center 3x3 Finish Triangles + Center Medallion
                    drawCenterHomeTriangles(cell = cell, borderColor = gridLineColor, centerFill = boardSurface)

                    // 4. 2v2 Adjacent-Corner Team Bridge Markers
                    if (gameState.mode == GameMode.TEAM_2V2) {
                        drawTeamPairIndicators(cell = cell)
                    }

                    // 5. Shared Safe Zone Halos (2v2 teammate-protected perimeter squares)
                    sharedSafeIndices.forEach { perimeterIdx ->
                        val coord = BoardGeometry.PERIMETER_COORDS[perimeterIdx]
                        val cx = (coord.col + 0.5f) * cell
                        val cy = (coord.row + 0.5f) * cell
                        drawCircle(
                            color = NordicYellow.copy(alpha = 0.25f),
                            radius = cell * 0.46f,
                            center = Offset(cx, cy)
                        )
                        drawCircle(
                            color = NordicYellow,
                            radius = cell * 0.46f,
                            center = Offset(cx, cy),
                            style = Stroke(width = cell * 0.075f)
                        )
                    }

                    // 6. Tactile 3D Ceramic Tokens
                    val legalIds = gameState.legalTokenIds.toSet()
                    val sortedTokens = visualTokens.sortedBy {
                        val isLegal = canInteract &&
                            gameState.turnPhase == TurnPhase.SELECTING_TOKEN &&
                            it.token.color == gameState.activeColor &&
                            it.token.id in legalIds
                        if (isLegal) 1 else 0
                    }

                    sortedTokens.forEach { item ->
                        val isLegal = canInteract &&
                            gameState.turnPhase == TurnPhase.SELECTING_TOKEN &&
                            item.token.color == gameState.activeColor &&
                            item.token.id in legalIds

                        val cx = (item.coord.col + 0.5f) * cell
                        val cy = (item.coord.row + 0.5f) * cell - (if (isLegal) cell * 0.05f else 0f)
                        val baseRadius = cell * if (item.clusterSize > 1) 0.28f else 0.37f
                        val tokenColor = when (item.token.color) {
                            PlayerColor.RED -> NordicRed
                            PlayerColor.GREEN -> NordicGreen
                            PlayerColor.YELLOW -> NordicYellow
                            PlayerColor.BLUE -> NordicBlue
                        }

                        if (isLegal) {
                            drawCircle(
                                color = tokenColor.copy(alpha = 0.28f),
                                radius = baseRadius * 1.50f * pulse,
                                center = Offset(cx, cy)
                            )
                            drawCircle(
                                color = inkColor.copy(alpha = 0.85f),
                                radius = baseRadius * 1.28f * pulse,
                                center = Offset(cx, cy),
                                style = Stroke(width = cell * 0.065f)
                            )
                        }

                        // Soft tabletop drop shadow
                        drawCircle(
                            color = Color.Black.copy(alpha = 0.22f),
                            radius = baseRadius * 1.02f,
                            center = Offset(cx, cy + cell * 0.065f)
                        )

                        // Outer ceramic base rim
                        drawCircle(
                            color = ChalkWhite,
                            radius = baseRadius,
                            center = Offset(cx, cy)
                        )

                        // Shaded ceramic dome body
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    tokenColor.copy(alpha = 0.88f),
                                    tokenColor
                                ),
                                center = Offset(cx - baseRadius * 0.24f, cy - baseRadius * 0.24f),
                                radius = baseRadius * 1.15f
                            ),
                            radius = baseRadius * 0.83f,
                            center = Offset(cx, cy)
                        )

                        // Specular top-left highlight & inner architectural ring
                        drawCircle(
                            color = ChalkWhite.copy(alpha = 0.52f),
                            radius = baseRadius * 0.44f,
                            center = Offset(cx, cy),
                            style = Stroke(width = baseRadius * 0.11f)
                        )
                        drawCircle(
                            color = ChalkWhite.copy(alpha = 0.42f),
                            radius = baseRadius * 0.18f,
                            center = Offset(cx - baseRadius * 0.26f, cy - baseRadius * 0.26f)
                        )
                    }
                }
            }
        }
    }
}

private data class RenderTokenItem(
    val token: Token,
    val coord: GridCoord,
    val clusterSize: Int
)

private fun computeTokenRenderList(
    tokens: List<Token>,
    animatingPositions: Map<Pair<PlayerColor, Int>, GridCoord>
): List<RenderTokenItem> {
    val rawCoords = tokens.map { token ->
        val coord = animatingPositions[token.color to token.id]
            ?: BoardGeometry.coordForToken(token.color, token.id, token.progress)
        token to coord
    }

    val grouped = rawCoords.groupBy { (_, coord) ->
        Pair((coord.col * 10).toInt(), (coord.row * 10).toInt())
    }

    val result = mutableListOf<RenderTokenItem>()
    grouped.values.forEach { cluster ->
        if (cluster.size == 1) {
            val (token, coord) = cluster.first()
            result += RenderTokenItem(token, coord, 1)
        } else {
            val offsets = listOf(
                Pair(-0.17f, -0.17f),
                Pair(0.17f, -0.17f),
                Pair(-0.17f, 0.17f),
                Pair(0.17f, 0.17f),
                Pair(0f, -0.22f),
                Pair(0f, 0.22f),
                Pair(-0.22f, 0f),
                Pair(0.22f, 0f)
            )
            cluster.forEachIndexed { idx, (token, coord) ->
                val (dx, dy) = offsets[idx % offsets.size]
                result += RenderTokenItem(
                    token = token,
                    coord = GridCoord(coord.col + dx, coord.row + dy),
                    clusterSize = cluster.size
                )
            }
        }
    }
    return result
}

private fun DrawScope.drawTrackBackground(
    cell: Float,
    isDark: Boolean,
    gridLineColor: Color,
    starColor: Color
) {
    val homeStretchFillAlpha = if (isDark) 0.42f else 0.26f
    val neutralAltFill = if (isDark) Color.White.copy(alpha = 0.02f) else BirchLinen.copy(alpha = 0.65f)

    for (r in 0 until 15) {
        for (c in 0 until 15) {
            val inYard = (c < 6 && r < 6) || (c >= 9 && r < 6) || (c >= 9 && r >= 9) || (c < 6 && r >= 9)
            val inCenter = c in 6..8 && r in 6..8
            if (inYard || inCenter) continue

            val specialColor: Color? = when {
                c == 1 && r == 6 -> NordicRed
                r == 7 && c in 1..5 -> NordicRed.copy(alpha = homeStretchFillAlpha)
                c == 8 && r == 1 -> NordicGreen
                c == 7 && r in 1..5 -> NordicGreen.copy(alpha = homeStretchFillAlpha)
                c == 13 && r == 8 -> NordicYellow
                r == 7 && c in 9..13 -> NordicYellow.copy(alpha = homeStretchFillAlpha)
                c == 6 && r == 13 -> NordicBlue
                c == 7 && r in 9..13 -> NordicBlue.copy(alpha = homeStretchFillAlpha)
                (c + r) % 2 == 1 -> neutralAltFill
                else -> null
            }

            if (specialColor != null) {
                drawRect(
                    color = specialColor,
                    topLeft = Offset(c * cell, r * cell),
                    size = Size(cell, cell)
                )
            }

            drawRect(
                color = gridLineColor,
                topLeft = Offset(c * cell, r * cell),
                size = Size(cell, cell),
                style = Stroke(width = 0.9.dp.toPx())
            )
        }
    }

    // Home-stretch directional entry chevrons
    drawEntryChevron(Offset(0.5f * cell, 7.5f * cell), 0f, NordicRed, cell)
    drawEntryChevron(Offset(7.5f * cell, 0.5f * cell), 90f, NordicGreen, cell)
    drawEntryChevron(Offset(14.5f * cell, 7.5f * cell), 180f, NordicYellow, cell)
    drawEntryChevron(Offset(7.5f * cell, 14.5f * cell), 270f, NordicBlue, cell)

    // Star emblems on the 4 star safe squares
    BoardGeometry.STAR_SAFE_INDICES.forEach { idx ->
        val coord = BoardGeometry.PERIMETER_COORDS[idx]
        drawNordicStar(
            center = Offset((coord.col + 0.5f) * cell, (coord.row + 0.5f) * cell),
            outerRadius = cell * 0.31f,
            innerRadius = cell * 0.14f,
            color = starColor.copy(alpha = 0.78f)
        )
    }

    // Start-square circular medallion
    BoardGeometry.START_SAFE_INDICES.forEach { idx ->
        val coord = BoardGeometry.PERIMETER_COORDS[idx]
        val center = Offset((coord.col + 0.5f) * cell, (coord.row + 0.5f) * cell)
        drawCircle(
            color = ChalkWhite.copy(alpha = 0.85f),
            radius = cell * 0.22f,
            center = center,
            style = Stroke(width = cell * 0.06f)
        )
    }
}

private fun DrawScope.drawEntryChevron(
    center: Offset,
    angleDeg: Float,
    color: Color,
    cell: Float
) {
    val rad = angleDeg * PI / 180.0
    val cosA = cos(rad).toFloat()
    val sinA = sin(rad).toFloat()
    val s = cell * 0.16f

    fun rot(x: Float, y: Float): Offset = Offset(
        center.x + x * cosA - y * sinA,
        center.y + x * sinA + y * cosA
    )

    val path = Path().apply {
        val p1 = rot(-s * 0.6f, -s)
        val p2 = rot(s * 0.7f, 0f)
        val p3 = rot(-s * 0.6f, s)
        moveTo(p1.x, p1.y)
        lineTo(p2.x, p2.y)
        lineTo(p3.x, p3.y)
    }
    drawPath(
        path = path,
        color = color.copy(alpha = 0.85f),
        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

private fun DrawScope.drawCornerYard(
    startCol: Int,
    startRow: Int,
    color: Color,
    innerSurface: Color,
    borderColor: Color,
    cell: Float,
    isActiveCorner: Boolean
) {
    val left = startCol * cell
    val top = startRow * cell
    val yardSize = cell * 6f
    val corner = CornerRadius(cell * 0.48f, cell * 0.48f)

    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(color.copy(alpha = 0.94f), color.copy(alpha = 0.80f)),
            start = Offset(left, top),
            end = Offset(left + yardSize, top + yardSize)
        ),
        topLeft = Offset(left, top),
        size = Size(yardSize, yardSize),
        cornerRadius = corner
    )

    if (isActiveCorner) {
        drawRoundRect(
            color = ChalkWhite.copy(alpha = 0.38f),
            topLeft = Offset(left + cell * 0.14f, top + cell * 0.14f),
            size = Size(yardSize - cell * 0.28f, yardSize - cell * 0.28f),
            cornerRadius = CornerRadius(cell * 0.4f, cell * 0.4f),
            style = Stroke(width = 2.dp.toPx())
        )
    }

    val inset = cell * 0.82f
    val innerSize = yardSize - inset * 2f
    drawRoundRect(
        color = innerSurface,
        topLeft = Offset(left + inset, top + inset),
        size = Size(innerSize, innerSize),
        cornerRadius = CornerRadius(cell * 0.36f, cell * 0.36f)
    )

    val pocketOffsets = listOf(1.5f to 1.5f, 3.5f to 1.5f, 1.5f to 3.5f, 3.5f to 3.5f)
    pocketOffsets.forEach { (dc, dr) ->
        val cx = (startCol + dc + 0.5f) * cell
        val cy = (startRow + dr + 0.5f) * cell
        drawCircle(
            color = color.copy(alpha = 0.20f),
            radius = cell * 0.56f,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = borderColor.copy(alpha = 0.45f),
            radius = cell * 0.56f,
            center = Offset(cx, cy),
            style = Stroke(width = 1.dp.toPx())
        )
    }
}

private fun DrawScope.drawCenterHomeTriangles(
    cell: Float,
    borderColor: Color,
    centerFill: Color
) {
    val left = 6f * cell
    val top = 6f * cell
    val right = 9f * cell
    val bottom = 9f * cell
    val cx = 7.5f * cell
    val cy = 7.5f * cell

    val triangles = listOf(
        NordicRed to Path().apply { moveTo(left, top); lineTo(cx, cy); lineTo(left, bottom); close() },
        NordicGreen to Path().apply { moveTo(left, top); lineTo(right, top); lineTo(cx, cy); close() },
        NordicYellow to Path().apply { moveTo(right, top); lineTo(right, bottom); lineTo(cx, cy); close() },
        NordicBlue to Path().apply { moveTo(left, bottom); lineTo(cx, cy); lineTo(right, bottom); close() }
    )

    triangles.forEach { (color, path) ->
        drawPath(path = path, color = color.copy(alpha = 0.90f))
        drawPath(path = path, color = borderColor, style = Stroke(width = 1.dp.toPx()))
    }

    // Crisp center Nordic compass medallion
    drawCircle(
        color = centerFill,
        radius = cell * 0.32f,
        center = Offset(cx, cy)
    )
    drawCircle(
        color = borderColor,
        radius = cell * 0.32f,
        center = Offset(cx, cy),
        style = Stroke(width = 1.2.dp.toPx())
    )
    drawCircle(
        color = WarmSand,
        radius = cell * 0.14f,
        center = Offset(cx, cy)
    )
}

private fun DrawScope.drawTeamPairIndicators(cell: Float) {
    val barThickness = cell * 0.16f
    drawRoundRect(
        color = NordicGreen.copy(alpha = 0.65f),
        topLeft = Offset(6.15f * cell, 0.08f * cell),
        size = Size(2.7f * cell, barThickness),
        cornerRadius = CornerRadius(barThickness / 2f, barThickness / 2f)
    )
    drawRoundRect(
        color = NordicBlue.copy(alpha = 0.65f),
        topLeft = Offset(6.15f * cell, 14.76f * cell),
        size = Size(2.7f * cell, barThickness),
        cornerRadius = CornerRadius(barThickness / 2f, barThickness / 2f)
    )
}

private fun DrawScope.drawNordicStar(
    center: Offset,
    outerRadius: Float,
    innerRadius: Float,
    color: Color
) {
    val path = Path()
    val points = 5
    for (i in 0 until points * 2) {
        val r = if (i % 2 == 0) outerRadius else innerRadius
        val angle = -PI / 2.0 + i * (PI / points)
        val x = center.x + (r * cos(angle)).toFloat()
        val y = center.y + (r * sin(angle)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path = path, color = color, style = Stroke(width = 1.4.dp.toPx()))
}
