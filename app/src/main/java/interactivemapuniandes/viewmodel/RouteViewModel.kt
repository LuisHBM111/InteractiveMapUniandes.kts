package com.uniandes.interactivemapuniandes.viewmodel

import android.util.Log
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
        Log.d("RouteDebug", "VM getNextClass start")

        _uiState.value = _uiState.value.copy(
            isRouteLoading = true,
            errorMessage = null,
            routeData = null
        )

        viewModelScope.launch {
            try {
                val result = routeRepository.getToNextClass()
                Log.d("RouteDebug", "VM getNextClass repository result success=${result.isSuccess}")

                if (result.isSuccess){
                    val route = result.getOrNull()
                    Log.d(
                        "RouteDebug",
                        "VM getNextClass dto hasBody=${route != null} hasUpcoming=${route?.hasUpcomingClass} pathNodes=${route?.path?.path?.path?.size}"
                    )
                    if (route == null || !route.hasUpcomingClass || route.path?.path?.path.isNullOrEmpty()) {
                        Log.e("RouteDebug", "VM getNextClass empty route")
                        _uiState.value = _uiState.value.copy(
                            isRouteLoading = false,
                            errorMessage = "No upcoming class route found"
                        )
                    } else {
                        val uiData = route.toRouteUiData()
                        Log.d("RouteDebug", "VM getNextClass mapped steps=${uiData.steps.size}")
                        _uiState.value = _uiState.value.copy(
                            isRouteLoading = false,
                            routeData = uiData
                        )
                    }
                } else {
                    Log.e("RouteDebug", "VM getNextClass failed", result.exceptionOrNull())
                    _uiState.value = _uiState.value.copy(
                        isRouteLoading = false,
                        errorMessage = result.exceptionOrNull()?.message
                    )
                }
            } catch (error: Exception) {
                Log.e("RouteDebug", "VM getNextClass crashed", error)
                _uiState.value = _uiState.value.copy(
                    isRouteLoading = false,
                    errorMessage = error.message
                )
            }
        }

    }

    fun getPreviousClass(){
        Log.d("RouteDebug", "VM getPreviousClass start")

        _uiState.value = _uiState.value.copy(
            isRouteLoading = true,
            errorMessage = null,
            routeData = null
        )

        viewModelScope.launch {
            try {
                val result = routeRepository.getToPreviousClass()
                Log.d("RouteDebug", "VM getPreviousClass repository result success=${result.isSuccess}")

                if (result.isSuccess){
                    val route = result.getOrNull()
                    Log.d(
                        "RouteDebug",
                        "VM getPreviousClass dto hasBody=${route != null} hasPrevious=${route?.hasPreviousClass} pathNodes=${route?.path?.path?.path?.size}"
                    )
                    if (route == null || !route.hasPreviousClass || route.path?.path?.path.isNullOrEmpty()) {
                        Log.e("RouteDebug", "VM getPreviousClass empty route")
                        _uiState.value = _uiState.value.copy(
                            isRouteLoading = false,
                            errorMessage = "No previous class route found"
                        )
                    } else {
                        val uiData = route.toRouteUiData()
                        Log.d("RouteDebug", "VM getPreviousClass mapped steps=${uiData.steps.size}")
                        _uiState.value = _uiState.value.copy(
                            isRouteLoading = false,
                            routeData = uiData
                        )
                    }
                } else {
                    Log.e("RouteDebug", "VM getPreviousClass failed", result.exceptionOrNull())
                    _uiState.value = _uiState.value.copy(
                        isRouteLoading = false,
                        errorMessage = result.exceptionOrNull()?.message
                    )
                }
            } catch (error: Exception) {
                Log.e("RouteDebug", "VM getPreviousClass crashed", error)
                _uiState.value = _uiState.value.copy(
                    isRouteLoading = false,
                    errorMessage = error.message
                )
            }
        }

    }

    fun getRandomClass(){

        val allBuildings = AllBuildings()

        val randomFrom = allBuildings.buildings.random()
        val randomTo = allBuildings.buildings.random()
        Log.d("RouteDebug", "VM getRandomClass start from=$randomFrom to=$randomTo")

        _uiState.value = _uiState.value.copy(
            isRouteLoading = true,
            errorMessage = null,
            routeData = null
        )

        viewModelScope.launch {
            try {
                val result = routeRepository.getSearchClass(randomFrom,randomTo)
                Log.d("RouteDebug", "VM getRandomClass repository result success=${result.isSuccess}")

                if (result.isSuccess){
                    val route = result.getOrNull()
                    Log.d("RouteDebug", "VM getRandomClass dto hasBody=${route != null} pathNodes=${route?.path?.size}")
                    if (route == null || route.path.isEmpty()) {
                        Log.e("RouteDebug", "VM getRandomClass empty route")
                        _uiState.value = _uiState.value.copy(
                            isRouteLoading = false,
                            errorMessage = "Route not found"
                        )
                    } else {
                        val uiData = route.toSearchRouteUiData()
                        Log.d("RouteDebug", "VM getRandomClass mapped steps=${uiData.steps.size}")
                        _uiState.value = _uiState.value.copy(
                            isRouteLoading = false,
                            routeData = uiData
                        )
                    }
                } else {
                    Log.e("RouteDebug", "VM getRandomClass failed", result.exceptionOrNull())
                    _uiState.value = _uiState.value.copy(
                        isRouteLoading = false,
                        errorMessage = result.exceptionOrNull()?.message
                    )
                }
            } catch (error: Exception) {
                Log.e("RouteDebug", "VM getRandomClass crashed", error)
                _uiState.value = _uiState.value.copy(
                    isRouteLoading = false,
                    errorMessage = error.message
                )
            }
        }

    }

    fun getSearchClass(from: String, to: String){
        Log.d("RouteDebug", "VM getSearchClass start from=$from to=$to")

        _uiState.value = _uiState.value.copy(
            isRouteLoading = true,
            errorMessage = null,
            routeData = null
        )

        viewModelScope.launch {
            try {
                val result = routeRepository.getSearchClass(from,to)
                Log.d("RouteDebug", "VM getSearchClass repository result success=${result.isSuccess}")

                if (result.isSuccess){
                    val route = result.getOrNull()
                    Log.d("RouteDebug", "VM getSearchClass dto hasBody=${route != null} pathNodes=${route?.path?.size}")
                    if (route == null || route.path.isEmpty()) {
                        Log.e("RouteDebug", "VM getSearchClass empty route")
                        _uiState.value = _uiState.value.copy(
                            isRouteLoading = false,
                            errorMessage = "Route not found"
                        )
                    } else {
                        val uiData = route.toSearchRouteUiData()
                        Log.d("RouteDebug", "VM getSearchClass mapped steps=${uiData.steps.size}")
                        _uiState.value = _uiState.value.copy(
                            isRouteLoading = false,
                            routeData = uiData
                        )
                    }
                } else {
                    Log.e("RouteDebug", "VM getSearchClass failed", result.exceptionOrNull())
                    _uiState.value = _uiState.value.copy(
                        isRouteLoading = false,
                        errorMessage = result.exceptionOrNull()?.message
                    )
                }
            } catch (error: Exception) {
                Log.e("RouteDebug", "VM getSearchClass crashed", error)
                _uiState.value = _uiState.value.copy(
                    isRouteLoading = false,
                    errorMessage = error.message
                )
            }
        }

    }
}
