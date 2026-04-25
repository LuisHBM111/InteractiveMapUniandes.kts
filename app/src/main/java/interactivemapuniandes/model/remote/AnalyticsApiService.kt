package com.uniandes.interactivemapuniandes.model.remote

import com.uniandes.interactivemapuniandes.model.data.UsageEventBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AnalyticsApiService {
    @POST("api/v1/analytics/usage")
    suspend fun logUsage(@Body body: UsageEventBody): Response<Any>
}
