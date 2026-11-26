package com.klyvoravault.ai

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sqrt

class MainActivity : AppCompatActivity(), SensorEventListener, TextToSpeech.OnInitListener {

    private lateinit var tvStatus: TextView
    private lateinit var tvHeard: TextView
    private lateinit var tvTheftStatus: TextView
    private lateinit var btnListener: Button
    private lateinit var btnGuard: Button
    private lateinit var etWakeWord: EditText

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var cameraManager: CameraManager? = null
    private var cameraId: String? = null
    private var isTorchOn = false

    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var speechRecognizer: SpeechRecognizer? = null

    private var isListening = false
    private var requireWakeWord = false
    private var wakeWord = "klyvora"
    private var isGuardArmed = false
    private var lastShakeTime = 0L
    private var selectedLangTag = "hi-IN"

    private val handler = Handler(Looper.getMainLooper())

    private val micPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startVoiceEngine()
        } else {
            isListening = false
            syncListenerButton()
            setStatus("Microphone permission allow karein.", "#F87171", speak = false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tts = TextToSpeech(this, this)
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as? CameraManager
        try {
            cameraId = cameraManager?.cameraIdList?.firstOrNull { id ->
                cameraManager?.getCameraCharacteristics(id)
                    ?.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (_: Exception) {
            cameraId = null
        }

        val prefs = getSharedPreferences("KlyvoraVaultSession", Context.MODE_PRIVATE)
        isGuardArmed = prefs.getBoolean("anti_theft_armed", false)
        requireWakeWord = prefs.getBoolean("require_wake_word", false)
        wakeWord = prefs.getString("wake_word", "klyvora")?.lowercase(Locale.ROOT) ?: "klyvora"
        selectedLangTag = prefs.getString("voice_lang", "hi-IN") ?: "hi-IN"

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val d = resources.displayMetrics.density
        fun dp(v: Float): Int = (v * d).toInt()

        fun box(bgHex: String, strokeHex: String? = null, radiusDp: Float = 16f): GradientDrawable {
            return GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(radiusDp).toFloat()
                setColor(Color.parseColor(bgHex))
                if (strokeHex != null) {
                    setStroke(dp(1.2f).coerceAtLeast(1), Color.parseColor(strokeHex))
                }
            }
        }

        fun row(): LinearLayout {
            return LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(10f) }
            }
        }

        fun actionBtn(label: String, bgHex: String, weight: Float = 1f, onClick: () -> Unit): Button {
            return Button(this).apply {
                text = label
                setTextColor(Color.WHITE)
                textSize = 13f
                typeface = Typeface.DEFAULT_BOLD
                isAllCaps = false
                background = box(bgHex, null, 12f)
                setPadding(dp(12f), dp(12f), dp(12f), dp(12f))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight).apply {
                    leftMargin = dp(4f)
                    rightMargin = dp(4f)
                }
                setOnClickListener { onClick() }
            }
        }

        val scroll = ScrollView(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(
                    Color.parseColor("#0A1326"),
                    Color.parseColor("#0D162B"),
                    Color.parseColor("#070B14")
                )
            )
            isFillViewport = true
        }

        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18f), dp(22f), dp(18f), dp(28f))
        }

        // Top Brand Bar
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = box("#111C33", "#2563EB", 20f)
            setPadding(dp(18f), dp(16f), dp(18f), dp(16f))
        }
        header.addView(TextView(this).apply {
            text = "KLYVORA VAULT"
            setTextColor(Color.parseColor("#93C5FD"))
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.14f
        })
        header.addView(TextView(this).apply {
            text = "Home Control"
            setTextColor(Color.WHITE)
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dp(4f), 0, 0)
        })

        // 1. Voice Control Card (Direct & Instant)
        val voiceCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = box("#0F172A", "#1E293B", 20f)
            setPadding(dp(18f), dp(18f), dp(18f), dp(18f))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(14f) }
        }

        voiceCard.addView(TextView(this).apply {
            text = "VOICE ASSISTANT"
            setTextColor(Color.parseColor("#93C5FD"))
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
        })

        tvStatus = TextView(this).apply {
            text = "Voice: OFF (Direct command mode)"
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 13f
            setPadding(0, dp(6f), 0, dp(4f))
        }

        tvHeard = TextView(this).apply {
            text = "Last spoken: None"
            setTextColor(Color.parseColor("#CBD5E1"))
            textSize = 12f
            setPadding(0, 0, 0, dp(10f))
        }

        btnListener = actionBtn("Start Voice Control", "#059669", 1f) {
            if (isListening) {
                stopVoiceEngine()
            } else {
                requestMicAndStart()
            }
        }

        val wakeToggleRow = row()
        val btnWakeMode = actionBtn(
            if (requireWakeWord) "Wake Word: ON ($wakeWord)" else "Wake Word: OFF (Any voice)",
            if (requireWakeWord) "#1D4ED8" else "#1E293B",
            1f
        ) {
            requireWakeWord = !requireWakeWord
            prefs.edit().putBoolean("require_wake_word", requireWakeWord).apply()
            btnWakeMode.text = if (requireWakeWord) "Wake Word: ON ($wakeWord)" else "Wake Word: OFF (Any voice)"
            btnWakeMode.setBackgroundColor(Color.parseColor(if (requireWakeWord) "#1D4ED8" else "#1E293B"))
            setStatus(
                if (requireWakeWord) "Sirf \"$wakeWord\" ke saath command chalega." else "Har voice command turant chalega.",
                "#60A5FA",
                speak = false
            )
        }
        wakeToggleRow.addView(btnWakeMode)

        val wakeEditRow = row()
        etWakeWord = EditText(this).apply {
            hint = "Wake word (e.g. klyvora)"
            setText(wakeWord)
            setTextColor(Color.WHITE)
            setHintTextColor(Color.parseColor("#64748B"))
            textSize = 12f
            background = box("#090E1A", "#263244", 11f)
            setPadding(dp(12f), dp(10f), dp(12f), dp(10f))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                rightMargin = dp(6f)
            }
        }
        wakeEditRow.addView(etWakeWord)
        wakeEditRow.addView(actionBtn("Save", "#3730A3", 0.4f) {
            val w = etWakeWord.text.toString().trim().lowercase(Locale.ROOT)
            if (w.isNotEmpty()) {
                wakeWord = w
                prefs.edit().putString("wake_word", wakeWord).apply()
                if (requireWakeWord) {
                    btnWakeMode.text = "Wake Word: ON ($wakeWord)"
                }
                setStatus("Wake word \"$wakeWord\" save ho gaya.", "#34D399", speak = false)
            }
        })

        voiceCard.addView(btnListener)
        voiceCard.addView(wakeToggleRow)
        voiceCard.addView(wakeEditRow)

        // 2. Language & Access Card
        val langAccessCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = box("#0F172A", "#1E293B", 20f)
            setPadding(dp(18f), dp(18f), dp(18f), dp(18f))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(14f) }
        }

        langAccessCard.addView(TextView(this).apply {
            text = "LANGUAGE & ACCESS"
            setTextColor(Color.parseColor("#C4B5FD"))
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
        })

        val langRow = row()
        langRow.addView(actionBtn("Hindi (हिन्दी)", if (selectedLangTag == "hi-IN") "#4F46E5" else "#1E293B") {
            selectedLangTag = "hi-IN"
            prefs.edit().putString("voice_lang", selectedLangTag).apply()
            tts?.language = Locale("hi", "IN")
            setStatus("Bhasha Hindi set ki gayi.", "#C4B5FD", speak = false)
        })
        langRow.addView(actionBtn("English", if (selectedLangTag == "en-IN") "#4F46E5" else "#1E293B") {
            selectedLangTag = "en-IN"
            prefs.edit().putString("voice_lang", selectedLangTag).apply()
            tts?.language = Locale("en", "IN")
            setStatus("Language set to English.", "#C4B5FD", speak = false)
        })

        val accessRow = row()
        accessRow.addView(actionBtn("App Permissions", "#1E293B") {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        })
        accessRow.addView(actionBtn("Open YouTube", "#1D4ED8") {
            openYouTube()
        })

        langAccessCard.addView(langRow)
        langAccessCard.addView(accessRow)

        // 3. Anti-Theft Guard Card
        val theftCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = box("#0F172A", "#1E293B", 20f)
            setPadding(dp(18f), dp(18f), dp(18f), dp(18f))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(14f) }
        }

        theftCard.addView(TextView(this).apply {
            text = "ANTI-THEFT GUARD"
            setTextColor(Color.parseColor("#6EE7B7"))
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
        })

        tvTheftStatus = TextView(this).apply {
            text = if (isGuardArmed) "Status: ARMED (Motion alert ON)" else "Status: OFF"
            setTextColor(Color.parseColor(if (isGuardArmed) "#34D399" else "#94A3B8"))
            textSize = 13f
            setPadding(0, dp(6f), 0, dp(10f))
        }

        btnGuard = actionBtn(
            if (isGuardArmed) "Turn OFF Guard" else "Turn ON Guard",
            if (isGuardArmed) "#B91C1C" else "#059669",
            1f
        ) {
            toggleGuard(prefs)
        }

        val theftExtrasRow = row()
        theftExtrasRow.addView(actionBtn("Flashlight", "#1E293B") { toggleTorch() })
        theftExtrasRow.addView(actionBtn("Alert Siren", "#1E293B") {
            playSiren()
            setStatus("Siren alert bajaya gaya.", "#F87171", speak = false)
        })

        theftCard.addView(tvTheftStatus)
        theftCard.addView(btnGuard)
        theftCard.addView(theftExtrasRow)

        val btnSignOut = Button(this).apply {
            text = "Sign Out"
            setTextColor(Color.parseColor("#FCA5A5"))
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            isAllCaps = false
            background = box("#1A1219", "#7F1D1D", 12f)
            setPadding(dp(14f), dp(11f), dp(14f), dp(11f))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(18f) }
            setOnClickListener {
                stopVoiceEngine()
                prefs.edit().putBoolean("is_logged_in", false).apply()
                startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                finish()
            }
        }

        page.addView(header)
        page.addView(tvStatus)
        page.addView(tvHeard)
        page.addView(voiceCard)
        page.addView(langAccessCard)
        page.addView(theftCard)
        page.addView(btnSignOut)

        scroll.addView(page)
        setContentView(scroll)
    }

    private fun requestMicAndStart() {
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            startVoiceEngine()
        } else {
            micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startVoiceEngine() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            setStatus("Speech recognizer available nahi hai.", "#F87171", speak = false)
            return
        }
        isListening = true
        syncListenerButton()
        setStatus("Voice ON: Ab jo command bolein, turant chalega.", "#34D399", speak = false)
        listenCycle()
    }

    private fun stopVoiceEngine() {
        isListening = false
        handler.removeCallbacksAndMessages(null)
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (_: Exception) {
        }
        speechRecognizer = null
        syncListenerButton()
        setStatus("Voice OFF.", "#94A3B8", speak = false)
    }

    private fun syncListenerButton() {
        btnListener.text = if (isListening) "Stop Voice Control" else "Start Voice Control"
        btnListener.setBackgroundColor(Color.parseColor(if (isListening) "#B91C1C" else "#059669"))
    }

    private fun listenCycle() {
        if (!isListening) return
        try {
            speechRecognizer?.destroy()
            val recognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer = recognizer

            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    if (isListening) {
                        setStatus("Sun raha hai... Command bolein.", "#60A5FA", speak = false)
                    }
                }

                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() = Unit

                override fun onError(error: Int) {
                    if (isListening) {
                        handler.postDelayed({ listenCycle() }, 600L)
                    }
                }

                override fun onResults(results: Bundle?) {
                    val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()?.trim().orEmpty()
                    if (text.isNotEmpty()) {
                        tvHeard.text = "Last spoken: \"$text\""
                        runSpokenCommand(text)
                    }
                    if (isListening) {
                        handler.postDelayed({ listenCycle() }, 400L)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, selectedLangTag)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            recognizer.startListening(intent)
        } catch (_: Exception) {
            if (isListening) {
                handler.postDelayed({ listenCycle() }, 900L)
            }
        }
    }

    private fun runSpokenCommand(spoken: String) {
        val lower = spoken.lowercase(Locale.ROOT)
        if (requireWakeWord && !lower.contains(wakeWord)) {
            setStatus("Wake word \"$wakeWord\" nahi mila. Sunna jari hai...", "#94A3B8", speak = false)
            return
        }

        val prefs = getSharedPreferences("KlyvoraVaultSession", Context.MODE_PRIVATE)
        when {
            lower.contains("youtube") || lower.contains("yt") || lower.contains("यूट्यूब") -> {
                openYouTube()
            }
            lower.contains("torch") || lower.contains("flash") || lower.contains("लाइट") -> {
                toggleTorch()
            }
            lower.contains("alarm") || lower.contains("siren") || lower.contains("alert") || lower.contains("सायरन") -> {
                playSiren()
                setStatus("Alert siren bajaya gaya.", "#F87171", speak = true)
            }
            lower.contains("disarm") || lower.contains("guard off") || lower.contains("बंद") -> {
                if (isGuardArmed) toggleGuard(prefs)
                else setStatus("Guard pehle se OFF hai.", "#FBBF24", speak = false)
            }
            lower.contains("arm") || lower.contains("guard") || lower.contains("सुरक्षा") -> {
                if (!isGuardArmed) toggleGuard(prefs)
                else setStatus("Guard pehle se ON hai.", "#34D399", speak = false)
            }
            lower.contains("battery") || lower.contains("बैटरी") -> {
                val bm = getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
                val pct = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
                if (pct >= 0) {
                    setStatus("Battery $pct% hai.", "#34D399", speak = true)
                }
            }
            lower.contains("time") || lower.contains("samay") || lower.contains("समय") -> {
                val now = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                setStatus("Samay: $now", "#60A5FA", speak = true)
            }
            else -> {
                setStatus("Suna: \"$spoken\"", "#60A5FA", speak = false)
            }
        }
    }

    private fun openYouTube() {
        try {
            val intent = packageManager.getLaunchIntentForPackage("com.google.android.youtube")
                ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://m.youtube.com"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            setStatus("YouTube open kiya ja raha hai.", "#34D399", speak = false)
        } catch (_: Exception) {
            setStatus("YouTube open nahi ho saka.", "#F87171", speak = false)
        }
    }

    private fun toggleTorch() {
        val id = cameraId
        if (id == null || cameraManager == null) {
            setStatus("Flashlight available nahi hai.", "#F87171", speak = false)
            return
        }
        try {
            isTorchOn = !isTorchOn
            cameraManager?.setTorchMode(id, isTorchOn)
            setStatus(
                if (isTorchOn) "Flashlight ON." else "Flashlight OFF.",
                if (isTorchOn) "#34D399" else "#FBBF24",
                speak = false
            )
        } catch (_: Exception) {
            setStatus("Flashlight control nahi ho saka.", "#F87171", speak = false)
        }
    }

    private fun toggleGuard(prefs: android.content.SharedPreferences) {
        isGuardArmed = !isGuardArmed
        prefs.edit().putBoolean("anti_theft_armed", isGuardArmed).apply()
        tvTheftStatus.text = if (isGuardArmed) "Status: ARMED (Motion alert ON)" else "Status: OFF"
        tvTheftStatus.setTextColor(Color.parseColor(if (isGuardArmed) "#34D399" else "#94A3B8"))
        btnGuard.text = if (isGuardArmed) "Turn OFF Guard" else "Turn ON Guard"
        btnGuard.setBackgroundColor(Color.parseColor(if (isGuardArmed) "#B91C1C" else "#059669"))
        setStatus(
            if (isGuardArmed) "Anti-Theft Guard ON." else "Anti-Theft Guard OFF.",
            if (isGuardArmed) "#34D399" else "#FBBF24",
            speak = true
        )
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.forLanguageTag(selectedLangTag)
            isTtsReady = true
        }
    }

    private fun speak(text: String) {
        if (isTtsReady && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "KV_SAY")
        }
    }

    private fun playSiren() {
        try {
            val tg = ToneGenerator(AudioManager.STREAM_ALARM, 100)
            tg.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 600)
        } catch (_: Exception) {
        }
    }

    private fun setStatus(msg: String, hex: String, speak: Boolean) {
        tvStatus.text = msg
        tvStatus.setTextColor(Color.parseColor(hex))
        if (speak) speak(msg)
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
    }

    override fun onDestroy() {
        stopVoiceEngine()
        if (isTorchOn && cameraId != null) {
            try {
                cameraManager?.setTorchMode(cameraId, false)
            } catch (_: Exception) {
            }
        }
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!isGuardArmed || event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val gForce = sqrt((x * x + y * y + z * z).toDouble()) / SensorManager.GRAVITY_EARTH
        if (gForce > 2.2) {
            val now = System.currentTimeMillis()
            if (now - lastShakeTime > 2000L) {
                lastShakeTime = now
                playSiren()
                setStatus("ALERT: Phone movement detect hua!", "#F87171", speak = true)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
