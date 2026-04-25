package com.uniandes.interactivemapuniandes.model.repository

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

// Last successful route, persisted so we can restore navigation if the app is killed
// or there's no network. Wiki resilience scenario: "Preserve navigation session if app terminates".
data class CachedRoute(
    val from: String,
    val to: String,
    val labels: List<String>,
    val lats: DoubleArray,
    val lngs: DoubleArray,
    val totalTimeSeconds: Int,
    val savedAtMillis: Long
)

class OfflineRouteCache(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("offline_route", Context.MODE_PRIVATE)

    fun save(route: CachedRoute) {
        val labels = JSONArray().apply { route.labels.forEach { put(it) } }
        val lats = JSONArray().apply { route.lats.forEach { put(it) } }
        val lngs = JSONArray().apply { route.lngs.forEach { put(it) } }
        val o = JSONObject()
            .put("from", route.from)
            .put("to", route.to)
            .put("labels", labels)
            .put("lats", lats)
            .put("lngs", lngs)
            .put("total_time", route.totalTimeSeconds)
            .put("saved_at", route.savedAtMillis)
        prefs.edit().putString(KEY, o.toString()).apply()
    }

    fun load(): CachedRoute? {
        val raw = prefs.getString(KEY, null) ?: return null
        return runCatching {
            val o = JSONObject(raw)
            val labelsArr = o.getJSONArray("labels")
            val latsArr = o.getJSONArray("lats")
            val lngsArr = o.getJSONArray("lngs")
            CachedRoute(
                from = o.optString("from"),
                to = o.optString("to"),
                labels = List(labelsArr.length()) { labelsArr.getString(it) },
                lats = DoubleArray(latsArr.length()) { latsArr.getDouble(it) },
                lngs = DoubleArray(lngsArr.length()) { lngsArr.getDouble(it) },
                totalTimeSeconds = o.optInt("total_time"),
                savedAtMillis = o.optLong("saved_at")
            )
        }.getOrNull()
    }

    fun clear() {
        prefs.edit().remove(KEY).apply()
    }

    companion object { private const val KEY = "last_route" }
}
