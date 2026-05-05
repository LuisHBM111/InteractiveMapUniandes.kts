package com.uniandes.interactivemapuniandes.data.routing.dto

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

data class NearestNodeResponse(
    val node: RoutePathNode,
    val distanceMeters: Int
)
