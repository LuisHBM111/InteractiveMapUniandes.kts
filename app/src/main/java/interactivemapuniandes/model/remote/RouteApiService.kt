package com.uniandes.interactivemapuniandes.model.remote

import com.google.gson.JsonObject
import com.uniandes.interactivemapuniandes.model.data.NearestNodeResponse
import com.uniandes.interactivemapuniandes.model.data.NextClassResponseDto
import com.uniandes.interactivemapuniandes.model.data.RouteGraphResponseDto
import interactivemapuniandes.model.data.ScheduleDTO
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface RouteApiService {
    @GET("api/v1/routes/graph/path")
    suspend fun getRoute(
        @Query("from") from: String,
        @Query("to") to: String
    ): Response<RouteGraphResponseDto>

    @GET("api/v1/routes/graph/path")
    suspend fun getGraphPath(
        @Query("from") from: String,
        @Query("to") to: String
    ): Response<JsonObject>

    @GET("api/v1/routes/graph/nearest")
    suspend fun findNearest(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double
    ): Response<NearestNodeResponse>

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

    @GET("api/v1/me/schedules/current")
    suspend fun getCurrentSchedule(
        @Header("Authorization") authorization: String
    ): Response<ScheduleDTO>

    @Multipart
    @POST("api/v1/me/schedules/import/file")
    suspend fun importScheduleFile(
        @Header("Authorization") authorization: String,
        @Part file: MultipartBody.Part,
        @Part("name") name: RequestBody,
        @Part("timezone") timezone: RequestBody,
        @Part("replaceExisting") replaceExisting: RequestBody
    ): Response<JsonObject>
}
