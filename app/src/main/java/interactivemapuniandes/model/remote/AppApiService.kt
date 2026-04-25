package com.uniandes.interactivemapuniandes.model.remote

import com.uniandes.interactivemapuniandes.model.data.AdDto
import com.uniandes.interactivemapuniandes.model.data.AlertDto
import com.uniandes.interactivemapuniandes.model.data.CreateAlertBody
import com.uniandes.interactivemapuniandes.model.data.CreateNotificationBody
import com.uniandes.interactivemapuniandes.model.data.FavoriteDto
import com.uniandes.interactivemapuniandes.model.data.NotificationDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface NotificationsApiService {
    @GET("api/v1/notifications")
    suspend fun list(): Response<List<NotificationDto>>

    @POST("api/v1/notifications")
    suspend fun create(@Body body: CreateNotificationBody): Response<NotificationDto>
}

interface AlertsApiService {
    @GET("api/v1/alerts")
    suspend fun list(): Response<List<AlertDto>>

    @POST("api/v1/alerts")
    suspend fun create(@Body body: CreateAlertBody): Response<AlertDto>
}

interface FavoritesApiService {
    @GET("api/v1/me/favorites")
    suspend fun list(): Response<List<FavoriteDto>>

    @POST("api/v1/me/favorites/{placeId}")
    suspend fun add(@Path("placeId") placeId: String): Response<FavoriteDto>

    @DELETE("api/v1/me/favorites/{placeId}")
    suspend fun remove(@Path("placeId") placeId: String): Response<Any>
}

interface AdsApiService {
    @GET("api/v1/ads/active")
    suspend fun listActive(): Response<List<AdDto>>

    @POST("api/v1/ads/{id}/click")
    suspend fun click(@Path("id") id: String): Response<Any>
}
