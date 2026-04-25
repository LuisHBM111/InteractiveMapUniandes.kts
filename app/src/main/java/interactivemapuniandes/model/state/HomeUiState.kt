package com.uniandes.interactivemapuniandes.model.state

import com.uniandes.interactivemapuniandes.model.data.RouteResponse

data class HomeUiState(
    val isRouteLoading: Boolean = false,
    val route: RouteResponse? = null,
    val routeError: String? = null
)
