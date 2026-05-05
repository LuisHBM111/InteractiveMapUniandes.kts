package com.uniandes.interactivemapuniandes.ui.home

import com.uniandes.interactivemapuniandes.domain.routing.Route

data class HomeUiState(
    val isRouteLoading: Boolean = false,
    val route: Route? = null,
    val routeError: String? = null
)
