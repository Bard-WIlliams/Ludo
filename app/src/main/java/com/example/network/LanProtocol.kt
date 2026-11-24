package com.example.network

import com.example.domain.GameMode
import com.example.domain.GameState
import com.example.domain.PlayerColor
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@JsonClass(generateAdapter = true)
data class LanLobbySlot(
    val color: PlayerColor,
    val occupantClientId: String? = null,
    val occupantName: String = "",
    val isActive: Boolean = false
)

@JsonClass(generateAdapter = true)
data class LobbySnapshot(
    val roomName: String,
    val hostIp: String,
    val port: Int,
    val mode: GameMode,
    val slots: List<LanLobbySlot>,
    val gameStarted: Boolean = false
)

enum class LanMessageType {
    JOIN_REQUEST,
    CLAIM_COLOR,
    LOBBY_SNAPSHOT,
    GAME_STATE,
    ROLL_COMMAND,
    MOVE_COMMAND,
    REMATCH_COMMAND,
    ERROR_NOTICE
}

@JsonClass(generateAdapter = true)
data class LanEnvelope(
    val type: LanMessageType,
    val clientId: String = "",
    val playerName: String = "",
    val color: PlayerColor? = null,
    val tokenId: Int? = null,
    val volumePercent: Int = 75,
    val lobby: LobbySnapshot? = null,
    val gameState: GameState? = null,
    val errorMessage: String? = null
)

object LanJsonCodec {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val adapter = moshi.adapter(LanEnvelope::class.java)

    fun encode(envelope: LanEnvelope): String = adapter.toJson(envelope)

    fun decode(raw: String): LanEnvelope? = try {
        adapter.fromJson(raw)
    } catch (_: Throwable) {
        null
    }
}
