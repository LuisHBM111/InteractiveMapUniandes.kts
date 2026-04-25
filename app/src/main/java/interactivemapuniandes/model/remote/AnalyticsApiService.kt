package com.uniandes.interactivemapuniandes.model.remote

import com.uniandes.interactivemapuniandes.model.data.CrashEventBody
import com.uniandes.interactivemapuniandes.model.data.LocationEventBody
import com.uniandes.interactivemapuniandes.model.data.UsageEventBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AnalyticsApiService {
    @POST("api/v1/analytics/usage")
    suspend fun logUsage(@Body body: UsageEventBody): Response<Any>

    @POST("api/v1/analytics/crash")
    suspend fun logCrash(@Body body: CrashEventBody): Response<Any>

    @POST("api/v1/analytics/location")
    suspend fun logLocation(@Body body: LocationEventBody): Response<Any>
}
