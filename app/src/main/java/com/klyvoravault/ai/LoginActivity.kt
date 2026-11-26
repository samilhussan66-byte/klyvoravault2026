package com.klyvoravault.ai

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    private var isSignUpMode = false
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("KlyvoraVaultSession", Context.MODE_PRIVATE)
        if (prefs.getBoolean("is_logged_in", false)) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        fun roundedBox(bgHex: String, strokeHex: String? = null, radiusDp: Float = 14f): GradientDrawable {
            val density = resources.displayMetrics.density
            return GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = radiusDp * density
                setColor(Color.parseColor(bgHex))
                if (strokeHex != null) {
                    setStroke((1 * density).toInt(), Color.parseColor(strokeHex))
                }
            }
        }

        val root = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#0B0F17"))
            isFillViewport = true
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(56, 72, 56, 72)
        }

        val badge = TextView(this).apply {
            text = "KLYVORA VAULT"
            setTextColor(Color.parseColor("#60A5FA"))
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.14f
        }

        val tvHeading = TextView(this).apply {
            text = "Welcome back"
            setTextColor(Color.parseColor("#F8FAFC"))
            textSize = 30f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 12, 0, 6)
        }

        val tvSubheading = TextView(this).apply {
            text = "Sign in to access your workspace"
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 15f
            setPadding(0, 0, 0, 32)
        }

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBox("#131A26", "#1E293B", 20f)
            setPadding(44, 44, 44, 44)
        }

        val lblEmail = TextView(this).apply {
            text = "Email"
            setTextColor(Color.parseColor("#CBD5E1"))
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, 10)
        }

        val etEmail = EditText(this).apply {
            hint = "name@example.com"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            setTextColor(Color.parseColor("#F8FAFC"))
            setHintTextColor(Color.parseColor("#64748B"))
            textSize = 15f
            background = roundedBox("#0B0F17", "#263244", 12f)
            setPadding(36, 30, 36, 30)
        }

        val lblPassword = TextView(this).apply {
            text = "Password"
            setTextColor(Color.parseColor("#CBD5E1"))
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 28, 0, 10)
        }

        val passwordRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = roundedBox("#0B0F17", "#263244", 12f)
            setPadding(36, 12, 20, 12)
        }

        val etPassword = EditText(this).apply {
            hint = "Enter your password"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setTextColor(Color.parseColor("#F8FAFC"))
            setHintTextColor(Color.parseColor("#64748B"))
            textSize = 15f
            background = null
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setPadding(0, 18, 16, 18)
        }

        val btnToggleEye = TextView(this).apply {
            text = "Show"
            setTextColor(Color.parseColor("#93C5FD"))
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(20, 14, 20, 14)
        }

        btnToggleEye.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                btnToggleEye.text = "Hide"
            } else {
                etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                btnToggleEye.text = "Show"
            }
            etPassword.setSelection(etPassword.text.length)
        }

        passwordRow.addView(etPassword)
        passwordRow.addView(btnToggleEye)

        val tvStatus = TextView(this).apply {
            text = ""
            setTextColor(Color.parseColor("#F87171"))
            textSize = 13f
            visibility = View.GONE
            setPadding(4, 20, 4, 4)
        }

        fun showStatus(message: String, isError: Boolean = true) {
            tvStatus.text = message
            tvStatus.setTextColor(Color.parseColor(if (isError) "#F87171" else "#34D399"))
            tvStatus.visibility = View.VISIBLE
        }

        val btnSubmit = Button(this).apply {
            text = "Sign In"
            setTextColor(Color.WHITE)
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            isAllCaps = false
            background = roundedBox("#2563EB", null, 12f)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.topMargin = 32
            layoutParams = params
            setPadding(24, 32, 24, 32)
        }

        val switchRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 32, 0, 0)
        }

        val tvSwitchPrompt = TextView(this).apply {
            text = "Don't have an account? "
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 14f
        }

        val tvSwitchAction = TextView(this).apply {
            text = "Create account"
            setTextColor(Color.parseColor("#60A5FA"))
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
        }

        switchRow.addView(tvSwitchPrompt)
        switchRow.addView(tvSwitchAction)

        fun applyMode() {
            tvStatus.visibility = View.GONE
            if (isSignUpMode
