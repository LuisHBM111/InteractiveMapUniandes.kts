package com.uniandes.interactivemapuniandes.model.remote

import com.uniandes.interactivemapuniandes.model.data.NearestNodeResponse
import com.uniandes.interactivemapuniandes.model.data.RouteResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface RouteApiService {
    @GET("api/v1/routes/graph/path")
    suspend fun getRoute(
        @Query("from") from: String,
        @Query("to") to: String
    ): Response<RouteResponse>

    @GET("api/v1/routes/graph/nearest")
    suspend fun findNearest(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double
    ): Response<NearestNodeResponse>
}
