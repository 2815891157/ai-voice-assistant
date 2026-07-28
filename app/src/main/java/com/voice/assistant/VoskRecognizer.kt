package com.voice.assistant

import android.content.Context
import android.util.Log
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService
import java.io.IOException

class VoskRecognizer(private val context: Context) {

    companion object {
        private const val TAG = "VoskRecognizer"
        private const val MODEL_NAME = "vosk-model-small-cn-0.22"
    }

    private var model: Model? = null
    private var speechService: SpeechService? = null
    @Volatile
    private var isInitialized = false

    fun initialize(onReady: () -> Unit, onError: (String) -> Unit) {
        Thread {
            try {
                StorageService.unpack(context, MODEL_NAME, "model",
                    { model ->
                        this.model = model
                        isInitialized = true
                        Log.d(TAG, "Vosk model loaded: $MODEL_NAME")
                        onReady()
                    },
                    { exception ->
                        Log.e(TAG, "Failed to load Vosk model", exception)
                        onError(exception.message ?: "")
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Vosk init error", e)
                onError(e.message ?: "")
            }
        }.start()
    }

    // Bug #23: synchronized  start/stop
    @Synchronized
    fun startListening(listener: RecognitionListener) {
        if (!isInitialized || model == null) {
            Log.w(TAG, "Model not initialized yet")
            return
        }
        try {
            stopListeningInternal()
            val recognizer = Recognizer(model, 16000.0f)
            speechService = SpeechService(recognizer, 16000.0f)
            speechService?.startListening(listener)
        } catch (e: IOException) {
            Log.e(TAG, "Failed to start listening", e)
        }
    }

    @Synchronized
    fun stopListening() {
        stopListeningInternal()
    }

    private fun stopListeningInternal() {
        try {
            speechService?.stop()
            speechService?.shutdown()
            speechService = null
        } catch (_: Exception) {}
    }

    fun isReady(): Boolean = isInitialized && model != null

    @Synchronized
    fun destroy() {
        stopListeningInternal()
        model?.close()
        model = null
        isInitialized = false
    }
}
