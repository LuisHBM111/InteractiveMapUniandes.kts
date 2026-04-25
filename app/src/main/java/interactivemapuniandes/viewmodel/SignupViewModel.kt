package com.uniandes.interactivemapuniandes.viewmodel

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SignupUiState(
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmError: String? = null,
    val generalError: String? = null,
    val isLoading: Boolean = false,
    val signupSuccess: Boolean = false
)

class SignupViewModel(private val auth: FirebaseAuth) {
    private val _state = MutableStateFlow(SignupUiState())
    val state: StateFlow<SignupUiState> = _state.asStateFlow()

    fun signUp(email: String, password: String, confirm: String) {
        if (_state.value.isLoading) return // Block double-taps

        _state.value = SignupUiState() // Clear previous errors

        when {
            email.isBlank() -> {
                _state.value = _state.value.copy(emailError = "Enter your email")
                return
            }
            password.length < 6 -> {
                _state.value = _state.value.copy(passwordError = "Min 6 characters")
                return
            }
            password != confirm -> {
                _state.value = _state.value.copy(confirmError = "Passwords don't match")
                return
            }
        }

        _state.value = _state.value.copy(isLoading = true)
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                _state.value = if (task.isSuccessful) {
                    _state.value.copy(isLoading = false, signupSuccess = true)
                } else {
                    _state.value.copy(
                        isLoading = false,
                        generalError = task.exception?.localizedMessage ?: "Signup failed"
                    )
                }
            }
    }

    fun clearGeneralError() {
        if (_state.value.generalError != null) {
            _state.value = _state.value.copy(generalError = null)
        }
    }
}
