package com.uniandes.interactivemapuniandes.utils

import android.app.Activity
import android.content.Intent
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.uniandes.interactivemapuniandes.R
import com.uniandes.interactivemapuniandes.view.AlertsActivity
import com.uniandes.interactivemapuniandes.view.HomeActivity
import com.uniandes.interactivemapuniandes.view.SchedulesActivity
import com.uniandes.interactivemapuniandes.view.SettingsActivity

fun BottomNavigationView.setupNavigation(activity: Activity, current: String) {

    val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

    when (current) { // Highlight the tab that owns the current screen
        "explore" -> bottomNav.selectedItemId = R.id.nav_explore
        "schedules" -> bottomNav.selectedItemId = R.id.nav_schedules
        "alerts" -> bottomNav.selectedItemId = R.id.nav_alerts
        "settings" -> bottomNav.selectedItemId = R.id.nav_settings
    }

    bottomNav.setOnItemSelectedListener { item ->
        val target = when (item.itemId) {
            R.id.nav_explore -> HomeActivity::class.java
            R.id.nav_schedules -> SchedulesActivity::class.java
            R.id.nav_alerts -> AlertsActivity::class.java // Real alerts now
            R.id.nav_settings -> SettingsActivity::class.java
            else -> return@setOnItemSelectedListener false
        }
        if (activity.javaClass == target) return@setOnItemSelectedListener true // Same screen, do nothing
        val intent = Intent(activity, target).apply {
            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT // Reuse existing instance if any
        }
        activity.startActivity(intent)
        true
    }
}
