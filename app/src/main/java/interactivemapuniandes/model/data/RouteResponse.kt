package com.uniandes.interactivemapuniandes.model.data

data class RouteResponse(
    val from: String,
    val to: String,
    val path: List<String>,
    val total_time: Int
)
