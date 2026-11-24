package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.GameMode
import com.example.domain.GameState
import com.example.domain.GridCoord
import com.example.domain.PlayerColor
import com.example.domain.TurnPhase
import com.example.ui.components.LudoBoardCanvas
import com.example.ui.components.PlayerTray
import com.example.ui.components.accentColor
import com.example.ui.components.nordicAmbientBackground
import com.example.viewmodel.MatchTransport

@Composable
fun GameScreen(
    gameState: GameState,
    transport: MatchTransport,
    localPlayerColor: PlayerColor?,
    isRollingDie: Boolean,
    rollingPreviewFace: Int?,
    animatingTokenPositions: Map<Pair<PlayerColor, Int>, GridCoord>,
    volumePercent: Int,
    onRollDice: () -> Unit,
    onSelectToken: (Int) -> Unit,
    onOpenSoundSheet: () -> Unit,
    onOpenRules: () -> Unit,
    onRematch: () -> Unit,
    onExitMatch: () -> Unit
) {
    var showExitConfirm by remember { mutableStateOf(false) }

    BackHandler {
        if (gameState.turnPhase == TurnPhase.GAME_OVER) {
            onExitMatch()
        } else {
            showExitConfirm = true
        }
    }

    val activeSlot = remember(gameState.players, gameState.activeColor) {
        gameState.players.firstOrNull { it.color == gameState.activeColor }
    }
    val canLocalAct = when {
        activeSlot?.isAi == true -> false
        transport == MatchTransport.LOCAL -> true
        else -> localPlayerColor == gameState.activeColor
    }

    val slotsByColor = remember(gameState.players) {
        gameState.players.associateBy { it.color }
    }
    val tokensByColor = remember(gameState.tokens) {
        gameState.tokens.groupBy { it.color }
    }

    Scaffold(
        modifier = Modifier.nordicAmbientBackground(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            // Minimalist Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (gameState.turnPhase == TurnPhase.GAME_OVER) {
                                onExitMatch()
                            } else {
                                showExitConfirm = true
                            }
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("game_exit_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Leave match"
                        )
                    }

                    Column {
                        Text(
                            text = if (gameState.mode == GameMode.TEAM_2V2) {
                                "2v2 Team Match"
                            } else {
                                "Classic Match"
                            },
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = buildString {
                                append("Turn ${gameState.turnCount}")
                                if (transport != MatchTransport.LOCAL) {
                                    append(" · LAN")
                                } else if (gameState.players.any { it.isActive && it.isAi }) {
                                    append(" · AI Enabled")
                                }
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onOpenRules,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("game_rules_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.HelpOutline,
                            contentDescription = "Game rules"
                        )
                    }
                    Surface(
                        onClick = onOpenSoundSheet,
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("game_sound_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (volumePercent == 0) {
                                    Icons.AutoMirrored.Rounded.VolumeOff
                                } else {
                                    Icons.AutoMirrored.Rounded.VolumeUp
                                },
                                contentDescription = "Sound volume",
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "$volumePercent%",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                }
            }

            // Adaptive Board + Corner Trays Layout
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                val availWidth = maxWidth
                val availHeight = maxHeight
                val isWide = availWidth >= 700.dp && availWidth > availHeight

                if (isWide) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1.2f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            val boardSide = minOf(availWidth * 0.56f, availHeight - 16.dp)
                            LudoBoardCanvas(
                                gameState = gameState,
                                animatingPositions = animatingTokenPositions,
                                canInteract = canLocalAct,
                                onTokenSelected = onSelectToken,
                                modifier = Modifier.size(boardSide)
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(0.9f)
                                .widthIn(max = 380.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            PlayerColor.entries.forEach { color ->
                                val slot = slotsByColor.getValue(color)
                                PlayerTray(
                                    slot = slot,
                                    tokens = tokensByColor[color].orEmpty(),
                                    gameMode = gameState.mode,
                                    isCurrentTurn = gameState.activeColor == color,
                                    turnPhase = gameState.turnPhase,
                                    diceValue = gameState.lastDiceValue,
                                    rollingPreviewFace = rollingPreviewFace,
                                    isRolling = isRollingDie,
                                    canInteract = canLocalAct,
                                    isLocalSeatBadge = localPlayerColor == color,
                                    onRollClick = onRollDice
                                )
                            }

                            StatusBanner(
                                gameState = gameState,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            listOf(PlayerColor.RED, PlayerColor.GREEN).forEach { color ->
                                val slot = slotsByColor.getValue(color)
                                PlayerTray(
                                    slot = slot,
                                    tokens = tokensByColor[color].orEmpty(),
                                    gameMode = gameState.mode,
                                    isCurrentTurn = gameState.activeColor == color,
                                    turnPhase = gameState.turnPhase,
                                    diceValue = gameState.lastDiceValue,
                                    rollingPreviewFace = rollingPreviewFace,
                                    isRolling = isRollingDie,
                                    canInteract = canLocalAct,
                                    isLocalSeatBadge = localPlayerColor == color,
                                    onRollClick = onRollDice,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        val boardMaxSide = minOf(availWidth - 4.dp, availHeight - 196.dp).coerceAtLeast(220.dp)
                        LudoBoardCanvas(
                            gameState = gameState,
                            animatingPositions = animatingTokenPositions,
                            canInteract = canLocalAct,
                            onTokenSelected = onSelectToken,
                            modifier = Modifier.size(boardMaxSide)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            listOf(PlayerColor.BLUE, PlayerColor.YELLOW).forEach { color ->
                                val slot = slotsByColor.getValue(color)
                                PlayerTray(
                                    slot = slot,
                                    tokens = tokensByColor[color].orEmpty(),
                                    gameMode = gameState.mode,
                                    isCurrentTurn = gameState.activeColor == color,
                                    turnPhase = gameState.turnPhase,
                                    diceValue = gameState.lastDiceValue,
                                    rollingPreviewFace = rollingPreviewFace,
                                    isRolling = isRollingDie,
                                    canInteract = canLocalAct,
                                    isLocalSeatBadge = localPlayerColor == color,
                                    onRollClick = onRollDice,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        StatusBanner(
                            gameState = gameState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }

    if (showExitConfirm) {
        AlertDialog(
            onDismissRequest = { showExitConfirm = false },
            title = { Text("Leave Match?") },
            text = { Text("Your current tabletop match progress will end.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitConfirm = false
                        onExitMatch()
                    },
                    modifier = Modifier.testTag("confirm_exit_match")
                ) {
                    Text("Leave")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirm = false }) {
                    Text("Keep Playing")
                }
            }
        )
    }

    if (gameState.turnPhase == TurnPhase.GAME_OVER) {
        VictoryDialog(
            gameState = gameState,
            canRematch = transport != MatchTransport.LAN_CLIENT,
            onRematch = onRematch,
            onExit = onExitMatch
        )
    }
}

@Composable
private fun StatusBanner(
    gameState: GameState,
    modifier: Modifier = Modifier
) {
    val activeAccent = gameState.activeColor.accentColor()
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, activeAccent.copy(alpha = 0.55f)),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(9.dp),
                shape = CircleShape,
                color = activeAccent
            ) {}
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = gameState.statusMessage,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag("game_status_text")
            )
        }
    }
}

@Composable
private fun VictoryDialog(
    gameState: GameState,
    canRematch: Boolean,
    onRematch: () -> Unit,
    onExit: () -> Unit
) {
    val headline = when {
        gameState.winnerTeam != null -> "${gameState.winnerTeam.displayName} Wins!"
        gameState.winnerColor != null -> {
            val winnerName = gameState.players.firstOrNull { it.color == gameState.winnerColor }?.name
                ?: gameState.winnerColor.displayName
            "$winnerName Wins!"
        }
        else -> "Match Complete"
    }

    val subline = if (gameState.winnerTeam != null) {
        val members = gameState.players
            .filter { it.color.teamId == gameState.winnerTeam }
            .joinToString(" & ") { it.name }
        "$members brought all 8 tokens home in ${gameState.turnCount} turns."
    } else {
        "Completed in ${gameState.turnCount} turns."
    }

    AlertDialog(
        onDismissRequest = {},
        icon = {
            Icon(
                imageVector = Icons.Rounded.EmojiEvents,
                contentDescription = null,
                tint = (gameState.winnerColor ?: PlayerColor.YELLOW).accentColor(),
                modifier = Modifier.size(38.dp)
            )
        },
        title = {
            Text(
                text = headline,
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = subline,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                gameState.players.filter { it.isActive }.forEach { slot ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                modifier = Modifier.size(8.dp),
                                shape = CircleShape,
                                color = slot.color.accentColor()
                            ) {}
                            Text(text = slot.name, style = MaterialTheme.typography.bodyMedium)
                            if (slot.isAi) {
                                Icon(
                                    imageVector = Icons.Rounded.SmartToy,
                                    contentDescription = "AI Bot",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Text(
                            text = "${slot.captures} captures",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (canRematch) {
                Button(
                    onClick = onRematch,
                    modifier = Modifier.testTag("victory_rematch_button")
                ) {
                    Text("Play Again")
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onExit,
                modifier = Modifier.testTag("victory_exit_button")
            ) {
                Text("Main Menu")
            }
        }
    )
}
