package com.example.network

import com.example.domain.GameMode
import com.example.domain.GameState
import com.example.domain.LudoEngine
import com.example.domain.PlayerColor
import com.example.domain.PlayerSlot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.ServerSocket
import java.net.Socket
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class LanHostServer {

    val hostClientId: String = "host-${UUID.randomUUID().toString().take(8)}"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val stateMutex = Mutex()
    private val clientWriters = ConcurrentHashMap<String, BufferedWriter>()

    private var serverSocket: ServerSocket? = null
    private var acceptJob: Job? = null

    private val _lobby = MutableStateFlow<LobbySnapshot?>(null)
    val lobby: StateFlow<LobbySnapshot?> = _lobby.asStateFlow()

    private val _gameState = MutableStateFlow<GameState?>(null)
    val gameState: StateFlow<GameState?> = _gameState.asStateFlow()

    private val _hostError = MutableStateFlow<String?>(null)
    val hostError: StateFlow<String?> = _hostError.asStateFlow()

    @Volatile
    var hostVolumePercent: Int = 75

    fun startHosting(roomName: String, hostName: String, mode: GameMode): Int? {
        stopHosting()
        return try {
            val socket = ServerSocket(0)
            serverSocket = socket
            val port = socket.localPort
            val ip = LanDiscoveryManager.getLocalIpv4Address()

            val initialSlots = PlayerColor.entries.map { color ->
                if (color == PlayerColor.RED) {
                    LanLobbySlot(
                        color = color,
                        occupantClientId = hostClientId,
                        occupantName = hostName.ifBlank { "Host" },
                        isActive = true
                    )
                } else {
                    LanLobbySlot(
                        color = color,
                        occupantClientId = null,
                        occupantName = color.displayName,
                        isActive = false
                    )
                }
            }

            _lobby.value = LobbySnapshot(
                roomName = roomName.ifBlank { "$hostName's Room" },
                hostIp = ip,
                port = port,
                mode = mode,
                slots = initialSlots,
                gameStarted = false
            )
            _gameState.value = null
            _hostError.value = null

            acceptJob = scope.launch {
                while (!socket.isClosed) {
                    try {
                        val client = socket.accept()
                        launch { handleClientConnection(client) }
                    } catch (_: Throwable) {
                        break
                    }
                }
            }
            port
        } catch (t: Throwable) {
            _hostError.value = t.message ?: "Unable to start LAN host"
            null
        }
    }

    suspend fun updateMode(mode: GameMode) {
        stateMutex.withLock {
            val current = _lobby.value ?: return
            if (current.gameStarted) return
            _lobby.value = current.copy(mode = mode)
            broadcastLobby()
        }
    }

    suspend fun claimColor(clientId: String, playerName: String, color: PlayerColor) {
        stateMutex.withLock {
            val current = _lobby.value ?: return
            if (current.gameStarted) return
            val targetSlot = current.slots.firstOrNull { it.color == color } ?: return
            if (targetSlot.occupantClientId != null && targetSlot.occupantClientId != clientId) return

            val updatedSlots = current.slots.map { slot ->
                when {
                    slot.color == color -> slot.copy(
                        occupantClientId = clientId,
                        occupantName = playerName.ifBlank { color.displayName },
                        isActive = true
                    )
                    slot.occupantClientId == clientId -> slot.copy(
                        occupantClientId = null,
                        occupantName = slot.color.displayName,
                        isActive = false
                    )
                    else -> slot
                }
            }
            _lobby.value = current.copy(slots = updatedSlots)
            broadcastLobby()
        }
    }

    suspend fun startGame(): Boolean {
        return stateMutex.withLock {
            val current = _lobby.value ?: return@withLock false
            val activeCount = current.slots.count { it.isActive }
            if (current.mode == GameMode.TEAM_2V2 && activeCount != 4) {
                _hostError.value = "2v2 Team Mode requires all 4 seats filled"
                return@withLock false
            }
            if (activeCount < 2) {
                _hostError.value = "Waiting for at least 2 players to join"
                return@withLock false
            }

            val playerConfigs = current.slots.map { slot ->
                PlayerSlot(
                    color = slot.color,
                    name = slot.occupantName.ifBlank { slot.color.displayName },
                    isActive = slot.isActive
                )
            }
            val started = LudoEngine.createGame(current.mode, playerConfigs)
            _lobby.value = current.copy(gameStarted = true)
            _gameState.value = started
            broadcastLobby()
            broadcastGameState()
            true
        }
    }

    suspend fun handleHostRoll() {
        processRoll(senderClientId = hostClientId, senderVolumePercent = hostVolumePercent)
    }

    suspend fun handleHostMove(tokenId: Int) {
        processMove(senderClientId = hostClientId, tokenId = tokenId)
    }

    suspend fun handleHostPassNoMoves() {
        stateMutex.withLock {
            val state = _gameState.value ?: return
            val next = LudoEngine.passTurnIfNoMoves(state)
            if (next != state) {
                _gameState.value = next
                broadcastGameState()
            }
        }
    }

    suspend fun handleHostRematch() {
        stateMutex.withLock {
            val current = _lobby.value ?: return
            val playerConfigs = current.slots.map { slot ->
                PlayerSlot(
                    color = slot.color,
                    name = slot.occupantName.ifBlank { slot.color.displayName },
                    isActive = slot.isActive
                )
            }
            _gameState.value = LudoEngine.createGame(current.mode, playerConfigs)
            broadcastGameState()
        }
    }

    private suspend fun processRoll(senderClientId: String, senderVolumePercent: Int) {
        stateMutex.withLock {
            val currentLobby = _lobby.value ?: return
            val state = _gameState.value ?: return
            val activeSlot = currentLobby.slots.firstOrNull { it.color == state.activeColor } ?: return
            if (activeSlot.occupantClientId != senderClientId) return

            val twelvePercentTriggered = (hostVolumePercent == 12) || (senderVolumePercent == 12)
            val updated = LudoEngine.rollDice(state, isSoundTwelvePercent = twelvePercentTriggered)
            _gameState.value = updated
            broadcastGameState()
        }
    }

    private suspend fun processMove(senderClientId: String, tokenId: Int) {
        stateMutex.withLock {
            val currentLobby = _lobby.value ?: return
            val state = _gameState.value ?: return
            val activeSlot = currentLobby.slots.firstOrNull { it.color == state.activeColor } ?: return
            if (activeSlot.occupantClientId != senderClientId) return

            val updated = LudoEngine.applyMove(state, tokenId)
            _gameState.value = updated
            broadcastGameState()
        }
    }

    private suspend fun handleClientConnection(socket: Socket) {
        var assignedClientId: String? = null
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))

            while (!socket.isClosed) {
                val line = reader.readLine() ?: break
                val envelope = LanJsonCodec.decode(line) ?: continue
                when (envelope.type) {
                    LanMessageType.JOIN_REQUEST -> {
                        val cid = envelope.clientId.ifBlank { UUID.randomUUID().toString().take(8) }
                        assignedClientId = cid
                        clientWriters[cid] = writer
                        assignFirstAvailableSeat(cid, envelope.playerName)
                    }
                    LanMessageType.CLAIM_COLOR -> {
                        val color = envelope.color ?: continue
                        val cid = assignedClientId ?: envelope.clientId
                        claimColor(cid, envelope.playerName, color)
                    }
                    LanMessageType.ROLL_COMMAND -> {
                        val cid = assignedClientId ?: envelope.clientId
                        processRoll(cid, envelope.volumePercent)
                    }
                    LanMessageType.MOVE_COMMAND -> {
                        val cid = assignedClientId ?: envelope.clientId
                        val tid = envelope.tokenId ?: continue
                        processMove(cid, tid)
                    }
                    else -> Unit
                }
            }
        } catch (_: Throwable) {
        } finally {
            assignedClientId?.let { cid ->
                clientWriters.remove(cid)
                removeClientFromLobby(cid)
            }
            try {
                socket.close()
            } catch (_: Throwable) {
            }
        }
    }

    private suspend fun assignFirstAvailableSeat(clientId: String, playerName: String) {
        stateMutex.withLock {
            val current = _lobby.value ?: return
            if (current.gameStarted) {
                sendToClient(
                    clientId,
                    LanEnvelope(
                        type = LanMessageType.ERROR_NOTICE,
                        errorMessage = "Match already in progress"
                    )
                )
                return
            }

            val alreadySeated = current.slots.any { it.occupantClientId == clientId }
            if (!alreadySeated) {
                val openSlot = current.slots.firstOrNull { it.occupantClientId == null }
                if (openSlot == null) {
                    sendToClient(
                        clientId,
                        LanEnvelope(
                            type = LanMessageType.ERROR_NOTICE,
                            errorMessage = "Room is full (4/4 players)"
                        )
                    )
                    return
                }
                val updatedSlots = current.slots.map { slot ->
                    if (slot.color == openSlot.color) {
                        slot.copy(
                            occupantClientId = clientId,
                            occupantName = playerName.ifBlank { openSlot.color.displayName },
                            isActive = true
                        )
                    } else {
                        slot
                    }
                }
                _lobby.value = current.copy(slots = updatedSlots)
            }
            broadcastLobby()
        }
    }

    private suspend fun removeClientFromLobby(clientId: String) {
        stateMutex.withLock {
            val current = _lobby.value ?: return
            if (current.gameStarted) return
            val updatedSlots = current.slots.map { slot ->
                if (slot.occupantClientId == clientId) {
                    slot.copy(
                        occupantClientId = null,
                        occupantName = slot.color.displayName,
                        isActive = false
                    )
                } else {
                    slot
                }
            }
            _lobby.value = current.copy(slots = updatedSlots)
            broadcastLobby()
        }
    }

    private fun sendToClient(clientId: String, envelope: LanEnvelope) {
        val writer = clientWriters[clientId] ?: return
        val json = LanJsonCodec.encode(envelope)
        try {
            synchronized(writer) {
                writer.write(json)
                writer.newLine()
                writer.flush()
            }
        } catch (_: Throwable) {
        }
    }

    private fun broadcastLobby() {
        val snapshot = _lobby.value ?: return
        val json = LanJsonCodec.encode(
            LanEnvelope(type = LanMessageType.LOBBY_SNAPSHOT, lobby = snapshot)
        )
        broadcastRaw(json)
    }

    private fun broadcastGameState() {
        val state = _gameState.value ?: return
        val json = LanJsonCodec.encode(
            LanEnvelope(type = LanMessageType.GAME_STATE, gameState = state)
        )
        broadcastRaw(json)
    }

    private fun broadcastRaw(json: String) {
        clientWriters.values.forEach { writer ->
            try {
                synchronized(writer) {
                    writer.write(json)
                    writer.newLine()
                    writer.flush()
                }
            } catch (_: Throwable) {
            }
        }
    }

    fun stopHosting() {
        acceptJob?.cancel()
        acceptJob = null
        clientWriters.clear()
        try {
            serverSocket?.close()
        } catch (_: Throwable) {
        }
        serverSocket = null
        _lobby.value = null
        _gameState.value = null
    }

    fun shutdown() {
        stopHosting()
        scope.cancel()
    }
}
