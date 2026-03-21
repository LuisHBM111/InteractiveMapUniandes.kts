package com.uniandes.interactivemapuniandes.data.remote

import com.uniandes.interactivemapuniandes.model.RouteResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface RouteApiService {
    @GET("route")
    suspend fun getRoute(
        @Query("from") from: String,
        @Query("to") to: String
    ): Response<RouteResponse>
}