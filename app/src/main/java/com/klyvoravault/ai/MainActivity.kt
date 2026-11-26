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
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sqrt

class MainActivity : AppCompatActivity(), SensorEventListener, TextToSpeech.OnInitListener {

    private lateinit var tvEngineState: TextView
    private lateinit var tvLastHeard: TextView
    private lateinit var tvTheftStatus: TextView
    private lateinit var btnEngineToggle: Button
    private lateinit var btnArmGuard: Button
    private lateinit var tvRulesSummary: TextView
    private lateinit var etWakeWord: EditText
    private lateinit var etCommandWord: EditText
    private lateinit var etCommandAction: EditText
    private lateinit var etManualCmd: EditText

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListeningEngineActive = false
    private var isGuardArmed = false
    private var wakeWord = "klyvora"
    private var lastShakeTime = 0L

    private val mainHandler = Handler(Looper.getMainLooper())
    private val commandRules = mutableListOf<Pair<String, String>>()

    private val audioPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startContinuousEngine()
        } else {
            isListeningEngineActive = false
            updateEngineButton()
            setEngineState("Microphone permission denied", "#F87171")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tts = TextToSpeech(this, this)
        val prefs = getSharedPreferences("KlyvoraVaultSession", Context.MODE_PRIVATE)
        val accountLabel = prefs.getString("user_email", null)
            ?: prefs.getString("user_phone", null)
            ?: "Protected Device"

        isGuardArmed = prefs.getBoolean("anti_theft_armed", false)
        wakeWord = prefs.getString("wake_word", "klyvora")?.lowercase(Locale.ROOT) ?: "klyvora"
        loadRules(prefs)

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

        fun card(badge: String, badgeBg: String, badgeFg: String, title: String): LinearLayout {
            return LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = box("#0F172A", "#1E293B", 20f)
                setPadding(dp(16f), dp(16f), dp(16f), dp(16f))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(14f) }

                addView(TextView(this@MainActivity).apply {
                    text = badge
                    setTextColor(Color.parseColor(badgeFg))
                    textSize = 10f
                    typeface = Typeface.DEFAULT_BOLD
                    background = box(badgeBg, null, 8f)
                    setPadding(dp(9f), dp(4f), dp(9f), dp(4f))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                })

                addView(TextView(this@MainActivity).apply {
                    text = title
                    setTextColor(Color.parseColor("#F8FAFC"))
                    textSize = 16f
                    typeface = Typeface.DEFAULT_BOLD
                    setPadding(0, dp(8f), 0, dp(6f))
                })
            }
        }

        fun row(): LinearLayout {
            return LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(8f) }
            }
        }

        fun btn(label: String, bgHex: String = "#1D4ED8", weight: Float = 1f, onClick: () -> Unit): Button {
            return Button(this).apply {
                text = label
                setTextColor(Color.WHITE)
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
                isAllCaps = false
                background = box(bgHex, null, 11f)
                setPadding(dp(10f), dp(10f), dp(10f), dp(10f))
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
            setPadding(dp(16f), dp(20f), dp(16f), dp(28f))
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = box("#111C33", "#2563EB", 20f)
            setPadding(dp(16f), dp(16f), dp(16f), dp(16f))
        }
        header.addView(TextView(this).apply {
            text = "KLYVORA VAULT"
            setTextColor(Color.parseColor("#93C5FD"))
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.14f
        })
        header.addView(TextView(this).apply {
            text = "Autonomous Voice & Guard Core"
            setTextColor(Color.WHITE)
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dp(4f), 0, dp(2f))
        })
        header.addView(TextView(this).apply {
            text = accountLabel
            setTextColor(Color.parseColor("#CBD5E1"))
            textSize = 12f
        })

        // 1. In-App Continuous Listener Card (no external Google mic dialog)
        val engineCard = card("AUTONOMOUS LISTENER", "#172554", "#93C5FD", "In-App Voice & Wake-Word Engine")

        tvEngineState = TextView(this).apply {
            text = "Engine: STANDBY (Wake word: \"$wakeWord\")"
            setTextColor(Color.parseColor("#93C5FD"))
            textSize = 13f
        }

        tvLastHeard = TextView(this).apply {
            text = "Last detected speech: None"
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 12f
            setPadding(0, dp(4f), 0, dp(8f))
        }

        val wakeRow = row()
        etWakeWord = EditText(this).apply {
            hint = "Wake word (e.g. klyvora)"
            setText(wakeWord)
            setTextColor(Color.WHITE)
            setHintTextColor(Color.parseColor("#64748B"))
            textSize = 12f
            background = box("#090E1A", "#263244", 10f)
            setPadding(dp(10f), dp(9f), dp(10f), dp(9f))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                rightMargin = dp(6f)
            }
        }
        wakeRow.addView(etWakeWord)
        wakeRow.addView(btn("Save Wake Word", "#3730A3", 0.55f) {
            val nextWake = etWakeWord.text.toString().trim().lowercase(Locale.ROOT)
            if (nextWake.isNotEmpty()) {
                wakeWord = nextWake
                prefs.edit().putString("wake_word", wakeWord).apply()
                setEngineState("Wake word updated to \"$wakeWord\"", "#34D399")
            }
        })

        val engineBtnRow = row()
        btnEngineToggle = btn("Start Listener", "#059669", 1f) {
            if (isListeningEngineActive) {
                stopContinuousEngine()
            } else {
                requestAudioAndStart()
            }
        }
        engineBtnRow.addView(btnEngineToggle)

        val manualRow = row()
        etManualCmd = EditText(this).apply {
            hint = "Test command (e.g. alarm, battery, youtube, arm)"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.parseColor("#64748B"))
            textSize = 12f
            background = box("#090E1A", "#263244", 10f)
            setPadding(dp(10f), dp(9f), dp(10f), dp(9f))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                rightMargin = dp(6f)
            }
        }
        manualRow.addView(etManualCmd)
        manualRow.addView(btn("Execute", "#2563EB", 0.4f) {
            val cmd = etManualCmd.text.toString().trim()
            if (cmd.isNotEmpty()) executeAction(cmd, "Manual")
        })

        engineCard.addView(tvEngineState)
        engineCard.addView(tvLastHeard)
        engineCard.addView(wakeRow)
        engineCard.addView(engineBtnRow)
        engineCard.addView(manualRow)

        // 2. Custom Command Rules Builder
        val rulesCard = card("AUTOMATION RULES", "#1E1B4B", "#C4B5FD", "Saved Voice / Command Triggers")

        val ruleInputs = row()
        etCommandWord = EditText(this).apply {
            hint = "Spoken word (e.g. siren)"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.parseColor("#64748B"))
            textSize = 12f
            background = box("#090E1A", "#263244", 10f)
            setPadding(dp(10f), dp(9f), dp(10f), dp(9f))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                rightMargin = dp(4f)
            }
        }
        etCommandAction = EditText(this).apply {
            hint = "Action (alarm, arm, disarm, battery, youtube)"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.parseColor("#64748B"))
            textSize = 12f
            background = box("#090E1A", "#263244", 10f)
            setPadding(dp(10f), dp(9f), dp(10f), dp(9f))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = dp(4f)
            }
        }
        ruleInputs.addView(etCommandWord)
        ruleInputs.addView(etCommandAction)

        val saveRuleRow = row()
        saveRuleRow.addView(btn("Save Rule", "#4F46E5", 1f) {
            val w = etCommandWord.text.toString().trim().lowercase(Locale.ROOT)
            val a = etCommandAction.text.toString().trim().lowercase(Locale.ROOT)
            if (w.isNotEmpty() && a.isNotEmpty()) {
                commandRules.removeAll { it.first == w }
                commandRules.add(w to a)
                saveRules(prefs)
                renderRules()
                etCommandWord.text.clear()
                etCommandAction.text.clear()
                setEngineState("Rule saved: \"$w\" -> $a", "#34D399")
            }
        })

        tvRulesSummary = TextView(this).apply {
            textSize = 12f
            setTextColor(Color.parseColor("#CBD5E1"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(8f) }
        }
        renderRules()

        rulesCard.addView(ruleInputs)
        rulesCard.addView(saveRuleRow)
        rulesCard.addView(tvRulesSummary)

        // 3. Anti-Theft Guard & Quick Controls
        val guardCard = card("ANTI-THEFT & SYSTEM", "#064E3B", "#6EE7B7", "Motion Guard & Device Controls")

        tvTheftStatus = TextView(this).apply {
            text = if (isGuardArmed) "Guard: ARMED (Motion sensor active)" else "Guard: STANDBY"
            setTextColor(Color.parseColor(if (isGuardArmed) "#34D399" else "#94A3B8"))
            textSize = 13f
        }

        val guardRow = row()
        btnArmGuard = btn(
            if (isGuardArmed) "Disarm Guard" else "Arm Guard",
            if (isGuardArmed) "#B91C1C" else "#059669",
            1f
        ) {
            toggleGuard(prefs)
        }
        guardRow.addView(btnArmGuard)

        val quickRow1 = row()
        quickRow1.addView(btn("Siren Alert", "#1E293B") { executeAction("alarm", "Quick") })
        quickRow1.addView(btn("Battery Info", "#1E293B") { executeAction("battery", "Quick") })
        quickRow1.addView(btn("System Time", "#1E293B") { executeAction("time", "Quick") })

        val quickRow2 = row()
        quickRow2.addView(btn("Open YouTube", "#1E293B") { executeAction("youtube", "Quick") })
        quickRow2.addView(btn("App Settings", "#1E293B") {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        })

        guardCard.addView(tvTheftStatus)
        guardCard.addView(guardRow)
        guardCard.addView(quickRow1)
        guardCard.addView(quickRow2)

        val btnLogout = Button(this).apply {
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
                stopContinuousEngine()
                prefs.edit().putBoolean("is_logged_in", false).apply()
                startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                finish()
            }
        }

        page.addView(header)
        page.addView(engineCard)
        page.addView(rulesCard)
        page.addView(guardCard)
        page.addView(btnLogout)

        scroll.addView(page)
        setContentView(scroll)
    }

    private fun requestAudioAndStart() {
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            startContinuousEngine()
        } else {
            audioPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startContinuousEngine() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            setEngineState("In-app SpeechRecognizer unavailable on this device", "#F87171")
            return
        }
        isListeningEngineActive = true
        updateEngineButton()
        setEngineState("Engine ACTIVE: Listening in-app (Wake: \"$wakeWord\")", "#34D399")
        triggerInAppRecognizer()
    }

    private fun stopContinuousEngine() {
        isListeningEngineActive = false
        mainHandler.removeCallbacksAndMessages(null)
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (_: Exception) {
        }
        speechRecognizer = null
        updateEngineButton()
        setEngineState("Engine STANDBY", "#94A3B8")
    }

    private fun triggerInAppRecognizer() {
        if (!isListeningEngineActive) return
        try {
            speechRecognizer?.destroy()
            val recognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer = recognizer

            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    if (isListeningEngineActive) {
                        setEngineState("Listening... (Wake: \"$wakeWord\")", "#60A5FA")
                    }
                }

                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() = Unit

                override fun onError(error: Int) {
                    if (isListeningEngineActive) {
                        mainHandler.postDelayed({ triggerInAppRecognizer() }, 900L)
                    }
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val heard = matches?.firstOrNull()?.trim().orEmpty()
                    if (heard.isNotEmpty()) {
                        tvLastHeard.text = "Last detected speech: \"$heard\""
                        val lower = heard.lowercase(Locale.ROOT)
                        if (lower.contains(wakeWord) || commandRules.any { lower.contains(it.first) }) {
                            executeAction(lower, "Voice")
                        }
                    }
                    if (isListeningEngineActive) {
                        mainHandler.postDelayed({ triggerInAppRecognizer() }, 500L)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            recognizer.startListening(intent)
        } catch (_: Exception) {
            if (isListeningEngineActive) {
                mainHandler.postDelayed({ triggerInAppRecognizer() }, 1200L)
            }
        }
    }

    private fun updateEngineButton() {
        btnEngineToggle.text = if (isListeningEngineActive) "Stop Listener" else "Start Listener"
        btnEngineToggle.setBackgroundColor(Color.parseColor(if (isListeningEngineActive) "#B91C1C" else "#059669"))
    }

    private fun executeAction(rawInput: String, source: String) {
        val lower = rawInput.lowercase(Locale.ROOT)
        val matched = commandRules.firstOrNull { lower.contains(it.first) }
        val target = matched?.second ?: lower
        val prefs = getSharedPreferences("KlyvoraVaultSession", Context.MODE_PRIVATE)

        when {
            target.contains("alarm") || target.contains("siren") || target.contains("alert") -> {
                playSiren()
                setEngineState("[$source] Siren alert triggered", "#F87171")
                speak("Alert triggered")
            }
            target.contains("disarm") || target.contains("unlock") -> {
                if (isGuardArmed) toggleGuard(prefs)
                else setEngineState("[$source] Guard already in STANDBY", "#FBBF24")
            }
            target.contains("arm") || target.contains("lock") || target.contains("guard") -> {
                if (!isGuardArmed) toggleGuard(prefs)
                else setEngineState("[$source] Guard already ARMED", "#34D399")
            }
            target.contains("battery") -> {
                val bm = getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
                val pct = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
                val msg = if (pct >= 0) "Battery at $pct%" else "Battery unavailable"
                setEngineState("[$source] $msg", "#60A5FA")
                speak(msg)
            }
            target.contains("time") -> {
                val now = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                setEngineState("[$source] Time: $now", "#60A5FA")
                speak("Time is $now")
            }
            target.contains("youtube") || target.contains("yt") -> {
                try {
                    val intent = packageManager.getLaunchIntentForPackage("com.google.android.youtube")
                        ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://m.youtube.com"))
                    startActivity(intent)
                    setEngineState("[$source] Opening YouTube", "#34D399")
                } catch (_: Exception) {
                    setEngineState("[$source] Unable to open YouTube", "#F87171")
                }
            }
            else -> {
                setEngineState("[$source] Heard \"$rawInput\" (no matching action)", "#94A3B8")
            }
        }
    }

    private fun toggleGuard(prefs: android.content.SharedPreferences) {
        isGuardArmed = !isGuardArmed
        prefs.edit().putBoolean("anti_theft_armed", isGuardArmed).apply()
        tvTheftStatus.text = if (isGuardArmed) "Guard: ARMED (Motion sensor active)" else "Guard: STANDBY"
        tvTheftStatus.setTextColor(Color.parseColor(if (isGuardArmed) "#34D399" else "#94A3B8"))
        btnArmGuard.text = if (isGuardArmed) "Disarm Guard" else "Arm Guard"
        btnArmGuard.setBackgroundColor(Color.parseColor(if (isGuardArmed) "#B91C1C" else "#059669"))
        setEngineState(if (isGuardArmed) "Anti-Theft Guard ARMED" else "Anti-Theft Guard STANDBY", "#34D399")
    }

    private fun loadRules(prefs: android.content.SharedPreferences) {
        commandRules.clear()
        val raw = prefs.getString("automation_rules", null)
        if (raw.isNullOrBlank()) {
            commandRules.add("siren" to "alarm")
            commandRules.add("arm guard" to "arm")
            commandRules.add("disarm guard" to "disarm")
            return
        }
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val k = obj.optString("k")
                val v = obj.optString("v")
                if (k.isNotBlank() && v.isNotBlank()) commandRules.add(k to v)
            }
        } catch (_: Exception) {
            commandRules.add("siren" to "alarm")
        }
    }

    private fun saveRules(prefs: android.content.SharedPreferences) {
        val arr = JSONArray()
        for ((k, v) in commandRules) {
            arr.put(JSONObject().put("k", k).put("v", v))
        }
        prefs.edit().putString("automation_rules", arr.toString()).apply()
    }

    private fun renderRules() {
        tvRulesSummary.text = if (commandRules.isEmpty()) {
            "Rules: None"
        } else {
            "Rules:\n" + commandRules.joinToString("\n") { "• \"${it.first}\" → ${it.second}" }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.getDefault()
            isTtsReady = true
        }
    }

    private fun speak(text: String) {
        if (isTtsReady && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "KV_TTS")
        }
    }

    private fun playSiren() {
        try {
            val tg = ToneGenerator(AudioManager.STREAM_ALARM, 100)
            tg.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 600)
        } catch (_: Exception) {
        }
    }

    private fun setEngineState(msg: String, hex: String) {
        tvEngineState.text = msg
        tvEngineState.setTextColor(Color.parseColor(hex))
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
        stopContinuousEngine()
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
                setEngineState("THEFT ALERT: Motion detected while ARMED!", "#F87171")
                speak("Motion alert")
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
