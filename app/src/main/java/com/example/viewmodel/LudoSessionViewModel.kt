package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SoundEngine
import com.example.data.SoundPreferences
import com.example.domain.BoardGeometry
import com.example.domain.GameMode
import com.example.domain.GameState
import com.example.domain.GridCoord
import com.example.domain.LudoAiAdvisor
import com.example.domain.LudoEngine
import com.example.domain.MoveEvent
import com.example.domain.PlayerColor
import com.example.domain.PlayerSlot
import com.example.domain.TurnPhase
import com.example.network.DiscoveredLanRoom
import com.example.network.LanClientManager
import com.example.network.LanConnectionStatus
import com.example.network.LanDiscoveryManager
import com.example.network.LanHostServer
import com.example.network.LobbySnapshot
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.security.SecureRandom

enum class MatchTransport {
    LOCAL,
    LAN_HOST,
    LAN_CLIENT
}

class LudoSessionViewModel(application: Application) : AndroidViewModel(application) {

    private val soundPreferences = SoundPreferences(application)
    private val soundEngine = SoundEngine()
    private val discoveryManager = LanDiscoveryManager(application)
    private val hostServer = LanHostServer()
    private val clientManager = LanClientManager()
    private val previewRng = SecureRandom()

    private val _volumePercent = MutableStateFlow(SoundPreferences.DEFAULT_VOLUME)
    val volumePercent: StateFlow<Int> = _volumePercent.asStateFlow()

    private val _transport = MutableStateFlow(MatchTransport.LOCAL)
    val transport: StateFlow<MatchTransport> = _transport.asStateFlow()

    private val _localGameState = MutableStateFlow<GameState?>(null)
    val gameState: StateFlow<GameState?> = combine(
        _transport,
        _localGameState,
        hostServer.gameState,
        clientManager.gameState
    ) { mode, local, hosted, client ->
        when (mode) {
            MatchTransport.LOCAL -> local
            MatchTransport.LAN_HOST -> hosted
            MatchTransport.LAN_CLIENT -> client
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val discoveredRooms: StateFlow<List<DiscoveredLanRoom>> = discoveryManager.discoveredRooms
    val isDiscovering: StateFlow<Boolean> = discoveryManager.isDiscovering
    val clientConnectionStatus: StateFlow<LanConnectionStatus> = clientManager.status

    val lanLobby: StateFlow<LobbySnapshot?> = combine(
        _transport,
        hostServer.lobby,
        clientManager.lobby
    ) { mode, hosted, client ->
        when (mode) {
            MatchTransport.LAN_HOST -> hosted
            MatchTransport.LAN_CLIENT -> client
            MatchTransport.LOCAL -> null
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val lanError: StateFlow<String?> = combine(
        hostServer.hostError,
        clientManager.errorMessage
    ) { hostErr, clientErr ->
        hostErr ?: clientErr
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val localPlayerColor: StateFlow<PlayerColor?> = combine(
        _transport,
        lanLobby
    ) { mode, lobby ->
        when (mode) {
            MatchTransport.LOCAL -> null
            MatchTransport.LAN_HOST -> lobby?.slots?.firstOrNull {
                it.occupantClientId == hostServer.hostClientId
            }?.color
            MatchTransport.LAN_CLIENT -> lobby?.slots?.firstOrNull {
                it.occupantClientId == clientManager.clientId
            }?.color
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _isRollingDie = MutableStateFlow(false)
    val isRollingDie: StateFlow<Boolean> = _isRollingDie.asStateFlow()

    private val _rollingPreviewFace = MutableStateFlow<Int?>(null)
    val rollingPreviewFace: StateFlow<Int?> = _rollingPreviewFace.asStateFlow()

    private val _animatingTokenPositions = MutableStateFlow<Map<Pair<PlayerColor, Int>, GridCoord>>(emptyMap())
    val animatingTokenPositions: StateFlow<Map<Pair<PlayerColor, Int>, GridCoord>> =
        _animatingTokenPositions.asStateFlow()

    private var lastLocalConfig: Pair<GameMode, List<PlayerSlot>>? = null
    private var lastObservedMoveSeq = 0L
    private var hopJob: Job? = null
    private var autoPassJob: Job? = null
    private var aiTurnJob: Job? = null

    init {
        viewModelScope.launch {
            soundPreferences.volumePercent.collect { vol ->
                _volumePercent.value = vol
                soundEngine.volumePercent = vol
                hostServer.hostVolumePercent = vol
            }
        }

        // Observe game state updates for hop animations, sound effects, AI turns, and no-move pass
        viewModelScope.launch {
            gameState.collect { state ->
                if (state == null) return@collect
                if (state.moveSequenceId != lastObservedMoveSeq && state.lastMoveEvent != null) {
                    lastObservedMoveSeq = state.moveSequenceId
                    runHopAnimationAndAudio(state.lastMoveEvent, state.turnPhase == TurnPhase.GAME_OVER)
                }
                if (state.turnPhase == TurnPhase.SELECTING_TOKEN && state.legalTokenIds.isEmpty()) {
                    scheduleNoMovePass()
                }
                if (_transport.value == MatchTransport.LOCAL && state.turnPhase != TurnPhase.GAME_OVER) {
                    val activeSlot = state.players.firstOrNull { it.color == state.activeColor }
                    if (activeSlot?.isAi == true) {
                        scheduleAiStep(state, activeSlot)
                    }
                }
            }
        }
    }

    fun setVolumePercent(percent: Int, playPreviewTap: Boolean = false) {
        val clamped = percent.coerceIn(0, 100)
        _volumePercent.value = clamped
        soundEngine.volumePercent = clamped
        hostServer.hostVolumePercent = clamped
        viewModelScope.launch {
            soundPreferences.setVolumePercent(clamped)
        }
        if (playPreviewTap) {
            soundEngine.playTokenStep()
        }
    }

    fun startLocalMatch(mode: GameMode, playerSlots: List<PlayerSlot>) {
        leaveLanRoom()
        aiTurnJob?.cancel()
        autoPassJob?.cancel()
        hopJob?.cancel()
        _transport.value = MatchTransport.LOCAL
        lastLocalConfig = mode to playerSlots
        lastObservedMoveSeq = 0L
        _animatingTokenPositions.value = emptyMap()
        _localGameState.value = LudoEngine.createGame(mode, playerSlots)
    }

    fun rollDice() {
        val current = gameState.value ?: return
        if (current.turnPhase != TurnPhase.WAITING_FOR_ROLL || _isRollingDie.value) return
        if (!canLocalUserAct(current)) return

        viewModelScope.launch {
            performAnimatedDiceTumble()
            when (_transport.value) {
                MatchTransport.LOCAL -> {
                    val state = _localGameState.value ?: return@launch
                    _localGameState.value = LudoEngine.rollDice(
                        state = state,
                        isSoundTwelvePercent = (_volumePercent.value == 12)
                    )
                }
                MatchTransport.LAN_HOST -> hostServer.handleHostRoll()
                MatchTransport.LAN_CLIENT -> clientManager.sendRoll(_volumePercent.value)
            }
        }
    }

    fun selectToken(tokenId: Int) {
        val current = gameState.value ?: return
        if (current.turnPhase != TurnPhase.SELECTING_TOKEN) return
        if (tokenId !in current.legalTokenIds) return
        if (!canLocalUserAct(current)) return
        if (hopJob?.isActive == true) return

        when (_transport.value) {
            MatchTransport.LOCAL -> {
                _localGameState.value = LudoEngine.applyMove(current, tokenId)
            }
            MatchTransport.LAN_HOST -> {
                viewModelScope.launch { hostServer.handleHostMove(tokenId) }
            }
            MatchTransport.LAN_CLIENT -> clientManager.sendMove(tokenId)
        }
    }

    fun rematch() {
        aiTurnJob?.cancel()
        autoPassJob?.cancel()
        hopJob?.cancel()
        lastObservedMoveSeq = 0L
        _animatingTokenPositions.value = emptyMap()
        when (_transport.value) {
            MatchTransport.LOCAL -> {
                val (mode, slots) = lastLocalConfig ?: return
                _localGameState.value = LudoEngine.createGame(mode, slots)
            }
            MatchTransport.LAN_HOST -> {
                viewModelScope.launch { hostServer.handleHostRematch() }
            }
            MatchTransport.LAN_CLIENT -> Unit
        }
    }

    fun leaveMatch() {
        aiTurnJob?.cancel()
        hopJob?.cancel()
        autoPassJob?.cancel()
        _animatingTokenPositions.value = emptyMap()
        _localGameState.value = null
        if (_transport.value != MatchTransport.LOCAL) {
            leaveLanRoom()
        }
    }

    // --- LAN Lobby Actions ---

    fun startLanDiscovery() {
        discoveryManager.startDiscovery()
    }

    fun stopLanDiscovery() {
        discoveryManager.stopDiscovery()
    }

    fun hostLanRoom(roomName: String, hostName: String, mode: GameMode) {
        clientManager.disconnect()
        _transport.value = MatchTransport.LAN_HOST
        lastObservedMoveSeq = 0L
        val port = hostServer.startHosting(roomName, hostName, mode)
        if (port != null) {
            discoveryManager.registerService(
                roomName = roomName.ifBlank { "$hostName's Room" },
                port = port
            )
        }
    }

    fun updateLanHostMode(mode: GameMode) {
        if (_transport.value != MatchTransport.LAN_HOST) return
        viewModelScope.launch {
            hostServer.updateMode(mode)
        }
    }

    fun joinLanRoom(hostIp: String, port: Int, playerName: String) {
        hostServer.stopHosting()
        discoveryManager.unregisterService()
        _transport.value = MatchTransport.LAN_CLIENT
        lastObservedMoveSeq = 0L
        clientManager.connect(hostIp, port, playerName)
    }

    fun claimLanColor(color: PlayerColor, playerName: String) {
        when (_transport.value) {
            MatchTransport.LAN_HOST -> {
                viewModelScope.launch {
                    hostServer.claimColor(hostServer.hostClientId, playerName, color)
                }
            }
            MatchTransport.LAN_CLIENT -> clientManager.claimColor(playerName, color)
            MatchTransport.LOCAL -> Unit
        }
    }

    fun startLanMatch() {
        if (_transport.value != MatchTransport.LAN_HOST) return
        viewModelScope.launch {
            hostServer.startGame()
        }
    }

    fun clearLanError() {
        clientManager.clearError()
    }

    fun leaveLanRoom() {
        discoveryManager.unregisterService()
        discoveryManager.stopDiscovery()
        hostServer.stopHosting()
        clientManager.disconnect()
        _transport.value = MatchTransport.LOCAL
    }

    private fun canLocalUserAct(state: GameState): Boolean {
        val activeSlot = state.players.firstOrNull { it.color == state.activeColor }
        if (activeSlot?.isAi == true) return false
        return when (_transport.value) {
            MatchTransport.LOCAL -> true
            MatchTransport.LAN_HOST, MatchTransport.LAN_CLIENT -> localPlayerColor.value == state.activeColor
        }
    }

    private suspend fun performAnimatedDiceTumble() {
        _isRollingDie.value = true
        soundEngine.playDiceRoll()
        var lastFace = 0
        repeat(6) {
            var nextFace = previewRng.nextInt(6) + 1
            if (nextFace == lastFace) {
                nextFace = (nextFace % 6) + 1
            }
            lastFace = nextFace
            _rollingPreviewFace.value = nextFace
            delay(65L)
        }
        _isRollingDie.value = false
        _rollingPreviewFace.value = null
    }

    private fun scheduleAiStep(state: GameState, aiSlot: PlayerSlot) {
        aiTurnJob?.cancel()
        aiTurnJob = viewModelScope.launch {
            // Wait for any token hop animation to settle first
            while (hopJob?.isActive == true) {
                delay(50L)
            }
            when (state.turnPhase) {
                TurnPhase.WAITING_FOR_ROLL -> {
                    delay(460L)
                    performAnimatedDiceTumble()
                    val latest = _localGameState.value ?: return@launch
                    if (latest.turnPhase == TurnPhase.WAITING_FOR_ROLL && latest.activeColor == aiSlot.color) {
                        _localGameState.value = LudoEngine.rollDice(
                            state = latest,
                            isSoundTwelvePercent = (_volumePercent.value == 12)
                        )
                    }
                }
                TurnPhase.SELECTING_TOKEN -> {
                    if (state.legalTokenIds.isNotEmpty()) {
                        delay(520L)
                        val latest = _localGameState.value ?: return@launch
                        if (latest.turnPhase == TurnPhase.SELECTING_TOKEN &&
                            latest.activeColor == aiSlot.color &&
                            latest.legalTokenIds.isNotEmpty()
                        ) {
                            val chosen = LudoAiAdvisor.chooseToken(latest, aiSlot.aiDifficulty)
                            if (chosen != null) {
                                _localGameState.value = LudoEngine.applyMove(latest, chosen)
                            }
                        }
                    }
                }
                TurnPhase.GAME_OVER -> Unit
            }
        }
    }

    private fun scheduleNoMovePass() {
        autoPassJob?.cancel()
        autoPassJob = viewModelScope.launch {
            delay(850L)
            when (_transport.value) {
                MatchTransport.LOCAL -> {
                    val state = _localGameState.value ?: return@launch
                    _localGameState.value = LudoEngine.passTurnIfNoMoves(state)
                }
                MatchTransport.LAN_HOST -> hostServer.handleHostPassNoMoves()
                MatchTransport.LAN_CLIENT -> Unit
            }
        }
    }

    private fun runHopAnimationAndAudio(event: MoveEvent, isGameOver: Boolean) {
        hopJob?.cancel()
        hopJob = viewModelScope.launch {
            val key = event.moverColor to event.tokenId
            if (event.fromProgress < 0 && event.toProgress == 0) {
                _animatingTokenPositions.value = mapOf(
                    key to BoardGeometry.coordForToken(event.moverColor, event.tokenId, 0)
                )
                soundEngine.playTokenStep()
                delay(120L)
            } else if (event.fromProgress >= 0) {
                for (step in (event.fromProgress + 1)..event.toProgress) {
                    _animatingTokenPositions.value = mapOf(
                        key to BoardGeometry.coordForToken(event.moverColor, event.tokenId, step)
                    )
                    soundEngine.playTokenStep()
                    delay(75L)
                }
            }
            _animatingTokenPositions.value = emptyMap()

            if (event.capturedTokens.isNotEmpty()) {
                soundEngine.playCapture()
            } else if (event.reachedHome) {
                soundEngine.playHomeFinish()
            }
            if (isGameOver) {
                delay(180L)
                soundEngine.playVictory()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        discoveryManager.unregisterService()
        discoveryManager.stopDiscovery()
        hostServer.shutdown()
        clientManager.shutdown()
    }
}
