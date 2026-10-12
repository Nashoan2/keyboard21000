package org.fossify.keyboard.helpers

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import org.fossify.keyboard.activities.VoicePermissionActivity
import java.util.Locale

class EmbeddedVoiceManager(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    var isListening: Boolean = false
        private set

    private var stateCallback: ((Boolean) -> Unit)? = null
    private var resultCallback: ((String) -> Unit)? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun requestPermission() {
        try {
            val intent = Intent(context, VoicePermissionActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (ignored: Exception) {
        }
    }

    fun startListening(
        onListeningState: (Boolean) -> Unit,
        onResult: (String) -> Unit
    ): Boolean {
        if (!hasPermission()) {
            requestPermission()
            return false
        }

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            return false
        }

        stateCallback = onListeningState
        resultCallback = onResult

        stopListening()

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    isListening = true
                    mainHandler.post { stateCallback?.invoke(true) }
                }

                override fun onBeginningOfSpeech() {}

                override fun onRmsChanged(rmsdB: Float) {}

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {}

                override fun onError(error: Int) {
                    isListening = false
                    mainHandler.post { stateCallback?.invoke(false) }
                }

                override fun onResults(results: Bundle?) {
                    isListening = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val recognizedText = matches?.firstOrNull()?.trim()
                    mainHandler.post {
                        if (!recognizedText.isNullOrEmpty()) {
                            resultCallback?.invoke(recognizedText)
                        }
                        stateCallback?.invoke(false)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {}

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            val isArabic = Config.newInstance(context).keyboardLanguage == LANGUAGE_ARABIC
            val langTag = if (isArabic) "ar-SA" else Locale.getDefault().toLanguageTag()

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, langTag)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, langTag)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            }

            speechRecognizer?.startListening(intent)
            isListening = true
            stateCallback?.invoke(true)
            return true
        } catch (e: Exception) {
            isListening = false
            stateCallback?.invoke(false)
            return false
        }
    }

    fun stopListening() {
        if (isListening) {
            try {
                speechRecognizer?.stopListening()
            } catch (ignored: Exception) {
            }
            isListening = false
            stateCallback?.invoke(false)
        }
    }

    fun destroy() {
        stopListening()
        try {
            speechRecognizer?.destroy()
        } catch (ignored: Exception) {
        }
        speechRecognizer = null
    }
}
