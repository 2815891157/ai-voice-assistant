package com.voice.assistant

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.RecognitionListener as AndroidRecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.view.*
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.*
import com.voice.assistant.App
import com.voice.assistant.R
import com.voice.assistant.ui.MainActivity
import com.voice.assistant.ui.SettingsActivity
import com.voice.assistant.util.ApiEngine
import com.voice.assistant.util.Prefs
import org.vosk.android.RecognitionListener as VoskRecognitionListener
import java.io.File
import java.util.Locale

class FloatingService : Service() {

    companion object {
        private const val TAG = "FloatingService"
        private const val DOUBLE_TAP_DELAY = 300L
        private const val LOCATION_UPDATE_MS = 30000L
        private const val LOCATION_UPDATE_DIST = 100f
    }

    private var wm: WindowManager? = null
    private var floatView: View? = null
    private var params: WindowManager.LayoutParams? = null
    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var voskRecognizer: VoskRecognizer? = null
    private var locationManager: LocationManager? = null
    private val handler = Handler(Looper.getMainLooper())

    private var isListening = false
    private var useVosk = true
    private var bubbleText: TextView? = null
    private var avatarImg: ImageView? = null
    private var bubbleContainer: LinearLayout? = null
    private var statusDot: View? = null
    private var avatarContainer: LinearLayout? = null
    private var lastTapTime = 0L
    private var isProcessing = false
    @Volatile
    private var ttsReady = false

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            Prefs.saveLastLocation(location.latitude, location.longitude)
        }
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    override fun onCreate() {
        super.onCreate()
        try {
            Prefs.init(this)
            wm = getSystemService(WINDOW_SERVICE) as? WindowManager
            ApiEngine.init(this)
            initTTS()
            initLocation()
            initVosk()
            createFloatingView()
            startListening()
            handler.postDelayed({ showGreeting() }, if (ttsReady) 800 else 2000)
        } catch (e: Exception) {
            Log.e(TAG, "onCreate failed", e)
            try { stopSelf() } catch (_: Exception) {}
        }
    }

    private fun initTTS() {
        try {
            tts = TextToSpeech(this) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val lang = when (Prefs.ttsLang) {
                        "en-US" -> Locale.US
                        "ja-JP" -> Locale.JAPAN
                        "ko-KR" -> Locale.KOREA
                        else -> Locale.CHINESE
                    }
                    ttsReady = true
                        tts?.language = lang
                    tts?.setSpeechRate(Prefs.ttsSpeed)
                    tts?.setPitch(Prefs.ttsPitch)
                }
            }
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {}
                override fun onError(utteranceId: String?) {}
            })
        } catch (e: Exception) {
            Log.e(TAG, "initTTS failed", e)
        }
    }

    private fun initLocation() {
        try {
            locationManager = getSystemService(LOCATION_SERVICE) as? LocationManager
            if (Prefs.useGpsLocation) {
                startLocationUpdates()
            }
        } catch (e: Exception) {
            Log.e(TAG, "initLocation failed", e)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        // Bug #20: 
        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Location permission not granted")
            return
        }
        try {
            val lm = locationManager ?: return
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_UPDATE_MS, LOCATION_UPDATE_DIST, locationListener)
            }
            if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, LOCATION_UPDATE_MS, LOCATION_UPDATE_DIST, locationListener)
            }
            val lastKnown = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            if (lastKnown != null) {
                Prefs.saveLastLocation(lastKnown.latitude, lastKnown.longitude)
            }
        } catch (e: Exception) {
            Log.e(TAG, "startLocationUpdates failed", e)
        }
    }

    private fun initVosk() {
        try {
            voskRecognizer = VoskRecognizer(this)
            voskRecognizer?.initialize(
                onReady = {
                    Log.d(TAG, "Vosk ready")
                    useVosk = true
                },
                onError = {
                    Log.w(TAG, "Vosk failed, using Android SpeechRecognizer")
                    useVosk = false
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "initVosk failed", e)
            useVosk = false
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createFloatingView() {
        try {
            val inflater = LayoutInflater.from(this)
            floatView = inflater.inflate(R.layout.floating_assistant, null)
            bubbleText = floatView?.findViewById(R.id.bubble_text)
            avatarImg = floatView?.findViewById(R.id.avatar_image)
            bubbleContainer = floatView?.findViewById(R.id.bubble_container)
            statusDot = floatView?.findViewById(R.id.status_dot)
            avatarContainer = floatView?.findViewById(R.id.avatar_container)
            loadAvatar()

            val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

            params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                overlayType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply { gravity = Gravity.TOP or Gravity.START; x = 50; y = 400 }

            var lastX = 0f; var lastY = 0f; var downX = 0f; var downY = 0f; var dragging = false

            floatView?.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> { lastX = event.rawX; lastY = event.rawY; downX = event.rawX; downY = event.rawY; dragging = false; true }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - lastX; val dy = event.rawY - lastY
                        if (Math.abs(event.rawX - downX) > 8 || Math.abs(event.rawY - downY) > 8) dragging = true
                        params?.x = (params?.x ?: 0) + dx.toInt(); params?.y = (params?.y ?: 0) + dy.toInt()
                        try { wm?.updateViewLayout(floatView, params) } catch (_: Exception) {}
                        lastX = event.rawX; lastY = event.rawY; true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!dragging) {
                            val now = System.currentTimeMillis()
                            if (now - lastTapTime < DOUBLE_TAP_DELAY) { onAvatarDoubleTap(); lastTapTime = 0L }
                            else { lastTapTime = now; handler.postDelayed({ if (lastTapTime == now) onAvatarClicked() }, DOUBLE_TAP_DELAY) }
                        }; true
                    }
                    else -> false
                }
            }
            wm?.addView(floatView, params)
        } catch (e: Exception) { Log.e(TAG, "createFloatingView failed", e) }
    }

    private fun loadAvatar() {
        try {
            val customPath = Prefs.customAvatarPath
            if (customPath.isNotEmpty()) {
                val file = File(customPath)
                if (file.exists()) { avatarImg?.setImageURI(android.net.Uri.fromFile(file)); return }
            }
            avatarImg?.setImageResource(R.drawable.ic_floating_avatar)
        } catch (e: Exception) { Log.e(TAG, "loadAvatar failed", e) }
    }

    private fun onAvatarClicked() {
        vibrate()
        if (isListening) { stopListening(); showBubble("~") }
        else startListening()
    }

    private fun onAvatarDoubleTap() {
        vibrate()
        showBubble(" ")
        val intent = Intent(this, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    private fun vibrate() {
        if (!Prefs.hapticFeedback) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                getSystemService(VibratorManager::class.java)?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION") (getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)?.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        } catch (_: Exception) {}
    }

    //   
    private fun startListening() {
        if (isListening) return
        isListening = true
        updateStatus(true)

        if (useVosk && voskRecognizer?.isReady() == true) {
            startVoskListening()
        } else {
            startAndroidListening()
        }
    }

    private fun startVoskListening() {
        voskRecognizer?.startListening(object : VoskRecognitionListener {
            override fun onPartialResult(hypothesis: String?) {}
            override fun onResult(result: String?) {
                isListening = false
                updateStatus(false)
                result?.let {
                    try {
                        val json = org.json.JSONObject(it)
                        val text = json.optString("text", "")
                        if (text.isNotEmpty()) handler.post { handleUserInput(text) }
                        else handler.postDelayed({ if (Prefs.wakeWordEnabled) startListening() }, 1000)
                    } catch (_: Exception) {
                        if (it.isNotEmpty()) handler.post { handleUserInput(it) }
                    }
                }
            }
            override fun onFinalResult(hypothesis: String?) {
                isListening = false
                updateStatus(false)
                hypothesis?.let {
                    try {
                        val json = org.json.JSONObject(it)
                        val text = json.optString("text", "")
                        if (text.isNotEmpty()) handler.post { handleUserInput(text) }
                    } catch (_: Exception) {}
                }
            }
            override fun onError(exception: Exception?) {
                isListening = false
                updateStatus(false)
                handler.postDelayed({ if (Prefs.wakeWordEnabled) startListening() }, 1000)
            }
            override fun onTimeout() {
                isListening = false
                updateStatus(false)
                handler.postDelayed({ if (Prefs.wakeWordEnabled) startListening() }, 1000)
            }
        })
    }

    private fun startAndroidListening() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            if (speechRecognizer == null) { showBubble(""); isListening = false; updateStatus(false); return }

            speechRecognizer?.setRecognitionListener(object : AndroidRecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {
                    handler.post {
                        val scale = 1f + (rmsdB + 10f).coerceIn(0f, 20f) / 60f
                        avatarContainer?.scaleX = scale; avatarContainer?.scaleY = scale
                    }
                }
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    isListening = false; updateStatus(false)
                    handler.post { avatarContainer?.scaleX = 1f; avatarContainer?.scaleY = 1f }
                }
                override fun onPartialResults(p: Bundle?) {}
                override fun onEvent(t: Int, p: Bundle?) {}
                override fun onResults(results: Bundle?) {
                    try {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) handler.post { handleUserInput(matches[0]) }
                        else handler.postDelayed({ hideBubble(); if (Prefs.wakeWordEnabled) startListening() }, 2000)
                    } catch (e: Exception) { Log.e(TAG, "onResults failed", e) }
                }
                override fun onError(error: Int) {
                    isListening = false; updateStatus(false)
                    handler.post { avatarContainer?.scaleX = 1f; avatarContainer?.scaleY = 1f }
                    if (error != SpeechRecognizer.ERROR_NO_MATCH && error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) Log.e(TAG, "SpeechRecognizer error: $error")
                    handler.postDelayed({ if (Prefs.wakeWordEnabled) startListening() }, 1000)
                }
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "startAndroidListening failed", e)
            isListening = false; updateStatus(false)
        }
    }

    private fun stopListening() {
        isListening = false; updateStatus(false)
        try { speechRecognizer?.stopListening() } catch (_: Exception) {}
        try { voskRecognizer?.stopListening() } catch (_: Exception) {}
        handler.post { avatarContainer?.scaleX = 1f; avatarContainer?.scaleY = 1f }
    }

    //   
    private fun handleUserInput(text: String) {
        try {
            showBubble(": $text")
            if (Prefs.wakeWordEnabled && text.contains(Prefs.wakeWord, ignoreCase = true)) {
                val wakeIdx = text.indexOf(Prefs.wakeWord, ignoreCase = true)
                val cmd = if (wakeIdx >= 0) text.substring(wakeIdx + Prefs.wakeWord.length).trim() else ""
                if (cmd.isNotEmpty()) processCommand(cmd)
                else { showBubble("~"); handler.postDelayed({ startListening() }, 800) }
                return
            }
            if (Prefs.alwaysRespond || !Prefs.wakeWordEnabled) processCommand(text)
            else { showBubble("\"${Prefs.wakeWord}\"~"); handler.postDelayed({ hideBubble(); if (Prefs.wakeWordEnabled) startListening() }, 2500) }
        } catch (e: Exception) { Log.e(TAG, "handleUserInput failed", e) }
    }

    private fun processCommand(text: String) {
        if (isProcessing) { showBubble("..."); return }
        isProcessing = true
        showBubble("...")
        Thread {
            try {
                val (reply, action) = ApiEngine.process(text, this@FloatingService)
                handler.post {
                    try {
                        showBubble(reply)
                        speak(reply)

                        when {
                            action?.startsWith("LAUNCH:") == true -> {
                                val pkg = action.removePrefix("LAUNCH:")
                                val intent = packageManager.getLaunchIntentForPackage(pkg)
                                if (intent != null) { intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(intent) }
                                else showBubble("")
                            }
                            action?.startsWith("MUSIC_ORPHEUS:") == true -> {
                                val songId = action.removePrefix("MUSIC_ORPHEUS:")
                                val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("orpheus://song?id=$songId"))
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                try { startActivity(intent) } catch (_: Exception) { showBubble("") }
                            }
                            action == "OPEN_SETTINGS" -> {
                                val intent = Intent(this@FloatingService, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                startActivity(intent)
                            }
                        }
                        handler.postDelayed({ isProcessing = false; if (Prefs.wakeWordEnabled) startListening() }, 2000)
                    } catch (e: Exception) {
                        handler.postDelayed({ isProcessing = false; if (Prefs.wakeWordEnabled) startListening() }, 2000)
                        Log.e(TAG, "processCommand UI failed", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "processCommand failed", e)
                handler.post { showBubble(""); handler.postDelayed({ if (Prefs.wakeWordEnabled) startListening() }, 2000) }
            }
        }.start()
    }

    private fun speak(text: String) {
        try {
            val cleanText = text.replace(Regex("\\[.*?\\]"), "").trim()
            if (cleanText.isEmpty()) return
            tts?.setSpeechRate(Prefs.ttsSpeed); tts?.setPitch(Prefs.ttsPitch)
            tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "utter_" + System.currentTimeMillis())
        } catch (e: Exception) { Log.e(TAG, "speak failed", e) }
    }

    private fun showBubble(text: String) {
        try {
            bubbleContainer?.visibility = View.VISIBLE; bubbleContainer?.alpha = 0f; bubbleContainer?.translationY = -10f
            bubbleContainer?.animate()?.alpha(1f)?.translationY(0f)?.setDuration(200)?.setInterpolator(OvershootInterpolator(1.2f))?.start()
            bubbleText?.text = text
            handler.postDelayed({ hideBubble() }, Prefs.bubbleTimeout * 1000L)
        } catch (_: Exception) {}
    }

    private fun hideBubble() {
        try { bubbleContainer?.animate()?.alpha(0f)?.translationY(-10f)?.setDuration(150)?.setInterpolator(AccelerateDecelerateInterpolator())?.withEndAction { bubbleContainer?.visibility = View.GONE }?.start() } catch (_: Exception) {}
    }

    private fun updateStatus(listening: Boolean) {
        try { statusDot?.setBackgroundResource(if (listening) R.drawable.dot_active else R.drawable.dot_inactive) } catch (_: Exception) {}
    }

    private fun showGreeting() { showBubble(Prefs.greeting); speak(Prefs.greeting) }

    private fun createNotification(): Notification {
        val pending = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return Notification.Builder(this, App.CHANNEL_ID).setContentTitle("").setContentText("").setSmallIcon(R.drawable.ic_notification).setContentIntent(pending).setOngoing(true).build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            if (intent?.action == "STOP") {
                if (Build.VERSION.SDK_INT >= 24) stopForeground(STOP_FOREGROUND_REMOVE) else @Suppress("DEPRECATION") stopForeground(true)
                stopSelf(); return START_NOT_STICKY
            }
            startForeground(1, createNotification())
        } catch (e: Exception) { Log.e(TAG, "onStartCommand failed", e) }
        return START_STICKY
    }

    override fun onDestroy() {
        try { tts?.stop(); tts?.shutdown() } catch (_: Exception) {}
        try { speechRecognizer?.destroy() } catch (_: Exception) {}
        try { voskRecognizer?.destroy() } catch (_: Exception) {}
        try { locationManager?.removeUpdates(locationListener) } catch (_: Exception) {}
        try { floatView?.let { wm?.removeView(it) } } catch (_: Exception) {}
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
