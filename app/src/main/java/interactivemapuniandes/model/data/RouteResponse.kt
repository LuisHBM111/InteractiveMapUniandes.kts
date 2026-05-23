package com.uniandes.interactivemapuniandes.model.data

data class RouteResponse(
    val from: String,
    val to: String,
    val path: List<String>,
    val totalTime: Int,
    val classId: String? = null,
    val classTitle: String? = null,
    val pathLatitudes: DoubleArray? = null,
    val pathLongitudes: DoubleArray? = null
)

data class RouteGraphResponseDto(
    val from: String,
    val to: String,
    val totalTimeSeconds: Int,
    val totalTimeMinutes: Double,
    val path: List<RouteGraphNodeDto>,
    val traversedEdges: List<RouteGraphEdgeDto> = emptyList()
)

data class RouteGraphNodeDto(
    val id: String,
    val label: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val place: RouteGraphPlaceDto? = null
)

data class RouteGraphPlaceDto(
    val id: String? = null,
    val name: String? = null
)

data class RouteGraphEdgeDto(
    val from: String,
    val to: String,
    val travelTimeSeconds: Int
)

data class NearestNodeResponse(
    val node: RouteGraphNodeDto?
)
