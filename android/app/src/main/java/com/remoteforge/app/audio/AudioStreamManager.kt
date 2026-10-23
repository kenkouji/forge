package com.remoteforge.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.*
import okio.ByteString
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class AudioStreamManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var webSocket: WebSocket? = null

    private val _isSinkActive = MutableStateFlow(false)
    val isSinkActive: StateFlow<Boolean> = _isSinkActive

    private val _isMicActive = MutableStateFlow(false)
    val isMicActive: StateFlow<Boolean> = _isMicActive

    private val _latencyMs = MutableStateFlow(18.0f)
    val latencyMs: StateFlow<Float> = _latencyMs

    private val _volume = MutableStateFlow(1.0f)
    val volume: StateFlow<Float> = _volume

    private val _amplitude = MutableStateFlow(0.0f)
    val amplitude: StateFlow<Float> = _amplitude

    private var audioTrack: AudioTrack? = null
    private val isTrackPlaying = AtomicBoolean(false)
    private val preBufferCount = AtomicInteger(0)

    // Match Mac hardware sample rate 44.1kHz stereo
    private val sampleRate = 44100
    private val channelConfigOut = AudioFormat.CHANNEL_OUT_STEREO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    fun startPlayback(wsUrl: String) {
        if (_isSinkActive.value) return

        try {
            val minBufSize = AudioTrack.getMinBufferSize(sampleRate, channelConfigOut, audioFormat)
            val bufferCapacity = maxOf(minBufSize * 4, 32 * 1024)

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(audioFormat)
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelConfigOut)
                        .build()
                )
                .setBufferSizeInBytes(bufferCapacity)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                .build()

            audioTrack?.setVolume(_volume.value)
            isTrackPlaying.set(false)
            preBufferCount.set(0)
            _isSinkActive.value = true

            connectWebSocket(wsUrl)
        } catch (e: Exception) {
            Log.e("AudioStreamManager", "Error initializing AudioTrack: ${e.message}")
        }
    }

    fun stopPlayback() {
        _isSinkActive.value = false
        _amplitude.value = 0.0f
        isTrackPlaying.set(false)
        preBufferCount.set(0)

        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.release()
            audioTrack = null
        } catch (e: Exception) {
            Log.w("AudioStreamManager", "Error stopping AudioTrack: ${e.message}")
        }
        disconnectWebSocket()
    }

    fun setVolume(vol: Float) {
        _volume.value = vol.coerceIn(0.0f, 1.0f)
        try {
            audioTrack?.setVolume(_volume.value)
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun connectWebSocket(url: String) {
        val client = OkHttpClient.Builder().build()
        val request = Request.Builder().url(url).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                val track = audioTrack ?: return
                if (!_isSinkActive.value) return

                val rawData = bytes.toByteArray()
                if (rawData.isEmpty()) return

                // Write to AudioTrack buffer
                track.write(rawData, 0, rawData.size, AudioTrack.WRITE_BLOCKING)

                // Pre-buffer 3 chunks (~60ms) before starting playback to avoid buffer underrun crackles
                val count = preBufferCount.incrementAndGet()
                if (count >= 3 && !isTrackPlaying.get()) {
                    try {
                        track.play()
                        isTrackPlaying.set(true)
                    } catch (e: Exception) {
                        Log.w("AudioStreamManager", "Error starting track playback: ${e.message}")
                    }
                }

                // Compute RMS amplitude for smooth visualizer
                var sumSquares = 0.0
                val sampleCount = rawData.size / 2
                for (i in 0 until rawData.size - 1 step 4) {
                    val sample = (rawData[i].toInt() and 0xFF) or (rawData[i + 1].toInt() shl 8)
                    val shortVal = sample.toShort()
                    sumSquares += shortVal * shortVal
                }
                if (sampleCount > 0) {
                    val rms = Math.sqrt(sumSquares / (sampleCount / 2))
                    val normalized = (rms / 32767.0).toFloat().coerceIn(0f, 1f)
                    _amplitude.value = normalized
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w("AudioStreamManager", "Audio WebSocket failure: ${t.message}")
            }
        })
    }

    private fun disconnectWebSocket() {
        try {
            webSocket?.close(1000, "User stopped stream")
            webSocket = null
        } catch (e: Exception) {
            // Ignore
        }
    }
}
