package com.uniandes.interactivemapuniandes.viewmodel

import com.uniandes.interactivemapuniandes.model.repository.RouteRepository
import com.uniandes.interactivemapuniandes.model.state.HomeUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HomeViewModel(
    private val routeRepository: RouteRepository
) {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    suspend fun loadRouteToNextClass(from: String) {
        _uiState.value = _uiState.value.copy(
            isRouteLoading = true,
            route = null,
            routeError = null
        )

        val result = routeRepository.getRouteToNextClass(from)
        _uiState.value = result.fold(
            onSuccess = { route ->
                _uiState.value.copy(
                    isRouteLoading = false,
                    route = route,
                    routeError = null
                )
            },
            onFailure = { error ->
                _uiState.value.copy(
                    isRouteLoading = false,
                    route = null,
                    routeError = error.message ?: "Could not load route"
                )
            }
        )
    }

    suspend fun loadRouteToClass(classId: String, from: String) {
        _uiState.value = _uiState.value.copy(
            isRouteLoading = true,
            route = null,
            routeError = null
        )

        val result = routeRepository.getRouteToClass(classId, from)
        _uiState.value = result.fold(
            onSuccess = { route ->
                _uiState.value.copy(
                    isRouteLoading = false,
                    route = route,
                    routeError = null
                )
            },
            onFailure = { error ->
                _uiState.value.copy(
                    isRouteLoading = false,
                    route = null,
                    routeError = error.message ?: "Could not load route"
                )
            }
        )
    }

    fun clearRoute() {
        if (_uiState.value.route != null) {
            _uiState.value = _uiState.value.copy(route = null)
        }
    }

    fun clearRouteError() {
        if (_uiState.value.routeError != null) {
            _uiState.value = _uiState.value.copy(routeError = null)
        }
    }
}
