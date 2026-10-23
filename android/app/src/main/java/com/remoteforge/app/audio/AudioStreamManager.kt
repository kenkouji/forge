package com.remoteforge.app.audio

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.remoteforge.app.ui.MainActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.*
import okio.ByteString
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class AudioStreamManager private constructor(private val appContext: Context) {

    companion object {
        private const val TAG = "AudioStreamManager"
        private const val NOTIFICATION_ID = 4002
        private const val CHANNEL_ID = "remoteforge_audio_channel"

        @Volatile
        private var instance: AudioStreamManager? = null

        fun getInstance(context: Context): AudioStreamManager {
            return instance ?: synchronized(this) {
                instance ?: AudioStreamManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var webSocket: WebSocket? = null
    private var activeWsUrl: String? = null

    private val _isSinkActive = MutableStateFlow(false)
    val isSinkActive: StateFlow<Boolean> = _isSinkActive

    private val _isMicActive = MutableStateFlow(false)
    val isMicActive: StateFlow<Boolean> = _isMicActive

    private val _latencyMs = MutableStateFlow(12.0f)
    val latencyMs: StateFlow<Float> = _latencyMs

    private val _volume = MutableStateFlow(1.0f)
    val volume: StateFlow<Float> = _volume

    private val _amplitude = MutableStateFlow(0.0f)
    val amplitude: StateFlow<Float> = _amplitude

    private var audioTrack: AudioTrack? = null
    private val isTrackPlaying = AtomicBoolean(false)
    private val preBufferCount = AtomicInteger(0)

    // Hardware match: 48kHz stereo 16-bit PCM for ultra-low latency & clean PC audio
    private val sampleRate = 48000
    private val channelConfigOut = AudioFormat.CHANNEL_OUT_STEREO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    init {
        createNotificationChannel()
    }

    fun startPlayback(wsUrl: String) {
        if (_isSinkActive.value && activeWsUrl == wsUrl) return
        activeWsUrl = wsUrl

        try {
            stopPlaybackInternal()

            val minBufSize = AudioTrack.getMinBufferSize(sampleRate, channelConfigOut, audioFormat)
            val bufferCapacity = maxOf(minBufSize * 2, 16 * 1024)

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

            showAudioNotification()
            connectWebSocket(wsUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing AudioTrack: ${e.message}")
        }
    }

    fun stopPlayback() {
        stopPlaybackInternal()
        hideAudioNotification()
    }

    private fun stopPlaybackInternal() {
        _isSinkActive.value = false
        _amplitude.value = 0.0f
        isTrackPlaying.set(false)
        preBufferCount.set(0)
        activeWsUrl = null

        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.release()
            audioTrack = null
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping AudioTrack: ${e.message}")
        }
        disconnectWebSocket()
    }

    fun setVolume(vol: Float) {
        val clamped = vol.coerceIn(0.0f, 1.0f)
        _volume.value = clamped
        try {
            audioTrack?.setVolume(clamped)
        } catch (_: Exception) {}
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

                // Write to AudioTrack buffer non-blocking for zero latency
                track.write(rawData, 0, rawData.size, AudioTrack.WRITE_NON_BLOCKING)

                // Pre-buffer 2 chunks (~40ms) then immediately play
                val count = preBufferCount.incrementAndGet()
                if (count >= 2 && !isTrackPlaying.get()) {
                    try {
                        track.play()
                        isTrackPlaying.set(true)
                    } catch (e: Exception) {
                        Log.w(TAG, "Error starting track playback: ${e.message}")
                    }
                }

                // Compute RMS amplitude for visualizer
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
                Log.w(TAG, "Audio WebSocket failure: ${t.message}")
                if (_isSinkActive.value && activeWsUrl != null) {
                    // Auto-reconnect in 1.5s if active
                    scope.launch {
                        delay(1500)
                        if (_isSinkActive.value && activeWsUrl != null) {
                            connectWebSocket(activeWsUrl!!)
                        }
                    }
                }
            }
        })
    }

    private fun disconnectWebSocket() {
        try {
            webSocket?.close(1000, "User stopped stream")
            webSocket = null
        } catch (_: Exception) {}
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "RemoteForge Audio Stream",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background streaming of PC sound output to phone/Bluetooth buds"
                setShowBadge(false)
            }
            val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    private fun showAudioNotification() {
        val launchIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            appContext, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setContentTitle("RemoteForge Live Audio")
            .setContentText("PC audio streaming to phone / Bluetooth earbuds")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(NOTIFICATION_ID, notification)
    }

    private fun hideAudioNotification() {
        val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.cancel(NOTIFICATION_ID)
    }
}
