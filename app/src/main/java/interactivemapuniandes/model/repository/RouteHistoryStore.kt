package com.uniandes.interactivemapuniandes.model.repository

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

// Tracks how often each (from, to) pair has been requested. Sprint 3 BQ #2:
// "What are the 5 most common routes taken by the student?".
data class RouteFrequency(val from: String, val to: String, val count: Int)

class RouteHistoryStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("route_history", Context.MODE_PRIVATE)

    fun record(from: String, to: String) {
        val all = readAll().toMutableList()
        val key = "$from|$to"
        val idx = all.indexOfFirst { "${it.from}|${it.to}" == key }
        if (idx >= 0) {
            val r = all[idx]
            all[idx] = r.copy(count = r.count + 1)
        } else {
            all.add(RouteFrequency(from, to, 1))
        }
        save(all)
    }

    fun topFive(): List<RouteFrequency> = readAll()
        .sortedByDescending { it.count }
        .take(5)

    private fun readAll(): List<RouteFrequency> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        val arr = JSONArray(raw)
        return List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            RouteFrequency(o.optString("from"), o.optString("to"), o.optInt("count", 1))
        }
    }

    private fun save(list: List<RouteFrequency>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().apply {
                put("from", it.from); put("to", it.to); put("count", it.count)
            })
        }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    companion object { private const val KEY = "history" }
}
