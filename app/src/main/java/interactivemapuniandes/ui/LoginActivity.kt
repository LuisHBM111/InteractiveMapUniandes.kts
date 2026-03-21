package com.uniandes.interactivemapuniandes.ui

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.uniandes.interactivemapuniandes.R

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var tvRegister: TextView
    private lateinit var tvForgotPassword: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvRegister = findViewById(R.id.tvRegister)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)

        // Si ya hay sesión activa, entra directo al Home
        if (auth.currentUser != null) {
            goToHome()
            return
        }

        btnLogin.setOnClickListener {
            loginUser()
        }

        tvRegister.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }

        tvForgotPassword.setOnClickListener {
            sendResetEmail()
        }
    }

    private fun loginUser() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (email.isEmpty()) {
            etEmail.error = "Enter your email"
            etEmail.requestFocus()
            return
        }

        if (password.isEmpty()) {
            etPassword.error = "Enter your password"
            etPassword.requestFocus()
            return
        }

        btnLogin.isEnabled = false
        btnLogin.text = "Logging in..."

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                btnLogin.isEnabled = true
                btnLogin.text = "Log In  →"

                if (task.isSuccessful) {
                    Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                    goToHome()
                } else {
                    Toast.makeText(
                        this,
                        task.exception?.localizedMessage ?: "Login failed",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun sendResetEmail() {
        val email = etEmail.text.toString().trim()

        if (email.isEmpty()) {
            etEmail.error = "Enter your email first"
            etEmail.requestFocus()
            return
        }

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(
                        this,
                        "Password reset email sent",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this,
                        task.exception?.localizedMessage ?: "Could not send reset email",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun goToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }
}