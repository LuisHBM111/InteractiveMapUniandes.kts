package com.uniandes.interactivemapuniandes.viewmodel

import com.uniandes.interactivemapuniandes.model.data.RouteResponse
import com.uniandes.interactivemapuniandes.model.remote.RetrofitInstance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class RouteUiState(
    val route: RouteResponse? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class RouteViewModel {
    private val _state = MutableStateFlow(RouteUiState())
    val state: StateFlow<RouteUiState> = _state.asStateFlow()

    suspend fun fetchRoute(from: String, to: String) {
        _state.value = _state.value.copy(isLoading = true, error = null)
        try {
            val resp = RetrofitInstance.api.getRoute(from, to)
            if (resp.isSuccessful) {
                _state.value = _state.value.copy(
                    route = resp.body()?.toRouteResponse(),
                    isLoading = false
                )
            } else {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Error ${resp.code()}"
                )
            }
        } catch (e: Exception) {
            _state.value = _state.value.copy(isLoading = false, error = e.message)
        }
    }
}
