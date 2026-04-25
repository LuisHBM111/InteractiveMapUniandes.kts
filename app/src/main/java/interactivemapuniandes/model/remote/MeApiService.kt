package com.uniandes.interactivemapuniandes.model.remote

import com.uniandes.interactivemapuniandes.model.data.Me
import com.uniandes.interactivemapuniandes.model.data.NextClass
import com.uniandes.interactivemapuniandes.model.data.ScheduledClass
import com.uniandes.interactivemapuniandes.model.data.UpdatePreferencesBody
import com.uniandes.interactivemapuniandes.model.data.UpdateProfileBody
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part

interface MeApiService {
    @GET("api/v1/me")
    suspend fun getMe(): Response<Me>

    @PATCH("api/v1/me/profile")
    suspend fun updateProfile(@Body body: UpdateProfileBody): Response<Any>

    @PATCH("api/v1/me/preferences")
    suspend fun updatePreferences(@Body body: UpdatePreferencesBody): Response<Any>

    @GET("api/v1/me/classes/next")
    suspend fun getNextClass(): Response<NextClass>

    @GET("api/v1/me/classes/today")
    suspend fun listTodayClasses(): Response<List<ScheduledClass>>

    @GET("api/v1/me/schedules/current/classes")
    suspend fun listCurrentScheduleClasses(): Response<List<ScheduledClass>>

    @Multipart
    @POST("api/v1/me/schedules/import/file")
    suspend fun importScheduleFile(
        @Part file: MultipartBody.Part,
        @Part("name") name: okhttp3.RequestBody?,
        @Part("replaceExisting") replaceExisting: okhttp3.RequestBody?
    ): Response<Any>
}
