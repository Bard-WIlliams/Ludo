package com.example.data

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class SoundEngine {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    var volumePercent: Int = SoundPreferences.DEFAULT_VOLUME

    fun playDiceRoll() {
        val vol = volumePercent
        if (vol <= 0) return
        scope.launch {
            playToneBurst(freqHz = 360.0, durationMs = 45, amplitude = vol / 100f * 0.25f)
            playToneBurst(freqHz = 490.0, durationMs = 55, amplitude = vol / 100f * 0.30f)
        }
    }

    fun playTokenStep() {
        val vol = volumePercent
        if (vol <= 0) return
        scope.launch {
            playToneBurst(freqHz = 540.0, durationMs = 38, amplitude = vol / 100f * 0.28f)
        }
    }

    fun playCapture() {
        val vol = volumePercent
        if (vol <= 0) return
        scope.launch {
            playToneBurst(freqHz = 220.0, durationMs = 110, amplitude = vol / 100f * 0.42f)
            playToneBurst(freqHz = 165.0, durationMs = 120, amplitude = vol / 100f * 0.35f)
        }
    }

    fun playHomeFinish() {
        val vol = volumePercent
        if (vol <= 0) return
        scope.launch {
            val scale = vol / 100f * 0.35f
            playToneBurst(freqHz = 523.25, durationMs = 90, amplitude = scale)
            playToneBurst(freqHz = 659.25, durationMs = 90, amplitude = scale)
            playToneBurst(freqHz = 783.99, durationMs = 140, amplitude = scale)
        }
    }

    fun playVictory() {
        val vol = volumePercent
        if (vol <= 0) return
        scope.launch {
            val scale = vol / 100f * 0.38f
            playToneBurst(freqHz = 523.25, durationMs = 110, amplitude = scale)
            playToneBurst(freqHz = 659.25, durationMs = 110, amplitude = scale)
            playToneBurst(freqHz = 783.99, durationMs = 120, amplitude = scale)
            playToneBurst(freqHz = 1046.50, durationMs = 240, amplitude = scale)
        }
    }

    private fun playToneBurst(freqHz: Double, durationMs: Int, amplitude: Float) {
        val sampleRate = 22050
        val count = (sampleRate * durationMs) / 1000
        if (count <= 0) return

        val samples = ShortArray(count)
        for (i in 0 until count) {
            val t = i.toDouble() / sampleRate
            val envelope = exp(-6.0 * i / count)
            val wave = sin(2.0 * PI * freqHz * t) * envelope * amplitude
            samples[i] = (wave * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        val minBuffer = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(count * 2)

        var track: AudioTrack? = null
        try {
            track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBuffer)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(samples, 0, count)
            track.play()
            Thread.sleep(durationMs.toLong() + 15L)
        } catch (_: Throwable) {
            // Safe no-op on environments without audio hardware
        } finally {
            try {
                track?.stop()
                track?.release()
            } catch (_: Throwable) {
            }
        }
    }
}
