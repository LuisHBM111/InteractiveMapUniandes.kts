package com.uniandes.interactivemapuniandes.view

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.uniandes.interactivemapuniandes.R
import com.uniandes.interactivemapuniandes.model.repository.AuthRepository
import com.uniandes.interactivemapuniandes.utils.Telemetry
import com.uniandes.interactivemapuniandes.viewmodel.LoginViewModel
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var tvRegister: TextView
    private lateinit var tvForgotPassword: TextView
    private lateinit var loginViewModel: LoginViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen() // Must run before super on Android 12+
        super.onCreate(savedInstanceState)
        Telemetry.screen("login")
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvRegister = findViewById(R.id.tvRegister)
        tvForgotPassword = findViewById(R.id.forgotPasswordSend)
        loginViewModel = LoginViewModel(AuthRepository(auth))

        observeUiState()

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
            sendResetEmailView()
        }
    }

    private fun loginUserView() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        loginViewModel.loginUser(email, password)
    }

    private fun sendResetEmailView(){
        val email = etEmail.text.toString().trim()
        loginViewModel.sendResetEmail(email)
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            loginViewModel.uiState.collect { state ->
                btnLogin.isEnabled = !state.isLoading
                btnLogin.text = if (state.isLoading) "Logging in..." else "Log In"

                etEmail.error = state.emailError
                etPassword.error = state.passwordError

                tvForgotPassword.isEnabled = !state.isSendingResetEmail
                tvForgotPassword.text = if (state.isSendingResetEmail) "Sending..." else "Send email"

                if (state.resetEmailSent) {
                    Toast.makeText(this@LoginActivity, "Reset email sent", Toast.LENGTH_LONG).show()
                    loginViewModel.clearResetEmailState()
                }

                state.resetEmailError?.let {
                    Toast.makeText(this@LoginActivity, it, Toast.LENGTH_LONG).show()
                    loginViewModel.clearResetEmailState()
                }

                if (state.loginSuccess) {
                    goToHome()
                }

                state.generalError?.let {
                    Toast.makeText(this@LoginActivity, it, Toast.LENGTH_LONG).show()
                    loginViewModel.clearGeneralError()
                }
            }
        }
    }
    private fun goToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }
}
