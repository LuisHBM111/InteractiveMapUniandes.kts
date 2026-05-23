package interactivemapuniandes.model.state

import interactivemapuniandes.model.data.mappers.RouteUiData

data class RouteUiState(
    val isRouteLoading: Boolean = false,
    val routeData: RouteUiData? = null,
    val errorMessage: String? = null
)