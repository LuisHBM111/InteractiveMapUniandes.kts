package com.uniandes.interactivemapuniandes.view

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.uniandes.interactivemapuniandes.R
import com.uniandes.interactivemapuniandes.utils.Telemetry

class SignupActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnSignup: MaterialButton
    private lateinit var btnBack: ImageButton
    private lateinit var tvGoToLogin: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Telemetry.screen("signup")
        enableEdgeToEdge()
        setContentView(R.layout.activity_signup)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnSignup = findViewById(R.id.btnSignup)
        btnBack = findViewById(R.id.btnBack)
        tvGoToLogin = findViewById(R.id.tvGoToLogin)

        btnBack.setOnClickListener {
            finish()
        }

        tvGoToLogin.setOnClickListener {
            finish()
        }

        btnSignup.setOnClickListener {
            signupUser()
        }
    }

    private fun signupUser() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val confirmPassword = etConfirmPassword.text.toString().trim()

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

        if (password.length < 6) {
            etPassword.error = "Password must be at least 6 characters"
            etPassword.requestFocus()
            return
        }

        if (confirmPassword.isEmpty()) {
            etConfirmPassword.error = "Confirm your password"
            etConfirmPassword.requestFocus()
            return
        }

        if (password != confirmPassword) {
            etConfirmPassword.error = "Passwords do not match"
            etConfirmPassword.requestFocus()
            return
        }

        btnSignup.isEnabled = false
        btnSignup.text = "Creating account..."

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                btnSignup.isEnabled = true
                btnSignup.text = "Create account"

                if (task.isSuccessful) {
                    Toast.makeText(this, "Account created successfully", Toast.LENGTH_SHORT).show()

                    // Puedes mandarlo a Home o devolverlo a Login.
                    // Aquí lo mando a Home para que se sienta más fluido.
                    val intent = Intent(this, HomeActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(
                        this,
                        task.exception?.localizedMessage ?: "Signup failed",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }
}
