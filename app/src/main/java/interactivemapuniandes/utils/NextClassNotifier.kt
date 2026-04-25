package com.uniandes.interactivemapuniandes.utils

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.uniandes.interactivemapuniandes.R
import com.uniandes.interactivemapuniandes.model.data.ScheduledClass
import com.uniandes.interactivemapuniandes.view.HomeActivity
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

// Schedules a single local notification 15 min before the next class. Fires through
// AlarmManager + NextClassReceiver. Wiki: "Display current and next class information".
object NextClassNotifier {
    private const val CHANNEL_ID = "next_class"
    private const val REQUEST_CODE = 7301
    private const val LEAD_MILLIS = 15L * 60L * 1000L

    fun scheduleFor(context: Context, klass: ScheduledClass) {
        val triggerMillis = parseStartMillis(klass.startsAt) ?: return
        val fireAt = triggerMillis - LEAD_MILLIS
        if (fireAt < System.currentTimeMillis()) return // Class already starts in <15 min

        ensureChannel(context)
        val intent = Intent(context, NextClassReceiver::class.java).apply {
            putExtra("title", klass.title)
            putExtra("location", klass.destination?.buildingName ?: klass.rawLocation ?: "")
            putExtra("startsAt", klass.startsAt)
            putExtra("routeTarget", klass.destination?.routeTarget ?: klass.room?.roomCode ?: klass.rawLocation)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        runCatching {
            am.set(AlarmManager.RTC_WAKEUP, fireAt, pi) // Inexact is fine for class reminders
        }
    }

    private fun parseStartMillis(iso: String): Long? {
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSX",
            "yyyy-MM-dd'T'HH:mm:ssX",
            "yyyy-MM-dd'T'HH:mm:ss"
        )
        for (p in patterns) {
            runCatching {
                val fmt = SimpleDateFormat(p, Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
                return fmt.parse(iso)?.time
            }
        }
        return null
    }

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        val ch = NotificationChannel(
            CHANNEL_ID, "Próxima clase",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "Recordatorio 15 min antes de tu próxima clase" }
        nm.createNotificationChannel(ch)
    }

    fun build(context: Context, title: String, body: String): Notification {
        ensureChannel(context)
        val openHome = Intent(context, HomeActivity::class.java)
        val openPi = PendingIntent.getActivity(
            context, 0, openHome,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_schedule)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(openPi)
            .setAutoCancel(true)
            .build()
    }
}

class NextClassReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: return
        val location = intent.getStringExtra("location") ?: ""
        val body = if (location.isNotBlank()) "Empieza pronto en $location" else "Empieza pronto"
        val notif = NextClassNotifier.build(context, "📚 Próxima clase: $title", body)
        val nm = ContextCompat.getSystemService(context, NotificationManager::class.java) ?: return
        nm.notify(7301, notif)
    }
}
