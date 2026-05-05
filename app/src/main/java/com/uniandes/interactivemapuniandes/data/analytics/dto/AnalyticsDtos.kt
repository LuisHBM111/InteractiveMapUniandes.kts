package com.uniandes.interactivemapuniandes.data.analytics.dto

data class UsageEventBody(
    val eventType: String,
    val feature: String? = null,
    val payload: Map<String, Any?>? = null
)

data class CrashEventBody(
    val message: String,
    val stackTrace: String? = null,
    val appVersion: String? = null,
    val deviceInfo: Map<String, Any?>? = null
)

data class LocationEventBody(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Double? = null,
    val context: String? = null // e.g. "lunch_window", "route_start"
)
