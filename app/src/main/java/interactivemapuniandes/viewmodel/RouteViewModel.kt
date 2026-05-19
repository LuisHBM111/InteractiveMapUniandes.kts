package com.uniandes.interactivemapuniandes.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uniandes.interactivemapuniandes.model.repository.RouteRepository
import interactivemapuniandes.model.data.mappers.toRouteUiData
import interactivemapuniandes.model.data.mappers.toSearchRouteUiData
import interactivemapuniandes.model.state.RouteUiState
import interactivemapuniandes.utils.AllBuildings
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
            val result = routeRepository.getToNextClass()

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

    fun getPreviousClass(){

        _uiState.value = _uiState.value.copy(
            isRouteLoading = true,
            errorMessage = null,
            routeData = null
        )

        viewModelScope.launch {
            val result = routeRepository.getToPreviousClass()

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

    fun getRandomClass(){

        val allBuildings = AllBuildings()

        val randomFrom = allBuildings.buildings.random()
        val randomTo = allBuildings.buildings.random()

        _uiState.value = _uiState.value.copy(
            isRouteLoading = true,
            errorMessage = null,
            routeData = null
        )

        viewModelScope.launch {
            val result = routeRepository.getSearchClass(randomFrom,randomTo)

            if (result.isSuccess){
                val route = result.getOrNull()
                val uiData = route?.toSearchRouteUiData()
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

    fun getSearchClass(from: String, to: String){

        _uiState.value = _uiState.value.copy(
            isRouteLoading = true,
            errorMessage = null,
            routeData = null
        )

        viewModelScope.launch {
            val result = routeRepository.getSearchClass(from,to)

            if (result.isSuccess){
                val route = result.getOrNull()
                val uiData = route?.toSearchRouteUiData()
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
