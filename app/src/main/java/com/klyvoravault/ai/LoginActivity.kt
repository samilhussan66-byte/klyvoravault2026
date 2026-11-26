package com.klyvoravault.ai

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    private var isCreateMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("KlyvoraVaultAuth", Context.MODE_PRIVATE)

        val scrollView = ScrollView(this).apply {
            setBackgroundColor(0xFF121417.toInt())
            setPadding(50, 80, 50, 50)
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val tvTitle = TextView(this).apply {
            text = "KlyvoraVault"
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 28f
        }

        val tvSubtitle = TextView(this).apply {
            text = "Login to your account"
            setTextColor(0xFF9FB0C3.toInt())
            textSize = 15f
            setPadding(0, 12, 0, 32)
        }

        val etEmail = EditText(this).apply {
            hint = "Email address"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            setTextColor(0xFFFFFFFF.toInt())
            setHintTextColor(0xFF7C8B9E.toInt())
            setBackgroundColor(0xFF1E242B.toInt())
            setPadding(36, 28, 36, 28)
        }

        val spacer1 = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                24
            )
        }

        val etPassword = EditText(this).apply {
            hint = "Password (min 4 chars)"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setTextColor(0xFFFFFFFF.toInt())
            setHintTextColor(0xFF7C8B9E.toInt())
            setBackgroundColor(0xFF1E242B.toInt())
            setPadding(36, 28, 36, 28)
        }

        val spacer2 = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                32
            )
        }

        val btnPrimary = Button(this).apply {
            text = "LOGIN"
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xFF2563EB.toInt())
            setPadding(24, 28, 24, 28)
        }

        val spacer3 = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                24
            )
        }

        val btnToggleMode = Button(this).apply {
            text = "CREATE ACCOUNT"
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xFF1E242B.toInt())
            setPadding(24, 24, 24, 24)
        }

        btnToggleMode.setOnClickListener {
            isCreateMode = !isCreateMode
            if (isCreateMode) {
                tvSubtitle.text = "Create a new account"
                btnPrimary.text = "CREATE ACCOUNT"
                btnToggleMode.text = "BACK TO LOGIN"
            } else {
                tvSubtitle.text = "Login to your account"
                btnPrimary.text = "LOGIN"
                btnToggleMode.text = "CREATE ACCOUNT"
            }
        }

        btnPrimary.setOnClickListener {
            val email = etEmail.text.toString().trim().lowercase()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Email aur password dono dalein", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (isCreateMode) {
                if (password.length < 4) {
                    Toast.makeText(this, "Password kam se kam 4 character ka rakhein", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                prefs.edit()
                    .putString("saved_email", email)
                    .putString("saved_password", password)
                    .apply()

                Toast.makeText(this, "Account ban gaya! Ab Login karein", Toast.LENGTH_SHORT).show()
                isCreateMode = false
                tvSubtitle.text = "Login to your account"
                btnPrimary.text = "LOGIN"
                btnToggleMode.text = "CREATE ACCOUNT"
                etPassword.text.clear()
            } else {
                val savedEmail = prefs.getString("saved_email", null)
                val savedPassword = prefs.getString("saved_password", null)

                if (savedEmail == null || savedPassword == null) {
                    Toast.makeText(this, "Pehle CREATE ACCOUNT dabayein", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (email == savedEmail && password == savedPassword) {
                    Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "Galat email ya password", Toast.LENGTH_SHORT).show()
                }
            }
        }

        layout.addView(tvTitle)
        layout.addView(tvSubtitle)
        layout.addView(etEmail)
        layout.addView(spacer1)
        layout.addView(etPassword)
        layout.addView(spacer2)
        layout.addView(btnPrimary)
        layout.addView(spacer3)
        layout.addView(btnToggleMode)
        scrollView.addView(layout)
        setContentView(scrollView)
    }
}
