package com.uniandes.interactivemapuniandes.view

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
import com.uniandes.interactivemapuniandes.model.repository.AuthRepository
import com.uniandes.interactivemapuniandes.viewmodel.LoginViewModel

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var tvRegister: TextView
    private lateinit var tvForgotPassword: TextView

    private lateinit var loginViewModel: LoginViewModel

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
        loginViewModel = LoginViewModel(AuthRepository(auth))

        // Si ya hay sesión activa, entra directo al Home
        if (auth.currentUser != null) {
            goToHome()
            return
        }

        btnLogin.setOnClickListener {
            loginUserView()
        }

        tvRegister.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }

        tvForgotPassword.setOnClickListener {
            sendResetEmail()
        }
    }

    private fun loginUserView() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        loginViewModel.loginUser(email, password)

        val state = loginViewModel.uiState

        btnLogin.isEnabled = false
        btnLogin.text = "Logging in..."

        etEmail.error = state.emailError
        etPassword.error = state.passwordError

        if(state.loginSuccess){
            goToHome()
        }

        if (state.generalError != null) {
            Toast.makeText(this, state.generalError, Toast.LENGTH_LONG).show()
        }

        btnLogin.isEnabled = true
        btnLogin.text = "Log In  →"

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
