package com.uniandes.interactivemapuniandes.model.state

data class LoginUiState(
    val emailError: String? = null,
    val passwordError: String? = null,
    val isLoading: Boolean = false,
    val loginSuccess: Boolean = false,
    val generalError: String? = null
)