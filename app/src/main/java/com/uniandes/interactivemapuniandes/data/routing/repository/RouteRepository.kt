package com.uniandes.interactivemapuniandes.data.routing.repository

import com.uniandes.interactivemapuniandes.domain.routing.Route

interface RouteRepository {
    suspend fun getGraphPath(from: String, to: String): Result<Route>

    suspend fun getRouteToNextClass(from: String): Result<Route>

    suspend fun getRouteToClass(classId: String, from: String): Result<Route>
}
