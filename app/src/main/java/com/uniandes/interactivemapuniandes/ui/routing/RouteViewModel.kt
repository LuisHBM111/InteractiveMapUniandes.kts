package com.uniandes.interactivemapuniandes.ui.routing

import com.uniandes.interactivemapuniandes.domain.routing.Route
import com.uniandes.interactivemapuniandes.domain.routing.RouteUseCases
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class RouteUiState(
    val route: Route? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class RouteViewModel(
    private val routeUseCases: RouteUseCases
) {
    private val _state = MutableStateFlow(RouteUiState())
    val state: StateFlow<RouteUiState> = _state.asStateFlow()

    suspend fun fetchRoute(from: String, to: String) {
        _state.value = _state.value.copy(isLoading = true, error = null)

        val result = routeUseCases.getGraphRoute(from, to)
        _state.value = result.fold(
            onSuccess = { route ->
                _state.value.copy(
                    route = route,
                    isLoading = false
                )
            },
            onFailure = { error ->
                _state.value.copy(
                    isLoading = false,
                    error = error.message ?: "Could not load route"
                )
            }
        )
    }
}
