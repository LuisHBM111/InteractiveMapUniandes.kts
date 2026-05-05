package com.uniandes.interactivemapuniandes.data.user.remote

import com.uniandes.interactivemapuniandes.data.user.dto.Me
import com.uniandes.interactivemapuniandes.data.user.dto.NextClass
import com.uniandes.interactivemapuniandes.data.user.dto.UpdatePreferencesBody
import com.uniandes.interactivemapuniandes.data.user.dto.UpdateProfileBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH

interface MeApiService {
    @GET("api/v1/me")
    suspend fun getMe(): Response<Me>

    @PATCH("api/v1/me/profile")
    suspend fun updateProfile(@Body body: UpdateProfileBody): Response<Any>

    @PATCH("api/v1/me/preferences")
    suspend fun updatePreferences(@Body body: UpdatePreferencesBody): Response<Any>

    @GET("api/v1/me/classes/next")
    suspend fun getNextClass(): Response<NextClass>
}
