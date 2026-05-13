package com.uniandes.interactivemapuniandes.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uniandes.interactivemapuniandes.model.repository.RouteRepository
import interactivemapuniandes.model.data.mappers.toRouteUiData
import interactivemapuniandes.model.state.RouteUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RouteViewModel(
    private val routeRepository: RouteRepository,
): ViewModel() {

    private val _uiState = MutableStateFlow(RouteUiState())

    val uiState: StateFlow<RouteUiState> = _uiState.asStateFlow()

    fun getNextClass(){

        _uiState.value = _uiState.value.copy(
            isRouteLoading = true,
            errorMessage = null,
            routeData = null
        )

        viewModelScope.launch {
            val result = routeRepository.getNextClass()

            if (result.isSuccess){
                val route = result.getOrNull()
                val uiData = route?.toRouteUiData()
                _uiState.value = _uiState.value.copy(
                    isRouteLoading = false,
                    routeData = uiData
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isRouteLoading = false,
                    errorMessage = result.exceptionOrNull()?.message
                )
            }
        }

    }

}