package com.uniandes.interactivemapuniandes.viewmodel

import android.widget.EditText
import android.widget.Toast
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.uniandes.interactivemapuniandes.model.repository.AuthRepository
import com.uniandes.interactivemapuniandes.model.state.LoginUiState

class LoginViewModel (private val authRepository: AuthRepository){

    var uiState = LoginUiState()
        private set

    fun loginUser(email: String, password: String) {

        uiState = LoginUiState()

        if (email.isBlank()) {
            uiState = uiState.copy(emailError = "Enter your email")
            return
        }

        if (password.isBlank()) {
            uiState = uiState.copy(passwordError = "Enter your password")
            return
        }

        uiState = uiState.copy(loginSuccess = true)

        authRepository.login(email, password) { success, message ->
            uiState = if (success){
                uiState.copy(isLoading = false, loginSuccess = true)
            } else {
                uiState.copy(isLoading = false, generalError = message)
            }
        }

    }

}