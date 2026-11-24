package com.example.network

import com.example.domain.GameState
import com.example.domain.PlayerColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.util.UUID

enum class LanConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}

class LanClientManager {

    val clientId: String = "client-${UUID.randomUUID().toString().take(8)}"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var socket: Socket? = null
    private var writer: BufferedWriter? = null
    private var readJob: Job? = null

    private val _status = MutableStateFlow(LanConnectionStatus.DISCONNECTED)
    val status: StateFlow<LanConnectionStatus> = _status.asStateFlow()

    private val _lobby = MutableStateFlow<LobbySnapshot?>(null)
    val lobby: StateFlow<LobbySnapshot?> = _lobby.asStateFlow()

    private val _gameState = MutableStateFlow<GameState?>(null)
    val gameState: StateFlow<GameState?> = _gameState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun connect(hostIp: String, port: Int, playerName: String) {
        disconnect()
        _status.value = LanConnectionStatus.CONNECTING
        _errorMessage.value = null

        readJob = scope.launch {
            try {
                val tcpSocket = Socket()
                tcpSocket.connect(InetSocketAddress(hostIp.trim(), port), 4500)
                socket = tcpSocket
                val out = BufferedWriter(OutputStreamWriter(tcpSocket.getOutputStream()))
                val input = BufferedReader(InputStreamReader(tcpSocket.getInputStream()))
                writer = out
                _status.value = LanConnectionStatus.CONNECTED

                sendEnvelope(
                    LanEnvelope(
                        type = LanMessageType.JOIN_REQUEST,
                        clientId = clientId,
                        playerName = playerName.ifBlank { "Player" }
                    )
                )

                while (!tcpSocket.isClosed) {
                    val line = input.readLine() ?: break
                    val msg = LanJsonCodec.decode(line) ?: continue
                    when (msg.type) {
                        LanMessageType.LOBBY_SNAPSHOT -> {
                            _lobby.value = msg.lobby
                        }
                        LanMessageType.GAME_STATE -> {
                            _gameState.value = msg.gameState
                        }
                        LanMessageType.ERROR_NOTICE -> {
                            _errorMessage.value = msg.errorMessage
                        }
                        else -> Unit
                    }
                }
            } catch (t: Throwable) {
                _errorMessage.value = t.message ?: "Could not connect to $hostIp:$port"
            } finally {
                _status.value = LanConnectionStatus.DISCONNECTED
            }
        }
    }

    fun claimColor(playerName: String, color: PlayerColor) {
        scope.launch {
            sendEnvelope(
                LanEnvelope(
                    type = LanMessageType.CLAIM_COLOR,
                    clientId = clientId,
                    playerName = playerName,
                    color = color
                )
            )
        }
    }

    fun sendRoll(volumePercent: Int) {
        scope.launch {
            sendEnvelope(
                LanEnvelope(
                    type = LanMessageType.ROLL_COMMAND,
                    clientId = clientId,
                    volumePercent = volumePercent
                )
            )
        }
    }

    fun sendMove(tokenId: Int) {
        scope.launch {
            sendEnvelope(
                LanEnvelope(
                    type = LanMessageType.MOVE_COMMAND,
                    clientId = clientId,
                    tokenId = tokenId
                )
            )
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    private fun sendEnvelope(envelope: LanEnvelope) {
        val out = writer ?: return
        val json = LanJsonCodec.encode(envelope)
        try {
            synchronized(out) {
                out.write(json)
                out.newLine()
                out.flush()
            }
        } catch (_: Throwable) {
        }
    }

    fun disconnect() {
        readJob?.cancel()
        readJob = null
        try {
            socket?.close()
        } catch (_: Throwable) {
        }
        socket = null
        writer = null
        _status.value = LanConnectionStatus.DISCONNECTED
        _lobby.value = null
        _gameState.value = null
    }

    fun shutdown() {
        disconnect()
        scope.cancel()
    }
}
