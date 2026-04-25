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
    val totalTimeMinutes: Double? = null,
    val path: List<RoutePathNode>,
    val traversedEdges: List<TraversedEdge>? = null
) {
    fun toRouteResponse(): RouteResponse {
        val latitudes = path.mapNotNull { it.latitude }
        val longitudes = path.mapNotNull { it.longitude }

        return RouteResponse(
            from = from,
            to = to,
            path = path.map { it.label },
            totalTime = totalTimeSeconds,
            pathLatitudes = latitudes.takeIf { it.size == path.size }?.toDoubleArray(),
            pathLongitudes = longitudes.takeIf { it.size == path.size }?.toDoubleArray()
        )
    }
}

data class RoutePathNode(
    val id: String,
    val label: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val place: RoutePlace? = null
)

data class RoutePlace(
    val id: String,
    val name: String
)

data class TraversedEdge(
    val from: String,
    val to: String,
    val travelTimeSeconds: Int
)

data class NearestNodeResponse(
    val node: RoutePathNode,
    val distanceMeters: Int
)
