package com.uniandes.interactivemapuniandes.utils

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

object Telemetry {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO) // Survives Activity lifecycle
    private const val TAG = "Telemetry"

    fun screen(name: String) = post("screen_view", name)

    fun event(feature: String, payload: Map<String, Any?>? = null) = post("feature_use", feature, payload)

    fun adClick(adId: String) = fire { RetrofitInstance.adsApi.click(adId) }

    fun lunchPing(lat: Double, lng: Double, accuracy: Float?) {
        if (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) !in 11..13) return // Only 11:00-14:00 local
        fire {
            RetrofitInstance.analyticsApi.logLocation(
                LocationEventBody(lat, lng, accuracy?.toDouble(), "lunch_window")
            )
        }
    }

    fun crash(message: String, stackTrace: String, appVersion: String?, device: Map<String, Any?>) = fire {
        RetrofitInstance.analyticsApi.logCrash(
            CrashEventBody(message, stackTrace.take(8000), appVersion, device) // Truncate giant stacks
        )
    }

    private fun post(eventType: String, feature: String, payload: Map<String, Any?>? = null) = fire {
        RetrofitInstance.analyticsApi.logUsage(UsageEventBody(eventType, feature, payload))
    }

    private fun fire(block: suspend () -> Unit) {
        scope.launch {
            runCatching { block() }.onFailure { Log.d(TAG, "post failed: ${it.message}") }
        }
    }
}
