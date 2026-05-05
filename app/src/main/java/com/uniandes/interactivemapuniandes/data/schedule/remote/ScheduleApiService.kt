package com.uniandes.interactivemapuniandes.data.schedule.remote

import com.google.gson.JsonObject
import com.uniandes.interactivemapuniandes.data.schedule.dto.ScheduleDTO
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ScheduleApiService {
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
