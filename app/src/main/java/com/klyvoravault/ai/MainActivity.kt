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
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Locale
import kotlin.math.sqrt

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var tvStatusBanner: TextView
    private lateinit var tvVoiceResult: TextView
    private lateinit var tvTheftStatus: TextView
    private lateinit var btnArmTheft: Button

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var isGuardArmed = false
    private var lastShakeTime = 0L

    private val speechLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = matches?.firstOrNull()?.trim().orEmpty()
            if (spokenText.isNotEmpty()) {
                handleVoiceCommand(spokenText)
            } else {
                showStatus("No voice input captured. Try speaking again.", "#FBBF24")
            }
        } else {
            showStatus("Voice listening cancelled.", "#94A3B8")
        }
    }

    private val micPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startVoiceListening()
        } else {
            showStatus("Microphone permission is required for voice commands.", "#F87171")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("KlyvoraVaultSession", Context.MODE_PRIVATE)
        val userEmail = prefs.getString("user_email", null)
            ?: prefs.getString("user_phone", null)
            ?: "Active Session"

        isGuardArmed = prefs.getBoolean("anti_theft_armed", false)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val d = resources.displayMetrics.density
        fun dp(value: Float): Int = (value * d).toInt()

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

        val bgGradient = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(
                Color.parseColor("#0A1326"),
                Color.parseColor("#0D162B"),
                Color.parseColor("#070B14")
            )
        )

        val scroll = ScrollView(this).apply {
            background = bgGradient
            isFillViewport = true
        }

        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20f), dp(24f), dp(20f), dp(28f))
        }

        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = box("#111C33", "#2563EB", 20f)
            setPadding(dp(18f), dp(18f), dp(18f), dp(18f))
        }

        val tvBrand = TextView(this).apply {
            text = "KLYVORA VAULT"
            setTextColor(Color.parseColor("#93C5FD"))
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.14f
        }

        val tvTitle = TextView(this).apply {
            text = "Command Dashboard"
            setTextColor(Color.parseColor("#FFFFFF"))
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dp(6f), 0, dp(4f))
        }

        val tvUser = TextView(this).apply {
            text = userEmail
            setTextColor(Color.parseColor("#CBD5E1"))
            textSize = 13f
        }

        topBar.addView(tvBrand)
        topBar.addView(tvTitle)
        topBar.addView(tvUser)

        tvStatusBanner = TextView(this).apply {
            text = ""
            textSize = 13f
            visibility = View.GONE
            background = box("#0F172A", "#1E293B", 14f)
            setPadding(dp(14f), dp(12f), dp(14f), dp(12f))
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = dp(14f)
            layoutParams = lp
        }

        // Voice Card
        val voiceCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = box("#0F172A", "#1E293B", 20f)
            setPadding(dp(18f), dp(18f), dp(18f), dp(18f))
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = dp(14f)
            layoutParams = lp
        }

        val tvVoiceBadge = TextView(this).apply {
            text = "VOICE MODULE"
            setTextColor(Color.parseColor("#93C5FD"))
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            background = box("#172554", null, 8f)
            setPadding(dp(10f), dp(4f), dp(10f), dp(4f))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val tvVoiceTitle = TextView(this).apply {
            text = "Voice Command Engine"
            setTextColor(Color.parseColor("#F8FAFC"))
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dp(10f), 0, dp(4f))
        }

        val tvVoiceDesc = TextView(this).apply {
            text = "Tap below to speak a command (e.g., \"arm protection\", \"disarm\", \"status\")."
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 13f
        }

        tvVoiceResult = TextView(this).apply {
            text = "Last command: None"
            setTextColor(Color.parseColor("#CBD5E1"))
            textSize = 13f
            visibility = View.VISIBLE
            setPadding(0, dp(10f), 0, 0)
        }

        val btnListen = Button(this).apply {
            text = "Listen Voice Command"
            setTextColor(Color.WHITE)
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            isAllCaps = false
            background = box("#1D4ED8", null, 12f)
            val btnLp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            btnLp.topMargin = dp(14f)
            layoutParams = btnLp
            setPadding(dp(14f), dp(11f), dp(14f), dp(11f))
            setOnClickListener {
                checkMicAndListen()
            }
        }

        voiceCard.addView(tvVoiceBadge)
        voiceCard.addView(tvVoiceTitle)
        voiceCard.addView(tvVoiceDesc)
        voiceCard.addView(tvVoiceResult)
        voiceCard.addView(btnListen)

        // Anti-Theft Card
        val theftCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = box("#0F172A", "#1E293B", 20f)
            setPadding(dp(18f), dp(18f), dp(18f), dp(18f))
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = dp(14f)
            layoutParams = lp
        }

        val tvTheftBadge = TextView(this).apply {
            text = "SECURITY"
            setTextColor(Color.parseColor("#6EE7B7"))
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            background = box("#064E3B", null, 8f)
            setPadding(dp(10f), dp(4f), dp(10f), dp(4f))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val tvTheftTitle = TextView(this).apply {
            text = "Anti-Theft Motion Guard"
            setTextColor(Color.parseColor("#F8FAFC"))
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dp(10f), 0, dp(4f))
        }

        tvTheftStatus = TextView(this).apply {
            text = if (isGuardArmed) {
                "Protection status: ARMED (Motion alert active)"
            } else {
                "Protection status: STANDBY"
            }
            setTextColor(Color.parseColor(if (isGuardArmed) "#34D399" else "#94A3B8"))
            textSize = 13f
        }

        btnArmTheft = Button(this).apply {
            text = if (isGuardArmed) "Disarm Protection" else "Arm Protection"
            setTextColor(Color.WHITE)
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            isAllCaps = false
            background = box(if (isGuardArmed) "#B91C1C" else "#1D4ED8", null, 12f)
            val btnLp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            btnLp.topMargin = dp(14f)
            layoutParams = btnLp
            setPadding(dp(14f), dp(11f), dp(14f), dp(11f))
            setOnClickListener {
                toggleGuardState(prefs)
            }
        }

        theftCard.addView(tvTheftBadge)
        theftCard.addView(tvTheftTitle)
        theftCard.addView(tvTheftStatus)
        theftCard.addView(btnArmTheft)

        val btnLogout = Button(this).apply {
            text = "Sign Out"
            setTextColor(Color.parseColor("#FCA5A5"))
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            isAllCaps = false
            background = box("#1A1219", "#7F1D1D", 12f)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = dp(20f)
            layoutParams = lp
            setPadding(dp(14f), dp(12f), dp(14f), dp(12f))
        }

        btnLogout.setOnClickListener {
            prefs.edit().putBoolean("is_logged_in", false).apply()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        page.addView(topBar)
        page.addView(tvStatusBanner)
        page.addView(voiceCard)
        page.addView(theftCard)
        page.addView(btnLogout)

        scroll.addView(page)
        setContentView(scroll)
    }

    private fun checkMicAndListen() {
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            startVoiceListening()
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startVoiceListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak a KlyvoraVault command")
        }
        try {
            speechLauncher.launch(intent)
        } catch (_: Exception) {
            showStatus("Speech recognition is not available on this device.", "#F87171")
        }
    }

    private fun handleVoiceCommand(spokenText: String) {
        tvVoiceResult.text = "Last command: \"$spokenText\""
        val lower = spokenText.lowercase(Locale.ROOT)
        val prefs = getSharedPreferences("KlyvoraVaultSession", Context.MODE_PRIVATE)

        when {
            lower.contains("arm") || lower.contains("lock") || lower.contains("guard") -> {
                if (!isGuardArmed) {
                    toggleGuardState(prefs)
                } else {
                    showStatus("Anti-Theft protection is already ARMED.", "#34D399")
                }
            }
            lower.contains("disarm") || lower.contains("unlock") || lower.contains("standby") -> {
                if (isGuardArmed) {
                    toggleGuardState(prefs)
                } else {
                    showStatus("Anti-Theft protection is already in STANDBY.", "#FBBF24")
                }
            }
            else -> {
                showStatus("Heard: \"$spokenText\"", "#60A5FA")
            }
        }
    }

    private fun toggleGuardState(prefs: android.content.SharedPreferences) {
        isGuardArmed = !isGuardArmed
        prefs.edit().putBoolean("anti_theft_armed", isGuardArmed).apply()

        tvTheftStatus.text = if (isGuardArmed) {
            "Protection status: ARMED (Motion alert active)"
        } else {
            "Protection status: STANDBY"
        }
        tvTheftStatus.setTextColor(Color.parseColor(if (isGuardArmed) "#34D399" else "#94A3B8"))
        btnArmTheft.text = if (isGuardArmed) "Disarm Protection" else "Arm Protection"

        showStatus(
            if (isGuardArmed) {
                "Anti-Theft protection is now ARMED. Sudden movement will trigger an alert."
            } else {
                "Anti-Theft protection set to STANDBY."
            },
            if (isGuardArmed) "#34D399" else "#FBBF24"
        )
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
                playAlertTone()
                showStatus("ALERT: Sudden movement detected while protection is ARMED!", "#F87171")
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun playAlertTone() {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_ALARM, 90)
            toneGen.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 400)
        } catch (_: Exception) {
            // Ignore audio device errors on restricted hardware
        }
    }

    private fun showStatus(msg: String, accentHex: String = "#60A5FA") {
        tvStatusBanner.text = msg
        tvStatusBanner.setTextColor(Color.parseColor(accentHex))
        tvStatusBanner.visibility = View.VISIBLE
    }
}
