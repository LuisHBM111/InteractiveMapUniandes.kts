package com.uniandes.interactivemapuniandes.model.repository

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

// Tiny JSON-on-disk store for hand-entered classes. Sprint 3 needs a local
// schedule fallback for offline use; we keep it in SharedPreferences as JSON
// so we don't pull in Room for a single table.
data class LocalClass(
    val title: String,
    val location: String,
    val day: String,
    val startsAt: String,
    val endsAt: String
)

class LocalScheduleStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("local_schedule", Context.MODE_PRIVATE)

    fun all(): List<LocalClass> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        val arr = JSONArray(raw)
        return List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            LocalClass(
                title = o.optString("title"),
                location = o.optString("location"),
                day = o.optString("day"),
                startsAt = o.optString("startsAt"),
                endsAt = o.optString("endsAt")
            )
        }
    }

    fun add(c: LocalClass) {
        val current = all().toMutableList()
        current.add(c)
        save(current)
    }

    fun clear() = save(emptyList())

    private fun save(list: List<LocalClass>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().apply {
                put("title", it.title)
                put("location", it.location)
                put("day", it.day)
                put("startsAt", it.startsAt)
                put("endsAt", it.endsAt)
            })
        }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    companion object { private const val KEY = "classes" }
}
