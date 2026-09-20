package com.forge.domain.engine

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

/**
 * Voice Coach + Audio Countdown Engine.
 * Plays concise tactical audio cues with polite transient audio focus ducking.
 * Never permanently hijacks user music playback.
 */
class VoiceCoachEngine(
    context: Context,
    var isVoiceCoachEnabled: Boolean = true,
    var isCountdownBeepsEnabled: Boolean = true
) {
    private val TAG = "FORGE_VoiceCoach"

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var toneGenerator: ToneGenerator? = null
    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false

    private var audioFocusRequest: AudioFocusRequest? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 75)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to initialize ToneGenerator", e)
        }

        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.US)
                if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                    isTtsInitialized = true
                    tts?.setSpeechRate(1.05f) // Energetic athletic delivery
                }
            }
        }

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                abandonAudioFocus()
            }
            override fun onError(utteranceId: String?) {
                abandonAudioFocus()
            }
        })
    }

    fun onWorkoutStart() {
        speak("Let's train.")
    }

    fun onSetComplete(restSeconds: Int) {
        val message = if (restSeconds >= 60) {
            val mins = restSeconds / 60
            val remSecs = restSeconds % 60
            if (remSecs == 0) "Set complete. Rest $mins minute${if (mins > 1) "s" else ""}."
            else "Set complete. Rest $mins minute and $remSecs seconds."
        } else {
            "Set complete. Rest $restSeconds seconds."
        }
        speak(message)
    }

    fun onCountdownTick(remainingSeconds: Int) {
        if (!isCountdownBeepsEnabled) return

        when (remainingSeconds) {
            10 -> {
                playTone(ToneGenerator.TONE_PROP_BEEP)
                if (isVoiceCoachEnabled) {
                    speak("Ten seconds.")
                }
            }
            5, 4, 3, 2, 1 -> {
                playTone(ToneGenerator.TONE_PROP_BEEP)
            }
            0 -> {
                playTone(ToneGenerator.TONE_PROP_PROMPT)
                if (isVoiceCoachEnabled) {
                    speak("Get ready. Next set.")
                }
            }
        }
    }

    fun onRestComplete() {
        speak("Rest complete. Continue with your next set.")
    }

    fun onNextExercise(exerciseName: String) {
        speak("Next exercise: $exerciseName.")
    }

    fun onWorkoutComplete() {
        speak("Workout complete. Great work.")
    }

    fun release() {
        try {
            toneGenerator?.release()
            toneGenerator = null
            tts?.stop()
            tts?.shutdown()
            tts = null
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing audio resources", e)
        }
    }

    private fun playTone(toneType: Int) {
        try {
            toneGenerator?.startTone(toneType, 120)
        } catch (e: Exception) {
            Log.w(TAG, "Tone playback error", e)
        }
    }

    private fun speak(text: String) {
        if (!isVoiceCoachEnabled || !isTtsInitialized) return

        requestAudioFocus()
        val utteranceId = "forge_coach_${System.currentTimeMillis()}"
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    private fun requestAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val playbackAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()

                audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(playbackAttributes)
                    .setAcceptsDelayedFocusGain(false)
                    .build()

                audioFocusRequest?.let { audioManager?.requestAudioFocus(it) }
            } else {
                @Suppress("DEPRECATION")
                audioManager?.requestAudioFocus(
                    null,
                    AudioManager.STREAM_NOTIFICATION,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Audio focus request failed", e)
        }
    }

    private fun abandonAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                audioManager?.abandonAudioFocus(null)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Audio focus abandon failed", e)
        }
    }
}
