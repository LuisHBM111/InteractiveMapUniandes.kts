package com.uniandes.interactivemapuniandes.utils

import android.app.Activity
import android.content.Intent
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.uniandes.interactivemapuniandes.R
import com.uniandes.interactivemapuniandes.view.AlertsActivity
import com.uniandes.interactivemapuniandes.view.HomeActivity
import com.uniandes.interactivemapuniandes.view.SettingsActivity
import interactivemapuniandes.view.ScheduleActivity

fun BottomNavigationView.setupNavigation(activity: Activity, current: String) {
    selectedItemId = when (current) {
        "explore" -> R.id.nav_explore
        "schedules" -> R.id.nav_schedules
        "alerts" -> R.id.nav_alerts
        "settings" -> R.id.nav_settings
        else -> selectedItemId
    }

    setOnItemSelectedListener { item ->
        val target = when (item.itemId) {
            R.id.nav_explore -> HomeActivity::class.java
            R.id.nav_schedules -> ScheduleActivity::class.java
            R.id.nav_alerts -> AlertsActivity::class.java
            R.id.nav_settings -> SettingsActivity::class.java
            else -> return@setOnItemSelectedListener false
        }

        if (activity.javaClass == target) {
            return@setOnItemSelectedListener true
        }

        val intent = Intent(activity, target).apply {
            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        }
        activity.startActivity(intent)
        true
    }
}
