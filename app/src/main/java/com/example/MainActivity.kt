package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.components.RulesDialog
import com.example.ui.components.SoundSettingsSheet
import com.example.ui.screens.GameScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LanLobbyScreen
import com.example.ui.screens.LocalSetupScreen
import com.example.ui.theme.NordicLudoTheme
import com.example.viewmodel.LudoSessionViewModel
import com.example.viewmodel.MatchTransport

private const val ROUTE_HOME = "home"
private const val ROUTE_LOCAL_SETUP = "local_setup/{withAi}"
private const val ROUTE_LAN_LOBBY = "lan_lobby"
private const val ROUTE_GAME = "game"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NordicLudoTheme {
                val navController = rememberNavController()
                val sessionVm: LudoSessionViewModel = viewModel()

                val volumePercent by sessionVm.volumePercent.collectAsStateWithLifecycle()
                val gameState by sessionVm.gameState.collectAsStateWithLifecycle()
                val transport by sessionVm.transport.collectAsStateWithLifecycle()
                val discoveredRooms by sessionVm.discoveredRooms.collectAsStateWithLifecycle()
                val isDiscovering by sessionVm.isDiscovering.collectAsStateWithLifecycle()
                val lanLobby by sessionVm.lanLobby.collectAsStateWithLifecycle()
                val connectionStatus by sessionVm.clientConnectionStatus.collectAsStateWithLifecycle()
                val lanError by sessionVm.lanError.collectAsStateWithLifecycle()
                val localPlayerColor by sessionVm.localPlayerColor.collectAsStateWithLifecycle()
                val isRollingDie by sessionVm.isRollingDie.collectAsStateWithLifecycle()
                val rollingPreviewFace by sessionVm.rollingPreviewFace.collectAsStateWithLifecycle()
                val animatingPositions by sessionVm.animatingTokenPositions.collectAsStateWithLifecycle()

                var showSoundSheet by remember { mutableStateOf(false) }
                var showRulesDialog by remember { mutableStateOf(false) }

                NavHost(
                    navController = navController,
                    startDestination = ROUTE_HOME
                ) {
                    composable(ROUTE_HOME) {
                        HomeScreen(
                            volumePercent = volumePercent,
                            onAiPlayClick = { navController.navigate("local_setup/true") },
                            onLocalPlayClick = { navController.navigate("local_setup/false") },
                            onLanPlayClick = { navController.navigate(ROUTE_LAN_LOBBY) },
                            onOpenSoundSheet = { showSoundSheet = true },
                            onOpenRules = { showRulesDialog = true }
                        )
                    }

                    composable(
                        route = ROUTE_LOCAL_SETUP,
                        arguments = listOf(navArgument("withAi") { type = NavType.BoolType })
                    ) { backStackEntry ->
                        val withAi = backStackEntry.arguments?.getBoolean("withAi") ?: false
                        LocalSetupScreen(
                            initialWithAi = withAi,
                            onBack = { navController.popBackStack() },
                            onStartGame = { mode, slots ->
                                sessionVm.startLocalMatch(mode, slots)
                                navController.navigate(ROUTE_GAME) {
                                    launchSingleTop = true
                                }
                            }
                        )
                    }

                    composable(ROUTE_LAN_LOBBY) {
                        LaunchedEffect(gameState, transport) {
                            if (gameState != null && transport != MatchTransport.LOCAL) {
                                navController.navigate(ROUTE_GAME) {
                                    launchSingleTop = true
                                }
                            }
                        }

                        LanLobbyScreen(
                            discoveredRooms = discoveredRooms,
                            isDiscovering = isDiscovering,
                            lobbySnapshot = lanLobby,
                            connectionStatus = connectionStatus,
                            localPlayerColor = localPlayerColor,
                            isHost = transport == MatchTransport.LAN_HOST,
                            errorMessage = lanError,
                            onStartDiscovery = { sessionVm.startLanDiscovery() },
                            onStopDiscovery = { sessionVm.stopLanDiscovery() },
                            onHostRoom = { room, hostName, mode ->
                                sessionVm.hostLanRoom(room, hostName, mode)
                            },
                            onUpdateHostMode = { mode -> sessionVm.updateLanHostMode(mode) },
                            onJoinRoom = { ip, port, name ->
                                sessionVm.joinLanRoom(ip, port, name)
                            },
                            onClaimColor = { color, name ->
                                sessionVm.claimLanColor(color, name)
                            },
                            onStartMatch = { sessionVm.startLanMatch() },
                            onLeaveLobby = { sessionVm.leaveLanRoom() },
                            onClearError = { sessionVm.clearLanError() },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(ROUTE_GAME) {
                        val activeGame = gameState
                        if (activeGame != null) {
                            GameScreen(
                                gameState = activeGame,
                                transport = transport,
                                localPlayerColor = localPlayerColor,
                                isRollingDie = isRollingDie,
                                rollingPreviewFace = rollingPreviewFace,
                                animatingTokenPositions = animatingPositions,
                                volumePercent = volumePercent,
                                onRollDice = { sessionVm.rollDice() },
                                onSelectToken = { id -> sessionVm.selectToken(id) },
                                onOpenSoundSheet = { showSoundSheet = true },
                                onOpenRules = { showRulesDialog = true },
                                onRematch = { sessionVm.rematch() },
                                onExitMatch = {
                                    sessionVm.leaveMatch()
                                    navController.popBackStack(ROUTE_HOME, inclusive = false)
                                }
                            )
                        } else {
                            LaunchedEffect(Unit) {
                                navController.popBackStack(ROUTE_HOME, inclusive = false)
                            }
                        }
                    }
                }

                if (showSoundSheet) {
                    SoundSettingsSheet(
                        volumePercent = volumePercent,
                        onVolumeChange = { percent, preview ->
                            sessionVm.setVolumePercent(percent, preview)
                        },
                        onDismiss = { showSoundSheet = false }
                    )
                }

                if (showRulesDialog) {
                    RulesDialog(onDismiss = { showRulesDialog = false })
                }
            }
        }
    }
}
