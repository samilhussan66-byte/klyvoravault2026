package com.klyvoravault.ai

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    private var isSignUpMode = false
    private var isPasswordVisible = false
    private var isPhoneMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("KlyvoraVaultSession", Context.MODE_PRIVATE)
        if (prefs.getBoolean("is_logged_in", false)) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        val d = resources.displayMetrics.density
        fun dp(value: Float): Int = (value * d).toInt()

        fun roundedBox(bgHex: String, strokeHex: String? = null, radiusDp: Float = 16f): GradientDrawable {
            return GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(radiusDp).toFloat()
                setColor(Color.parseColor(bgHex))
                if (strokeHex != null) {
                    setStroke(dp(1.2f).coerceAtLeast(1), Color.parseColor(strokeHex))
                }
            }
        }

        fun fullScreenGradient(): GradientDrawable {
            return GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(
                    Color.parseColor("#0A1326"),
                    Color.parseColor("#0D162B"),
                    Color.parseColor("#070B14")
                )
            )
        }

        fun eyeIconDrawable(crossed: Boolean): Drawable {
            return object : Drawable() {
                private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#93C5FD")
                    style = Paint.Style.STROKE
                    strokeWidth = dp(1.8f).toFloat()
                    strokeCap = Paint.Cap.ROUND
                }

                override fun draw(canvas: Canvas) {
                    val b = bounds
                    val w = b.width().toFloat()
                    val h = b.height().toFloat()
                    val cx = w / 2f
                    val cy = h / 2f
                    val rx = w * 0.42f
                    val ry = h * 0.24f

                    val eyePath = Path().apply {
                        moveTo(cx - rx, cy)
                        quadTo(cx, cy - ry * 1.7f, cx + rx, cy)
                        quadTo(cx, cy + ry * 1.7f, cx - rx, cy)
                        close()
                    }
                    canvas.drawPath(eyePath, paint)
                    canvas.drawCircle(cx, cy, (minOf(w, h) * 0.14f), paint)

                    if (crossed) {
                        canvas.drawLine(cx - rx * 0.9f, cy + ry * 1.2f, cx + rx * 0.9f, cy - ry * 1.2f, paint)
                    }
                }

                override fun setAlpha(alpha: Int) {
                    paint.alpha = alpha
                }

                override fun setColorFilter(colorFilter: ColorFilter?) {
                    paint.colorFilter = colorFilter
                }

                @Deprecated("Deprecated in Java")
                override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
            }
        }

        fun googleBadgeDrawable(): Drawable {
            return object : Drawable() {
                private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                override fun draw(canvas: Canvas) {
                    val rect = RectF(
                        dp(2f).toFloat(),
                        dp(2f).toFloat(),
                        (bounds.width() - dp(2f)).toFloat(),
                        (bounds.height() - dp(2f)).toFloat()
                    )
                    paint.style = Paint.Style.FILL
                    paint.color = Color.parseColor("#1E293B")
                    canvas.drawOval(rect, paint)

                    paint.color = Color.parseColor("#60A5FA")
                    paint.textSize = dp(12f).toFloat()
                    paint.typeface = Typeface.DEFAULT_BOLD
                    paint.textAlign = Paint.Align.CENTER
                    val fm = paint.fontMetrics
                    val y = rect.centerY() - (fm.ascent + fm.descent) / 2f
                    canvas.drawText("G", rect.centerX(), y, paint)
                }

                override fun setAlpha(alpha: Int) {
                    paint.alpha = alpha
                }

                override fun setColorFilter(colorFilter: ColorFilter?) {
                    paint.colorFilter = colorFilter
                }

                @Deprecated("Deprecated in Java")
                override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
            }
        }

        val scroll = ScrollView(this).apply {
            background = fullScreenGradient()
            isFillViewport = true
        }

        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(20f), dp(28f), dp(20f), dp(28f))
        }

        // Primary Brand Identity at the very top
        val brandBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = roundedBox("#111C33", "#2563EB", 20f)
            setPadding(dp(18f), dp(18f), dp(18f), dp(18f))
        }

        val tvBrandName = TextView(this).apply {
            text = "KLYVORA VAULT"
            setTextColor(Color.parseColor("#FFFFFF"))
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.12f
            gravity = Gravity.CENTER
        }

        val tvBrandTag = TextView(this).apply {
            text = "Smart Security & Voice Assistant"
            setTextColor(Color.parseColor("#93C5FD"))
            textSize = 12f
            gravity = Gravity.CENTER
            setPadding(0, dp(4f), 0, 0)
        }

        brandBox.addView(tvBrandName)
        brandBox.addView(tvBrandTag)

        // Main Form Card
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBox("#0F172A", "#1E293B", 22f)
            setPadding(dp(20f), dp(22f), dp(20f), dp(22f))
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = dp(18f)
            layoutParams = lp
        }

        val tvModeTitle = TextView(this).apply {
            text = "Welcome back"
            setTextColor(Color.parseColor("#F8FAFC"))
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
        }

        val tvModeDesc = TextView(this).apply {
            text = "Sign in to continue"
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 13f
            setPadding(0, dp(4f), 0, dp(18f))
        }

        val emailGroup = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val lblEmail = TextView(this).apply {
            text = "Email"
            setTextColor(Color.parseColor("#E2E8F0"))
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, dp(6f))
        }

        val etEmail = EditText(this).apply {
            hint = "Enter email"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            setTextColor(Color.parseColor("#FFFFFF"))
            setHintTextColor(Color.parseColor("#64748B"))
            textSize = 15f
            background = roundedBox("#090E1A", "#263244", 12f)
            setPadding(dp(14f), dp(12f), dp(14f), dp(12f))
        }

        val lblPassword = TextView(this).apply {
            text = "Password"
            setTextColor(Color.parseColor("#E2E8F0"))
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dp(14f), 0, dp(6f))
        }

        val passRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = roundedBox("#090E1A", "#263244", 12f)
            setPadding(dp(14f), dp(4f), dp(10f), dp(4f))
        }

        val etPassword = EditText(this).apply {
            hint = "Password"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setTextColor(Color.parseColor("#FFFFFF"))
            setHintTextColor(Color.parseColor("#64748B"))
            textSize = 15f
            background = null
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setPadding(0, dp(10f), dp(10f), dp(10f))
        }

        val ivEye = ImageView(this).apply {
            setImageDrawable(eyeIconDrawable(false))
            contentDescription = "Toggle password visibility"
            val lp = LinearLayout.LayoutParams(dp(36f), dp(36f))
            layoutParams = lp
            setPadding(dp(6f), dp(6f), dp(6f), dp(6f))
        }

        ivEye.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                ivEye.setImageDrawable(eyeIconDrawable(true))
            } else {
                etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                ivEye.setImageDrawable(eyeIconDrawable(false))
            }
            val len = etPassword.text?.length ?: 0
            if (len >= 0) {
                etPassword.setSelection(len)
            }
        }

        passRow.addView(etPassword)
        passRow.addView(ivEye)

        emailGroup.addView(lblEmail)
        emailGroup.addView(etEmail)
        emailGroup.addView(lblPassword)
        emailGroup.addView(passRow)

        val phoneGroup = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }

        val lblPhone = TextView(this).apply {
            text = "Phone number"
            setTextColor(Color.parseColor("#E2E8F0"))
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, dp(6f))
        }

        val etPhone = EditText(this).apply {
            hint = "+91 98765 43210"
            inputType = InputType.TYPE_CLASS_PHONE
            setTextColor(Color.parseColor("#FFFFFF"))
            setHintTextColor(Color.parseColor("#64748B"))
            textSize = 15f
            background = roundedBox("#090E1A", "#263244", 12f)
            setPadding(dp(14f), dp(12f), dp(14f), dp(12f))
        }

        phoneGroup.addView(lblPhone)
        phoneGroup.addView(etPhone)

        val tvFeedback = TextView(this).apply {
            text = ""
            textSize = 13f
            visibility = View.GONE
            setPadding(dp(2f), dp(12f), dp(2f), dp(2f))
        }

        val btnContinueDashboard = Button(this).apply {
            text = "Continue to Dashboard"
            setTextColor(Color.WHITE)
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            isAllCaps = false
            visibility = View.GONE
            background = roundedBox("#059669", null, 12f)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = dp(12f)
            layoutParams = lp
            setPadding(dp(16f), dp(12f), dp(16f), dp(12f))
        }

        btnContinueDashboard.setOnClickListener {
            prefs.edit().putBoolean("is_logged_in", true).apply()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        fun showMessage(msg: String, isSuccess: Boolean = false, showProceed: Boolean = false) {
            tvFeedback.text = msg
            tvFeedback.setTextColor(Color.parseColor(if (isSuccess) "#34D399" else "#F87171"))
            tvFeedback.visibility = View.VISIBLE
            btnContinueDashboard.visibility = if (showProceed) View.VISIBLE else View.GONE
        }

        fun clearMessage() {
            tvFeedback.text = ""
            tvFeedback.visibility = View.GONE
            btnContinueDashboard.visibility = View.GONE
        }

        val btnPrimary = Button(this).apply {
            text = "Sign In"
            setTextColor(Color.WHITE)
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            isAllCaps = false
            background = roundedBox("#2563EB", null, 12f)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = dp(18f)
            layoutParams = lp
            setPadding(dp(16f), dp(13f), dp(16f), dp(13f))
        }

        val tvOrDivider = TextView(this).apply {
            text = "or continue with"
            setTextColor(Color.parseColor("#64748B"))
            textSize = 12f
            gravity = Gravity.CENTER
            setPadding(0, dp(14f), 0, dp(10f))
        }

        val altRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        val btnPhoneAlt = Button(this).apply {
            text = "Phone"
            setTextColor(Color.parseColor("#E2E8F0"))
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            isAllCaps = false
            background = roundedBox("#111C33", "#263244", 12f)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                rightMargin = dp(6f)
            }
            setPadding(dp(12f), dp(11f), dp(12f), dp(11f))
        }

        val btnGoogleAlt = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = roundedBox("#111C33", "#263244", 12f)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = dp(6f)
            }
            setPadding(dp(12f), dp(11f), dp(12f), dp(11f))
        }

        val ivGoogle = ImageView(this).apply {
            setImageDrawable(googleBadgeDrawable())
            layoutParams = LinearLayout.LayoutParams(dp(20f), dp(20f)).apply {
                rightMargin = dp(8f)
            }
        }

        val tvGoogle = TextView(this).apply {
            text = "Google"
            setTextColor(Color.parseColor("#E2E8F0"))
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
        }

        btnGoogleAlt.addView(ivGoogle)
        btnGoogleAlt.addView(tvGoogle)

        altRow.addView(btnPhoneAlt)
        altRow.addView(btnGoogleAlt)

        fun refreshViewState() {
            clearMessage()
            if (isPhoneMode) {
                emailGroup.visibility = View.GONE
                phoneGroup.visibility = View.VISIBLE
                tvModeTitle.text = "Phone sign in"
                tvModeDesc.text = "Enter your mobile number to continue"
                btnPrimary.text = "Continue with Phone"
                btnPhoneAlt.text = "Email"
            } else {
                emailGroup.visibility = View.VISIBLE
                phoneGroup.visibility = View.GONE
                btnPhoneAlt.text = "Phone"
                if (isSignUpMode) {
                    tvModeTitle.text = "Create account"
                    tvModeDesc.text = "Set your email and password"
                    btnPrimary.text = "Create Account"
                } else {
                    tvModeTitle.text = "Welcome back"
                    tvModeDesc.text = "Sign in to continue"
                    btnPrimary.text = "Sign In"
                }
            }
        }

        btnPhoneAlt.setOnClickListener {
            isPhoneMode = !isPhoneMode
            refreshViewState()
        }

        btnGoogleAlt.setOnClickListener {
            clearMessage()
            showMessage("Google sign-in selected. Tap Continue to Dashboard to proceed.", isSuccess = true, showProceed = true)
        }

        btnPrimary.setOnClickListener {
            clearMessage()
            if (isPhoneMode) {
                val phoneDigits = etPhone.text.toString().filter { it.isDigit() }
                if (phoneDigits.length < 10) {
                    showMessage("Please enter a valid 10-digit phone number")
                    return@setOnClickListener
                }
                prefs.edit().putString("user_phone", phoneDigits).apply()
                showMessage("Phone verified ($phoneDigits). Confirm to open Dashboard.", isSuccess = true, showProceed = true)
                return@setOnClickListener
            }

            val email = etEmail.text.toString().trim().lowercase()
            val password = etPassword.text.toString()

            if (email.isEmpty() || !email.contains("@")) {
                showMessage("Please enter a valid email address")
                return@setOnClickListener
            }
            if (password.length < 6) {
                showMessage("Password must be at least 6 characters")
                return@setOnClickListener
            }

            if (isSignUpMode) {
                prefs.edit()
                    .putString("user_email", email)
                    .putString("user_password", password)
                    .apply()
                showMessage(
                    "Password saved for $email. Confirm below to continue to Dashboard.",
                    isSuccess = true,
                    showProceed = true
                )
            } else {
                val savedEmail = prefs.getString("user_email", null)
                val savedPass = prefs.getString("user_password", null)

                if (savedEmail == null || savedPass == null) {
                    showMessage("No account found. Switch to Create account first.")
                    return@setOnClickListener
                }

                if (email == savedEmail && password == savedPass) {
                    prefs.edit().putBoolean("is_logged_in", true).apply()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    showMessage("Incorrect email or password")
                }
            }
        }

        val switchRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = dp(18f)
            layoutParams = lp
        }

        val tvSwitchLabel = TextView(this).apply {
            text = "Don't have an account? "
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 13f
        }

        val tvSwitchLink = TextView(this).apply {
            text = "Create account"
            setTextColor(Color.parseColor("#60A5FA"))
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(4f), dp(4f), dp(4f), dp(4f))
        }

        fun updateSwitchLabel() {
            if (isSignUpMode) {
                tvSwitchLabel.text = "Already have an account? "
                tvSwitchLink.text = "Sign in"
            } else {
                tvSwitchLabel.text = "Don't have an account? "
                tvSwitchLink.text = "Create account"
            }
        }

        tvSwitchLink.setOnClickListener {
            isPhoneMode = false
            isSignUpMode = !isSignUpMode
            updateSwitchLabel()
            refreshViewState()
        }

        switchRow.addView(tvSwitchLabel)
        switchRow.addView(tvSwitchLink)

        card.addView(tvModeTitle)
        card.addView(tvModeDesc)
        card.addView(emailGroup)
        card.addView(phoneGroup)
        card.addView(tvFeedback)
        card.addView(btnContinueDashboard)
        card.addView(btnPrimary)
        card.addView(tvOrDivider)
        card.addView(altRow)

        page.addView(brandBox)
        page.addView(card)
        page.addView(switchRow)

        scroll.addView(page)
        setContentView(scroll)
    }
}
