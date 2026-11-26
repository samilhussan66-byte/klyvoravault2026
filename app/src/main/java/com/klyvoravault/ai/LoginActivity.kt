package com.klyvoravault.ai

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scrollView = ScrollView(this).apply {
            setBackgroundColor(0xFF121417.toInt())
            setPadding(50, 80, 50, 50)
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val title = TextView(this).apply {
            text = "KlyvoraVault"
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 28f
        }

        val etEmail = EditText(this).apply {
            hint = "Email address"
            setTextColor(0xFFFFFFFF.toInt())
            setHintTextColor(0xFF7C8B9E.toInt())
            setBackgroundColor(0xFF1E242B.toInt())
            setPadding(36, 28, 36, 28)
        }

        val etPassword = EditText(this).apply {
            hint = "Password"
            setTextColor(0xFFFFFFFF.toInt())
            setHintTextColor(0xFF7C8B9E.toInt())
            setBackgroundColor(0xFF1E242B.toInt())
            setPadding(36, 28, 36, 28)
        }

        val btnLogin = Button(this).apply {
            text = "Login"
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xFF2563EB.toInt())
        }

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Welcome to KlyvoraVault", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }

        layout.addView(title)
        layout.addView(etEmail)
        layout.addView(etPassword)
        layout.addView(btnLogin)
        scrollView.addView(layout)
        setContentView(scrollView)
    }
}
