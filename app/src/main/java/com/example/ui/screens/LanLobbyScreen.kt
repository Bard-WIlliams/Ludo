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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.WifiFind
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.GameMode
import com.example.domain.PlayerColor
import com.example.network.DiscoveredLanRoom
import com.example.network.LanConnectionStatus
import com.example.network.LobbySnapshot
import com.example.ui.components.accentColor
import com.example.ui.components.nordicAmbientBackground
import com.example.ui.components.surfaceTintColor

@Composable
fun LanLobbyScreen(
    discoveredRooms: List<DiscoveredLanRoom>,
    isDiscovering: Boolean,
    lobbySnapshot: LobbySnapshot?,
    connectionStatus: LanConnectionStatus,
    localPlayerColor: PlayerColor?,
    isHost: Boolean,
    errorMessage: String?,
    onStartDiscovery: () -> Unit,
    onStopDiscovery: () -> Unit,
    onHostRoom: (String, String, GameMode) -> Unit,
    onUpdateHostMode: (GameMode) -> Unit,
    onJoinRoom: (String, Int, String) -> Unit,
    onClaimColor: (PlayerColor, String) -> Unit,
    onStartMatch: () -> Unit,
    onLeaveLobby: () -> Unit,
    onClearError: () -> Unit,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Join, 1 = Host
    var playerName by remember { mutableStateOf("Player") }
    var roomName by remember { mutableStateOf("Nordic Room") }
    var hostMode by remember { mutableStateOf(GameMode.FREE_FOR_ALL) }
    var manualIp by remember { mutableStateOf("") }
    var manualPort by remember { mutableStateOf("") }

    DisposableEffect(lobbySnapshot == null && selectedTab == 0) {
        if (lobbySnapshot == null && selectedTab == 0) {
            onStartDiscovery()
        }
        onDispose {
            onStopDiscovery()
        }
    }

    Scaffold(
        modifier = Modifier.nordicAmbientBackground(),
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
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
                        onClick = {
                            onLeaveLobby()
                            onBack()
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("lan_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                    Text(
                        text = "LAN Multiplayer",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }

                if (errorMessage != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = errorMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedButton(onClick = onClearError) {
                                Text("Dismiss")
                            }
                        }
                    }
                }

                if (lobbySnapshot != null) {
                    ActiveRoomLobbyView(
                        lobby = lobbySnapshot,
                        localPlayerColor = localPlayerColor,
                        isHost = isHost,
                        playerName = playerName,
                        onUpdateHostMode = onUpdateHostMode,
                        onClaimColor = onClaimColor,
                        onStartMatch = onStartMatch,
                        onLeaveLobby = onLeaveLobby
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        OutlinedTextField(
                            value = playerName,
                            onValueChange = { if (it.length <= 16) playerName = it },
                            singleLine = true,
                            label = { Text("Your Name") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("lan_player_name_input")
                        )

                        TabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Tab(
                                selected = selectedTab == 0,
                                onClick = {
                                    selectedTab = 0
                                    onStartDiscovery()
                                },
                                text = { Text("Join Room") },
                                modifier = Modifier.testTag("lan_tab_join")
                            )
                            Tab(
                                selected = selectedTab == 1,
                                onClick = {
                                    selectedTab = 1
                                    onStopDiscovery()
                                },
                                text = { Text("Host Room") },
                                modifier = Modifier.testTag("lan_tab_host")
                            )
                        }

                        if (selectedTab == 0) {
                            // Nearby NSD Rooms
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Nearby LAN Rooms",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                IconButton(
                                    onClick = onStartDiscovery,
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("lan_refresh_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Refresh,
                                        contentDescription = "Refresh LAN rooms"
                                    )
                                }
                            }

                            if (isDiscovering) {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            }

                            if (connectionStatus == LanConnectionStatus.CONNECTING) {
                                Text(
                                    text = "Connecting to room…",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }

                            if (discoveredRooms.isEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.WifiFind,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Text(
                                            text = "Scanning same Wi-Fi or Hotspot…",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = "Make sure all players are on the same local network, or enter the host's IP & port below.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            } else {
                                discoveredRooms.forEach { room ->
                                    Surface(
                                        onClick = {
                                            onJoinRoom(room.hostAddress, room.port, playerName)
                                        },
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("discovered_room_${room.port}")
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = room.serviceName,
                                                    style = MaterialTheme.typography.titleMedium
                                                )
                                                Text(
                                                    text = "${room.hostAddress}:${room.port}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Text(
                                                text = "Join",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    }
                                }
                            }

                            // Manual IP & Port Fallback
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "Manual IP Fallback",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = manualIp,
                                            onValueChange = { manualIp = it },
                                            singleLine = true,
                                            label = { Text("Host IP") },
                                            placeholder = { Text("192.168.1.10") },
                                            modifier = Modifier
                                                .weight(1.8f)
                                                .testTag("lan_manual_ip_input")
                                        )
                                        OutlinedTextField(
                                            value = manualPort,
                                            onValueChange = { manualPort = it.filter(Char::isDigit) },
                                            singleLine = true,
                                            label = { Text("Port") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("lan_manual_port_input")
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            val portInt = manualPort.toIntOrNull() ?: return@Button
                                            onJoinRoom(manualIp, portInt, playerName)
                                        },
                                        enabled = manualIp.isNotBlank() && (manualPort.toIntOrNull() ?: 0) > 0,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("lan_manual_connect_button")
                                    ) {
                                        Text("Connect to IP")
                                    }
                                }
                            }
                        } else {
                            // Host Room Tab
                            OutlinedTextField(
                                value = roomName,
                                onValueChange = { if (it.length <= 24) roomName = it },
                                singleLine = true,
                                label = { Text("Room Name") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("lan_room_name_input")
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                FilterChip(
                                    selected = hostMode == GameMode.FREE_FOR_ALL,
                                    onClick = { hostMode = GameMode.FREE_FOR_ALL },
                                    label = { Text("Free-for-All (2–4P)") },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = hostMode == GameMode.TEAM_2V2,
                                    onClick = { hostMode = GameMode.TEAM_2V2 },
                                    label = { Text("2v2 Team Mode") },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Button(
                                onClick = { onHostRoom(roomName, playerName, hostMode) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("lan_create_room_button"),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Open LAN Room")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveRoomLobbyView(
    lobby: LobbySnapshot,
    localPlayerColor: PlayerColor?,
    isHost: Boolean,
    playerName: String,
    onUpdateHostMode: (GameMode) -> Unit,
    onClaimColor: (PlayerColor, String) -> Unit,
    onStartMatch: () -> Unit,
    onLeaveLobby: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = lobby.roomName,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "IP: ${lobby.hostIp}   •   Port: ${lobby.port}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = if (lobby.mode == GameMode.TEAM_2V2) {
                        "2v2 Adjacent-Corner Teams (Red+Green vs Blue+Yellow)"
                    } else {
                        "Classic Free-for-All PvP (2–4 Players)"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (isHost) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = lobby.mode == GameMode.FREE_FOR_ALL,
                    onClick = { onUpdateHostMode(GameMode.FREE_FOR_ALL) },
                    label = { Text("Free-for-All") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = lobby.mode == GameMode.TEAM_2V2,
                    onClick = { onUpdateHostMode(GameMode.TEAM_2V2) },
                    label = { Text("2v2 Teams") },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Text(
            text = "Tap an open corner to choose your color",
            style = MaterialTheme.typography.titleMedium
        )

        lobby.slots.forEach { slot ->
            val accent = slot.color.accentColor()
            val tint = slot.color.surfaceTintColor()
            val isMine = localPlayerColor == slot.color
            val isOpen = slot.occupantClientId == null

            Surface(
                onClick = {
                    if (isOpen && !isMine) {
                        onClaimColor(slot.color, playerName)
                    }
                },
                enabled = isOpen && !isMine,
                shape = RoundedCornerShape(16.dp),
                color = if (slot.isActive) tint else MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    width = if (isMine) 2.dp else 1.dp,
                    color = if (slot.isActive) accent else MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("lan_seat_${slot.color.name.lowercase()}")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(16.dp),
                            shape = CircleShape,
                            color = accent
                        ) {}
                        Column {
                            Text(
                                text = if (slot.isActive) slot.occupantName else "${slot.color.displayName} (Open)",
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (lobby.mode == GameMode.TEAM_2V2) {
                                Text(
                                    text = slot.color.teamId.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Text(
                        text = when {
                            isMine -> "You"
                            slot.isActive -> "Seated"
                            else -> "Claim"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isMine || isOpen) accent else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        val activeCount = lobby.slots.count { it.isActive }
        val readyToStart = if (lobby.mode == GameMode.TEAM_2V2) activeCount == 4 else activeCount >= 2

        if (isHost) {
            Button(
                onClick = onStartMatch,
                enabled = readyToStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("lan_start_match_button"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = if (lobby.mode == GameMode.TEAM_2V2 && activeCount < 4) {
                        "Need 4/4 players for 2v2 ($activeCount/4)"
                    } else if (activeCount < 2) {
                        "Waiting for another player ($activeCount/4)"
                    } else {
                        "Start LAN Match ($activeCount Players)"
                    }
                )
            }
        } else {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = "Waiting for host to start the match…",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                )
            }
        }

        OutlinedButton(
            onClick = onLeaveLobby,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("lan_leave_room_button")
        ) {
            Text("Leave Room")
        }
    }
}
