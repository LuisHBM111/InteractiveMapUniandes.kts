package com.uniandes.interactivemapuniandes.model.remote

import com.uniandes.interactivemapuniandes.model.data.UsageEventBody
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.Response

// Times every outgoing call and posts a usage_event with the elapsed ms.
// Skips the analytics endpoint itself so we don't loop.
class TelemetryInterceptor : Interceptor {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request()
        val started = System.currentTimeMillis()
        val response = chain.proceed(req)
        val elapsed = System.currentTimeMillis() - started

        val path = req.url.encodedPath
        if (!path.contains("/analytics/")) { // Avoid feedback loop
            scope.launch { runCatching { logUsage(path, elapsed, response.code) } }
        }
        return response
    }

    private suspend fun logUsage(path: String, elapsedMs: Long, status: Int) {
        val body = UsageEventBody(
            eventType = "api_call",
            feature = featureFromPath(path),
            payload = mapOf(
                "path" to path,
                "responseMs" to elapsedMs,
                "status" to status
            )
        )
        RetrofitInstance.analyticsApi.logUsage(body)
    }

    private fun featureFromPath(path: String): String = when {
        "translate" in path -> "translate"
        "graph/path" in path -> "route"
        "graph/nearest" in path -> "nearest"
        "places" in path -> "places"
        "schedules" in path -> "schedule"
        "restaurants" in path -> "restaurants"
        "/me" in path -> "me"
        else -> "other"
    }
}
