package com.uniandes.interactivemapuniandes.model.data

data class RouteResponse(
    val from: String,
    val to: String,
    val path: List<String>,
    val totalTime: Int,
    val classId: String? = null,
    val classTitle: String? = null
)
