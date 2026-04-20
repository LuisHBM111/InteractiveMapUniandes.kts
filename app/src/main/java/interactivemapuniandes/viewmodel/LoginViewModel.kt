package com.uniandes.interactivemapuniandes.viewmodel

import com.uniandes.interactivemapuniandes.model.repository.AuthRepository
import com.uniandes.interactivemapuniandes.model.state.LoginUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LoginViewModel(private val authRepository: AuthRepository) {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun loginUser(email: String, password: String) {
        _uiState.value = LoginUiState()

        if (email.isBlank()) {
            _uiState.value = _uiState.value.copy(emailError = "Enter your email")
            return
        }

        if (password.isBlank()) {
            _uiState.value = _uiState.value.copy(passwordError = "Enter your password")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true)

        authRepository.login(email, password) { success, message ->
            _uiState.value = if (success) {
                _uiState.value.copy(
                    isLoading = false,
                    loginSuccess = true,
                    generalError = null
                )
            } else {
                _uiState.value.copy(
                    isLoading = false,
                    generalError = message
                )
            }
        }
    }

    fun clearGeneralError() {
        if (_uiState.value.generalError != null) {
            _uiState.value = _uiState.value.copy(generalError = null)
        }
    }
}
