package com.uniandes.interactivemapuniandes.utils

import android.content.Context
import android.os.Build
import com.uniandes.interactivemapuniandes.BuildConfig
import org.json.JSONObject
import java.io.PrintWriter
import java.io.StringWriter

// Saves uncaught crashes to disk; flushes to /api/v1/analytics/crash on next launch.
// Wiki BQ #1: "What app code functionality or area causes the most crashes?".
object CrashReporter {
    private const val PREFS = "crash_pending"
    private const val KEY = "buffer"

    fun install(context: Context) {
        flushPending(context.applicationContext) // Drain anything left from a previous crash

        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { persist(context.applicationContext, thread, throwable) }
            previous?.uncaughtException(thread, throwable) // Let the system kill us
        }
    }

    private fun persist(context: Context, thread: Thread, t: Throwable) {
        val sw = StringWriter()
        t.printStackTrace(PrintWriter(sw))
        val obj = JSONObject().apply {
            put("message", t.message ?: t.javaClass.simpleName)
            put("stackTrace", sw.toString())
            put("threadName", thread.name)
            put("appVersion", BuildConfig.VERSION_NAME)
            put("device_model", Build.MODEL)
            put("device_manufacturer", Build.MANUFACTURER)
            put("device_sdk", Build.VERSION.SDK_INT)
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, obj.toString()).apply()
    }

    private fun flushPending(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY, null) ?: return
        prefs.edit().remove(KEY).apply() // Clear before posting so we don't loop on a poison record

        runCatching {
            val o = JSONObject(raw)
            Telemetry.crash(
                message = o.optString("message"),
                stackTrace = o.optString("stackTrace"),
                appVersion = o.optString("appVersion").ifBlank { null },
                device = mapOf(
                    "model" to o.optString("device_model"),
                    "manufacturer" to o.optString("device_manufacturer"),
                    "sdk" to o.optInt("device_sdk"),
                    "thread" to o.optString("threadName")
                )
            )
        }
    }
}
