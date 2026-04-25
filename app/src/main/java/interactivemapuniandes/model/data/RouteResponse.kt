package com.uniandes.interactivemapuniandes.model.data

data class RouteResponse(
    val from: String,
    val to: String,
    val totalTimeSeconds: Int,
    val totalTimeMinutes: Double,
    val path: List<RoutePathNode>,
    val traversedEdges: List<TraversedEdge>? = null
)

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
