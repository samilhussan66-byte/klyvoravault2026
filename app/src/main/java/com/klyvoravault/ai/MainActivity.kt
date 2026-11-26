package com.klyvoravault.ai

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("KlyvoraVaultSession", Context.MODE_PRIVATE)
        val userEmail = prefs.getString("user_email", null)
            ?: prefs.getString("user_phone", null)
            ?: "Active Session"

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

        val tvStatusBanner = TextView(this).apply {
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

        fun showStatus(msg: String, accentHex: String = "#60A5FA") {
            tvStatusBanner.text = msg
            tvStatusBanner.setTextColor(Color.parseColor(accentHex))
            tvStatusBanner.visibility = View.VISIBLE
        }

        fun moduleCard(
            title: String,
            subtitle: String,
            badgeText: String,
            badgeBgHex: String,
            badgeTextHex: String,
            actionLabel: String,
            onAction: () -> Unit
        ): LinearLayout {
            return LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = box("#0F172A", "#1E293B", 20f)
                setPadding(dp(18f), dp(18f), dp(18f), dp(18f))
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                lp.topMargin = dp(14f)
                layoutParams = lp

                val tvBadge = TextView(this@MainActivity).apply {
                    text = badgeText
                    setTextColor(Color.parseColor(badgeTextHex))
                    textSize = 11f
                    typeface = Typeface.DEFAULT_BOLD
                    background = box(badgeBgHex, null, 8f)
                    setPadding(dp(10f), dp(4f), dp(10f), dp(4f))
                    val blp = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    layoutParams = blp
                }

                val tvCardTitle = TextView(this@MainActivity).apply {
                    text = title
                    setTextColor(Color.parseColor("#F8FAFC"))
                    textSize = 17f
                    typeface = Typeface.DEFAULT_BOLD
                    setPadding(0, dp(10f), 0, dp(4f))
                }

                val tvCardSub = TextView(this@MainActivity).apply {
                    text = subtitle
                    setTextColor(Color.parseColor("#94A3B8"))
                    textSize = 13f
                }

                val btnAction = Button(this@MainActivity).apply {
                    text = actionLabel
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
                    setOnClickListener { onAction() }
                }

                addView(tvBadge)
                addView(tvCardTitle)
                addView(tvCardSub)
                addView(btnAction)
            }
        }

        val voiceCard = moduleCard(
            title = "Voice Command Engine",
            subtitle = "Hands-free voice actions and multi-language command module ready for integration.",
            badgeText = "VOICE MODULE",
            badgeBgHex = "#172554",
            badgeTextHex = "#93C5FD",
            actionLabel = "Prepare Voice Module"
        ) {
            showStatus("Voice Command module initialized and ready for next step.", "#60A5FA")
        }

        var isGuardArmed = prefs.getBoolean("anti_theft_armed", false)

        val theftCard = moduleCard(
            title = "Anti-Theft Protection",
            subtitle = if (isGuardArmed) "Protection status: ARMED" else "Protection status: STANDBY",
            badgeText = "SECURITY",
            badgeBgHex = "#064E3B",
            badgeTextHex = "#6EE7B7",
            actionLabel = if (isGuardArmed) "Disarm Protection" else "Arm Protection"
        ) {
            isGuardArmed = !isGuardArmed
            prefs.edit().putBoolean("anti_theft_armed", isGuardArmed).apply()
            showStatus(
                if (isGuardArmed) "Anti-Theft protection is now ARMED." else "Anti-Theft protection set to STANDBY.",
                if (isGuardArmed) "#34D399" else "#FBBF24"
            )
        }

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
}
