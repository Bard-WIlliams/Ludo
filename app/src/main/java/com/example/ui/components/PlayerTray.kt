package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.GameMode
import com.example.domain.PlayerSlot
import com.example.domain.Token
import com.example.domain.TurnPhase
import com.example.ui.theme.ChalkWhite
import com.example.ui.theme.DarkBirch
import com.example.ui.theme.DarkOat
import com.example.ui.theme.SoftOat

@Composable
fun PlayerTray(
    slot: PlayerSlot,
    tokens: List<Token>,
    gameMode: GameMode,
    isCurrentTurn: Boolean,
    turnPhase: TurnPhase,
    diceValue: Int?,
    rollingPreviewFace: Int?,
    isRolling: Boolean,
    canInteract: Boolean,
    isLocalSeatBadge: Boolean,
    onRollClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = slot.color.accentColor()
    val tint = slot.color.surfaceTintColor()
    val finishedCount = tokens.count { it.isFinished }

    val borderColor by animateColorAsState(
        targetValue = if (isCurrentTurn && slot.isActive) accent else MaterialTheme.colorScheme.outline.copy(alpha = 0.75f),
        animationSpec = tween(220),
        label = "trayBorder"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isCurrentTurn && slot.isActive) tint else MaterialTheme.colorScheme.surface,
        animationSpec = tween(220),
        label = "trayBg"
    )

    Surface(
        modifier = modifier
            .alpha(if (slot.isActive) 1f else 0.42f)
            .testTag("player_tray_${slot.color.name.lowercase()}"),
        shape = RoundedCornerShape(18.dp),
        color = bgColor,
        border = BorderStroke(
            width = if (isCurrentTurn && slot.isActive) 2.dp else 1.dp,
            color = borderColor
        ),
        shadowElevation = if (isCurrentTurn && slot.isActive) 4.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(10.dp),
                        shape = CircleShape,
                        color = accent
                    ) {}
                    Text(
                        text = slot.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (slot.isAi && slot.isActive) {
                        Icon(
                            imageVector = Icons.Rounded.SmartToy,
                            contentDescription = "AI Bot",
                            tint = accent,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    if (gameMode == GameMode.TEAM_2V2 && slot.isActive) {
                        val teamLabel = if (slot.color.teamId.name == "NORTH") "N" else "S"
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = slot.color.teamId.accentColor().copy(alpha = 0.18f)
                        ) {
                            Text(
                                text = teamLabel,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = slot.color.teamId.accentColor(),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }

                    if (isLocalSeatBadge) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = accent.copy(alpha = 0.18f)
                        ) {
                            Text(
                                text = "YOU",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = accent,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }

                    // Finished home pips (0..4)
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        repeat(4) { idx ->
                            val done = idx < finishedCount
                            Surface(
                                modifier = Modifier.size(7.dp),
                                shape = CircleShape,
                                color = if (done) accent else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                            ) {}
                        }
                    }

                    if (slot.captures > 0) {
                        Text(
                            text = "×${slot.captures}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = accent
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            if (slot.isActive) {
                DieWidget(
                    value = if (isCurrentTurn) diceValue else null,
                    previewFace = if (isCurrentTurn) rollingPreviewFace else null,
                    accentColor = accent,
                    isActiveTurn = isCurrentTurn,
                    isWaitingForRoll = isCurrentTurn && turnPhase == TurnPhase.WAITING_FOR_ROLL,
                    isRolling = isCurrentTurn && isRolling,
                    canTap = isCurrentTurn && turnPhase == TurnPhase.WAITING_FOR_ROLL && canInteract && !slot.isAi,
                    onClick = onRollClick,
                    testTag = "die_${slot.color.name.lowercase()}"
                )
            } else {
                Text(
                    text = "Off",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 6.dp)
                )
            }
        }
    }
}

@Composable
fun DieWidget(
    value: Int?,
    previewFace: Int?,
    accentColor: Color,
    isActiveTurn: Boolean,
    isWaitingForRoll: Boolean,
    isRolling: Boolean,
    canTap: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val isDark = isSystemInDarkTheme()
    val infiniteTransition = rememberInfiniteTransition(label = "diePulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(680),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dieScalePulse"
    )
    val rollRotation by animateFloatAsState(
        targetValue = if (isRolling) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 360f),
        label = "dieSpin"
    )
    val rollPop by animateFloatAsState(
        targetValue = if (isRolling) 1.15f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 400f),
        label = "diePop"
    )

    val scale = when {
        isRolling -> rollPop
        canTap && isWaitingForRoll -> pulse
        else -> 1f
    }

    val topFaceColor = when {
        isDark && isActiveTurn -> DarkOat
        isDark -> DarkBirch
        isActiveTurn -> ChalkWhite
        else -> SoftOat
    }
    val bottomFaceColor = when {
        isDark && isActiveTurn -> DarkBirch
        isDark -> DarkBirch
        isActiveTurn -> SoftOat
        else -> SoftOat
    }
    val borderColor = if (canTap) accentColor else if (isActiveTurn) accentColor.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)

    Box(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .size(50.dp)
            .scale(scale)
            .rotate(rollRotation)
            .clip(RoundedCornerShape(14.dp))
            .then(
                if (canTap) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        val pipColor = if (isActiveTurn) accentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val corner = CornerRadius(13.dp.toPx(), 13.dp.toPx())
            // Tactile beveled Nordic die surface
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(topFaceColor, bottomFaceColor),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height)
                ),
                cornerRadius = corner
            )
            drawRoundRect(
                color = borderColor,
                cornerRadius = corner,
                style = Stroke(width = if (isActiveTurn) 2.dp.toPx() else 1.dp.toPx())
            )

            val displayValue = when {
                isRolling && previewFace != null -> previewFace
                value != null -> value
                else -> 0
            }

            val pad = size.minDimension * 0.24f
            val left = pad
            val midX = size.width * 0.5f
            val right = size.width - pad
            val top = pad
            val midY = size.height * 0.5f
            val bottom = size.height - pad
            val r = size.minDimension * 0.088f

            val pips: List<Offset> = when (displayValue) {
                1 -> listOf(Offset(midX, midY))
                2 -> listOf(Offset(left, top), Offset(right, bottom))
                3 -> listOf(Offset(left, top), Offset(midX, midY), Offset(right, bottom))
                4 -> listOf(Offset(left, top), Offset(right, top), Offset(left, bottom), Offset(right, bottom))
                5 -> listOf(
                    Offset(left, top), Offset(right, top),
                    Offset(midX, midY),
                    Offset(left, bottom), Offset(right, bottom)
                )
                6 -> listOf(
                    Offset(left, top), Offset(right, top),
                    Offset(left, midY), Offset(right, midY),
                    Offset(left, bottom), Offset(right, bottom)
                )
                else -> emptyList()
            }

            if (pips.isEmpty()) {
                drawCircle(
                    color = pipColor.copy(alpha = if (isActiveTurn) 0.55f else 0.22f),
                    radius = r * 0.95f,
                    center = Offset(midX, midY)
                )
            } else {
                pips.forEach { pt ->
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.14f),
                        radius = r,
                        center = Offset(pt.x, pt.y + 0.8.dp.toPx())
                    )
                    drawCircle(color = pipColor, radius = r, center = pt)
                }
            }
        }
    }
}
