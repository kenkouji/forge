package com.remoteforge.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.*
import okio.ByteString
import java.util.concurrent.atomic.AtomicBoolean

class AudioStreamManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var webSocket: WebSocket? = null

    private val _isSinkActive = MutableStateFlow(false)
    val isSinkActive: StateFlow<Boolean> = _isSinkActive

    private val _isMicActive = MutableStateFlow(false)
    val isMicActive: StateFlow<Boolean> = _isMicActive

    private val _latencyMs = MutableStateFlow(24.0f)
    val latencyMs: StateFlow<Float> = _latencyMs

    private val _volume = MutableStateFlow(1.0f)
    val volume: StateFlow<Float> = _volume

    private var audioTrack: AudioTrack? = null
    private var audioRecord: AudioRecord? = null
    private val isRecording = AtomicBoolean(false)

    private val sampleRate = 48000
    private val channelConfigOut = AudioFormat.CHANNEL_OUT_STEREO
    private val channelConfigIn = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    fun startPlayback(wsUrl: String) {
        if (_isSinkActive.value) return

        try {
            val minBufSize = AudioTrack.getMinBufferSize(sampleRate, channelConfigOut, audioFormat)
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
                .setBufferSizeInBytes(minBufSize * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.setVolume(_volume.value)
            audioTrack?.play()
            _isSinkActive.value = true

            connectWebSocket(wsUrl)
        } catch (e: Exception) {
            Log.e("AudioStreamManager", "Error initializing AudioTrack: ${e.message}")
        }
    }

    fun stopPlayback() {
        _isSinkActive.value = false
        try {
            audioTrack?.stop()
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
                if (_isSinkActive.value && audioTrack != null) {
                    val rawData = bytes.toByteArray()
                    audioTrack?.write(rawData, 0, rawData.size)
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
