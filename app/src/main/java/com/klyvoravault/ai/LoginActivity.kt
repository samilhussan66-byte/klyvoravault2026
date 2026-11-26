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

        fun boxDrawable(bgHex: String, strokeHex: String? = null, radiusDp: Float = 16f): GradientDrawable {
            val d = resources.displayMetrics.density
            return GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = radiusDp * d
                setColor(Color.parseColor(bgHex))
                if (strokeHex != null) {
                    setStroke((1.5f * d).toInt(), Color.parseColor(strokeHex))
                }
            }
        }

        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#0A0F1A"))
            isFillViewport = true
        }

        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(48, 64, 48, 64)
        }

        val headerBanner = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = boxDrawable("#162447", "#2563EB", 22f)
            setPadding(40, 36, 40, 36)
        }

        val tvBrand = TextView(this).apply {
            text = "KLYVORA VAULT"
            setTextColor(Color.parseColor("#93C5FD"))
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.14f
        }

        val tvTitle = TextView(this).apply {
            text = "Welcome back"
            setTextColor(Color.parseColor("#FFFFFF"))
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 10, 0, 6)
        }

        val tvSubtitle = TextView(this).apply {
            text = "Sign in to continue to your account"
            setTextColor(Color.parseColor("#CBD5E1"))
            textSize = 14f
        }

        headerBanner.addView(tvBrand)
        headerBanner.addView(tvTitle)
        headerBanner.addView(tvSubtitle)

        val formCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = boxDrawable("#111827", "#1F2937", 22f)
            setPadding(40, 40, 40, 40)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = 28
            layoutParams = lp
        }

        val lblEmail = TextView(this).apply {
            text = "Email"
            setTextColor(Color.parseColor("#E2E8F0"))
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(2, 0, 0, 10)
        }

        val etEmail = EditText(this).apply {
            hint = "Enter email"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            setTextColor(Color.parseColor("#FFFFFF"))
            setHintTextColor(Color.parseColor("#6B7280"))
            textSize = 15f
            background = boxDrawable("#0A0F1A", "#2D3748", 14f)
            setPadding(32, 28, 32, 28)
        }

        val lblPassword = TextView(this).apply {
            text = "Password"
            setTextColor(Color.parseColor("#E2E8F0"))
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(2, 24, 0, 10)
        }

        val passContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = boxDrawable("#0A0F1A", "#2D3748", 14f)
            setPadding(32, 8, 20, 8)
        }

        val etPassword = EditText(this).apply {
            hint = "Password"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setTextColor(Color.parseColor("#FFFFFF"))
            setHintTextColor(Color.parseColor("#6B7280"))
            textSize = 15f
            background = null
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setPadding(0, 20, 12, 20)
        }

        val btnEye = TextView(this).apply {
            text = "Show"
            setTextColor(Color.parseColor("#60A5FA"))
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(18, 14, 14, 14)
        }

        btnEye.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                btnEye.text = "Hide"
            } else {
                etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                btnEye.text = "Show"
            }
            val len = etPassword.text?.length ?: 0
            if (len >= 0) {
                etPassword.setSelection(len)
            }
        }

        passContainer.addView(etPassword)
        passContainer.addView(btnEye)

        val tvError = TextView(this).apply {
            text = ""
            setTextColor(Color.parseColor("#F87171"))
            textSize = 13f
            visibility = View.GONE
            setPadding(4, 18, 4, 2)
        }

        fun setError(msg: String?) {
            if (msg.isNullOrBlank()) {
                tvError.visibility = View.GONE
                tvError.text = ""
            } else {
                tvError.text = msg
                tvError.visibility = View.VISIBLE
            }
        }

        val btnSubmit = Button(this).apply {
            text = "Sign In"
            setTextColor(Color.parseColor("#FFFFFF"))
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            isAllCaps = false
            background = boxDrawable("#2563EB", null, 14f)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = 28
            layoutParams = lp
            setPadding(24, 30, 24, 30)
        }

        val bottomRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = 28
            layoutParams = lp
        }

        val tvBottomText = TextView(this).apply {
            text = "New to KlyvoraVault? "
            setTextColor(Color.parseColor("#9CA3AF"))
            textSize = 14f
        }

        val tvBottomAction = TextView(this).apply {
            text = "Create account"
            setTextColor(Color.parseColor("#60A5FA"))
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(8, 6, 8, 6)
        }

        bottomRow.addView(tvBottomText)
        bottomRow.addView(tvBottomAction)

        fun updateMode() {
            setError(null)
            if (isSignUpMode) {
                tvTitle.text = "Create account"
                tvSubtitle.text = "Register with your email and password"
                btnSubmit.text = "Create Account"
                tvBottomText.text = "Already registered? "
                tvBottomAction.text = "Sign in"
            } else {
                tvTitle.text = "Welcome back"
                tvSubtitle.text = "Sign in to continue to your account"
                btnSubmit.text = "Sign In"
                tvBottomText.text = "New to KlyvoraVault? "
                tvBottomAction.text = "Create account"
            }
        }

        tvBottomAction.setOnClickListener {
            isSignUpMode = !isSignUpMode
            updateMode()
        }

        btnSubmit.setOnClickListener {
            val email = etEmail.text.toString().trim().lowercase()
            val password = etPassword.text.toString()

            if (email.isEmpty() || !email.contains("@")) {
                setError("Please enter a valid email")
                return@setOnClickListener
            }
            if (password.length < 6) {
                setError("Password must be at least 6 characters")
                return@setOnClickListener
            }

            setError(null)
            if (isSignUpMode) {
                prefs.edit()
                    .putString("user_email", email)
                    .putString("user_password", password)
                    .putBoolean("is_logged_in", true)
                    .apply()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                val savedEmail = prefs.getString("user_email", null)
                val savedPass = prefs.getString("user_password", null)

                if (savedEmail == null || savedPass == null) {
                    setError("Account not found. Tap Create account")
                    return@setOnClickListener
                }

                if (email == savedEmail && password == savedPass) {
                    prefs.edit().putBoolean("is_logged_in", true).apply()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    setError("Incorrect email or password")
                }
            }
        }

        formCard.addView(lblEmail)
        formCard.addView(etEmail)
        formCard.addView(lblPassword)
        formCard.addView(passContainer)
        formCard.addView(tvError)
        formCard.addView(btnSubmit)

        page.addView(headerBanner)
        page.addView(formCard)
        page.addView(bottomRow)

        scroll.addView(page)
        setContentView(scroll)
    }
}
