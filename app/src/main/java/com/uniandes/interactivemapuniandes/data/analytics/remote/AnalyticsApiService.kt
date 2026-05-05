package com.uniandes.interactivemapuniandes.data.analytics.remote

import com.uniandes.interactivemapuniandes.data.analytics.dto.CrashEventBody
import com.uniandes.interactivemapuniandes.data.analytics.dto.LocationEventBody
import com.uniandes.interactivemapuniandes.data.analytics.dto.UsageEventBody
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
