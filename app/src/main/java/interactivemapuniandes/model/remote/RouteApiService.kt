package com.uniandes.interactivemapuniandes.model.remote

import com.google.gson.JsonObject
import com.uniandes.interactivemapuniandes.model.data.NextClassResponseDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface RouteApiService {
    @GET("api/v1/routes/graph/path")
    suspend fun getGraphPath(
        @Query("from") from: String,
        @Query("to") to: String
    ): Response<JsonObject>

    @GET("api/v1/me/routes/to-next-class")
    suspend fun getRouteToNextClass(
        @Header("Authorization") authorization: String,
        @Query("from") from: String
    ): Response<JsonObject>

    @GET("api/v1/me/routes/to-class/{classId}")
    suspend fun getRouteToClass(
        @Header("Authorization") authorization: String,
        @Path("classId") classId: String,
        @Query("from") from: String
    ): Response<JsonObject>

    @GET("api/v1/me/classes/next")
    suspend fun getNextClass(
        @Header("Authorization") authorization: String
    ): Response<NextClassResponseDto>

    @POST("api/v1/me/schedules/import/default")
    suspend fun importDefaultSchedule(
        @Header("Authorization") authorization: String
    ): Response<JsonObject>
}
