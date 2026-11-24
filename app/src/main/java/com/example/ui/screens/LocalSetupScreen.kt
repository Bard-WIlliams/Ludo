package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.AiDifficulty
import com.example.domain.GameMode
import com.example.domain.PlayerColor
import com.example.domain.PlayerSlot
import com.example.domain.TeamId
import com.example.ui.components.accentColor
import com.example.ui.components.nordicAmbientBackground
import com.example.ui.components.surfaceTintColor

private enum class SeatChoice {
    HUMAN,
    AI,
    OFF
}

@Composable
fun LocalSetupScreen(
    initialWithAi: Boolean,
    onBack: () -> Unit,
    onStartGame: (GameMode, List<PlayerSlot>) -> Unit
) {
    var mode by remember { mutableStateOf(GameMode.FREE_FOR_ALL) }
    var aiDifficulty by remember { mutableStateOf(AiDifficulty.TACTICAL) }

    val names = remember(initialWithAi) {
        mutableStateMapOf(
            PlayerColor.RED to if (initialWithAi) "You" else "Red",
            PlayerColor.GREEN to if (initialWithAi) "Bot Green" else "Green",
            PlayerColor.YELLOW to if (initialWithAi) "Bot Yellow" else "Yellow",
            PlayerColor.BLUE to if (initialWithAi) "Bot Blue" else "Blue"
        )
    }
    val seatChoices = remember(initialWithAi) {
        mutableStateMapOf(
            PlayerColor.RED to SeatChoice.HUMAN,
            PlayerColor.GREEN to if (initialWithAi) SeatChoice.AI else SeatChoice.HUMAN,
            PlayerColor.YELLOW to if (initialWithAi) SeatChoice.AI else SeatChoice.HUMAN,
            PlayerColor.BLUE to if (initialWithAi) SeatChoice.AI else SeatChoice.HUMAN
        )
    }

    val effectiveSeats = PlayerColor.entries.associateWith { color ->
        val raw = seatChoices.getValue(color)
        if (mode == GameMode.TEAM_2V2 && raw == SeatChoice.OFF) SeatChoice.AI else raw
    }

    val activeCount = effectiveSeats.values.count { it != SeatChoice.OFF }
    val humanCount = effectiveSeats.values.count { it == SeatChoice.HUMAN }
    val hasAnyAi = effectiveSeats.values.any { it == SeatChoice.AI }

    Scaffold(
        modifier = Modifier.nordicAmbientBackground(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("local_setup_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                    Text(
                        text = if (initialWithAi) "Play vs AI Bots" else "Local Pass & Play",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Spacer(modifier = Modifier.height(2.dp))

                    // Mode Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilterChip(
                            selected = mode == GameMode.FREE_FOR_ALL,
                            onClick = { mode = GameMode.FREE_FOR_ALL },
                            label = { Text("Free-for-All (2–4P)") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("mode_free_for_all")
                        )
                        FilterChip(
                            selected = mode == GameMode.TEAM_2V2,
                            onClick = { mode = GameMode.TEAM_2V2 },
                            label = { Text("2v2 Team Mode") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("mode_team_2v2")
                        )
                    }

                    // AI Difficulty Selector when any AI seat is active
                    if (hasAnyAi) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "AI Bot Difficulty",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    AiDifficulty.entries.forEach { diff ->
                                        FilterChip(
                                            selected = aiDifficulty == diff,
                                            onClick = { aiDifficulty = diff },
                                            label = { Text(diff.displayName) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("ai_diff_${diff.name.lowercase()}")
                                        )
                                    }
                                }
                                Text(
                                    text = when (aiDifficulty) {
                                        AiDifficulty.CASUAL -> "Casual bots play relaxed moves without hunting."
                                        AiDifficulty.TACTICAL -> "Tactical bots prioritize captures, home finishes, and safe zones."
                                        AiDifficulty.MASTER -> "Master bots actively dodge threats and coordinate 2v2 shields."
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (mode == GameMode.TEAM_2V2) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Text(
                                text = "Adjacent corners team up: Top Pair (Red + Green) vs. Bottom Pair (Blue + Yellow). Mix Human and AI seats freely.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(14.dp)
                            )
                        }

                        TeamHeader(team = TeamId.NORTH, subtitle = "Top Adjacent Corners")
                        listOf(PlayerColor.RED, PlayerColor.GREEN).forEach { color ->
                            val seat = effectiveSeats.getValue(color)
                            PlayerSlotConfigCard(
                                color = color,
                                name = names.getValue(color),
                                seat = seat,
                                allowOff = false,
                                allowAiFromHuman = humanCount > 1,
                                onNameChange = { names[color] = it },
                                onSeatChange = { next ->
                                    updateSeat(color, next, names, seatChoices)
                                }
                            )
                        }

                        TeamHeader(team = TeamId.SOUTH, subtitle = "Bottom Adjacent Corners")
                        listOf(PlayerColor.BLUE, PlayerColor.YELLOW).forEach { color ->
                            val seat = effectiveSeats.getValue(color)
                            PlayerSlotConfigCard(
                                color = color,
                                name = names.getValue(color),
                                seat = seat,
                                allowOff = false,
                                allowAiFromHuman = humanCount > 1,
                                onNameChange = { names[color] = it },
                                onSeatChange = { next ->
                                    updateSeat(color, next, names, seatChoices)
                                }
                            )
                        }
                    } else {
                        PlayerColor.entries.forEach { color ->
                            val seat = effectiveSeats.getValue(color)
                            val isOnlyHuman = seat == SeatChoice.HUMAN && humanCount <= 1
                            PlayerSlotConfigCard(
                                color = color,
                                name = names.getValue(color),
                                seat = seat,
                                allowOff = activeCount > 2 && !isOnlyHuman,
                                allowAiFromHuman = !isOnlyHuman,
                                onNameChange = { names[color] = it },
                                onSeatChange = { next ->
                                    updateSeat(color, next, names, seatChoices)
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                Button(
                    onClick = {
                        val slots = PlayerColor.entries.map { color ->
                            val seat = effectiveSeats.getValue(color)
                            PlayerSlot(
                                color = color,
                                name = names.getValue(color).trim().ifBlank { color.displayName },
                                isActive = seat != SeatChoice.OFF,
                                isAi = seat == SeatChoice.AI,
                                aiDifficulty = aiDifficulty
                            )
                        }
                        onStartGame(mode, slots)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("start_local_match_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    val aiCount = effectiveSeats.values.count { it == SeatChoice.AI }
                    Text(
                        text = when {
                            mode == GameMode.TEAM_2V2 && aiCount > 0 -> "Start 2v2 Match ($aiCount AI)"
                            mode == GameMode.TEAM_2V2 -> "Start 2v2 Team Match"
                            aiCount > 0 -> "Start Match (${activeCount - aiCount} Human · $aiCount AI)"
                            else -> "Start $activeCount-Player Match"
                        },
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

private fun updateSeat(
    color: PlayerColor,
    next: SeatChoice,
    names: MutableMap<PlayerColor, String>,
    seatChoices: MutableMap<PlayerColor, SeatChoice>
) {
    val prev = seatChoices[color]
    seatChoices[color] = next
    val currentName = names[color].orEmpty()
    if (next == SeatChoice.AI && (currentName == color.displayName || currentName == "You")) {
        names[color] = "Bot ${color.displayName}"
    } else if (next == SeatChoice.HUMAN && currentName.startsWith("Bot ")) {
        names[color] = color.displayName
    } else if (prev == SeatChoice.OFF && currentName.isBlank()) {
        names[color] = color.displayName
    }
}

@Composable
private fun TeamHeader(team: TeamId, subtitle: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 4.dp)
    ) {
        Surface(
            modifier = Modifier.size(10.dp),
            shape = CircleShape,
            color = team.accentColor()
        ) {}
        Text(
            text = "${team.displayName} · $subtitle",
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
private fun PlayerSlotConfigCard(
    color: PlayerColor,
    name: String,
    seat: SeatChoice,
    allowOff: Boolean,
    allowAiFromHuman: Boolean,
    onNameChange: (String) -> Unit,
    onSeatChange: (SeatChoice) -> Unit
) {
    val accent = color.accentColor()
    val tint = color.surfaceTintColor()
    val isActive = seat != SeatChoice.OFF

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (isActive) tint else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (isActive) 1.5.dp else 1.dp,
            color = if (isActive) accent else MaterialTheme.colorScheme.outline
        ),
        shadowElevation = if (isActive) 1.dp else 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    modifier = Modifier.size(16.dp),
                    shape = CircleShape,
                    color = accent
                ) {}

                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 16) onNameChange(it) },
                    enabled = isActive,
                    singleLine = true,
                    label = { Text(color.displayName) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("player_name_${color.name.lowercase()}")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = seat == SeatChoice.HUMAN,
                    onClick = { onSeatChange(SeatChoice.HUMAN) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Person,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text("Human") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("seat_human_${color.name.lowercase()}")
                )
                FilterChip(
                    selected = seat == SeatChoice.AI,
                    onClick = { onSeatChange(SeatChoice.AI) },
                    enabled = seat == SeatChoice.AI || allowAiFromHuman,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.SmartToy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text("AI Bot") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("seat_ai_${color.name.lowercase()}")
                )
                if (allowOff || seat == SeatChoice.OFF) {
                    FilterChip(
                        selected = seat == SeatChoice.OFF,
                        onClick = { onSeatChange(SeatChoice.OFF) },
                        enabled = allowOff,
                        label = { Text("Off") },
                        modifier = Modifier
                            .weight(0.8f)
                            .testTag("seat_off_${color.name.lowercase()}")
                    )
                }
            }
        }
    }
}
