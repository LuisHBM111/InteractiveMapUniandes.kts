package com.uniandes.interactivemapuniandes.utils

import android.content.Context
import android.util.Log
import com.uniandes.interactivemapuniandes.model.data.CrashEventBody
import com.uniandes.interactivemapuniandes.model.data.LocationEventBody
import com.uniandes.interactivemapuniandes.model.data.UsageEventBody
import com.uniandes.interactivemapuniandes.model.remote.RetrofitInstance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Calendar

// Fire-and-forget telemetry. Backed by /api/v1/analytics/{usage,crash,location}.
// Wiki BQ #1 (crash hotspots), #2 (peak-hour features), #7/10/13 (feature usage),
// #11/12 (lunch foot traffic), #14 (ad CTR / route satisfaction).
object Telemetry {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private const val TAG = "Telemetry"

    fun screen(name: String) {
        post(UsageEventBody(eventType = "screen_view", feature = name))
    }

    fun event(feature: String, payload: Map<String, Any?>? = null) {
        post(UsageEventBody(eventType = "feature_use", feature = feature, payload = payload))
    }

    fun adClick(adId: String) {
        scope.launch {
            runCatching { RetrofitInstance.adsApi.click(adId) } // Wiki BQ #13/#14
        }
    }

    // Lunch window (11:00 - 14:00 local) location pings drive BQ #11/#12.
    fun lunchPing(context: Context, lat: Double, lng: Double, accuracy: Float?) {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (hour !in 11..13) return // Skip outside lunch window
        scope.launch {
            runCatching {
                RetrofitInstance.analyticsApi.logLocation(
                    LocationEventBody(
                        latitude = lat,
                        longitude = lng,
                        accuracyMeters = accuracy?.toDouble(),
                        context = "lunch_window"
                    )
                )
            }.onFailure { Log.d(TAG, "lunchPing failed: ${it.message}") }
        }
    }

    fun crash(message: String, stackTrace: String, appVersion: String?, device: Map<String, Any?>) {
        scope.launch {
            runCatching {
                RetrofitInstance.analyticsApi.logCrash(
                    CrashEventBody(
                        message = message,
                        stackTrace = stackTrace.take(8000), // Truncate giant stacks
                        appVersion = appVersion,
                        deviceInfo = device
                    )
                )
            }.onFailure { Log.d(TAG, "crash post failed: ${it.message}") }
        }
    }

    private fun post(body: UsageEventBody) {
        scope.launch {
            runCatching { RetrofitInstance.analyticsApi.logUsage(body) }
                .onFailure { Log.d(TAG, "usage post failed: ${it.message}") }
        }
    }
}
