package com.uniandes.interactivemapuniandes.core.navigation

import android.app.Activity
import android.content.Intent
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.uniandes.interactivemapuniandes.R
import com.uniandes.interactivemapuniandes.ui.alerts.AlertsActivity
import com.uniandes.interactivemapuniandes.ui.favorites.FavoritesActivity
import com.uniandes.interactivemapuniandes.ui.home.HomeActivity
import com.uniandes.interactivemapuniandes.ui.alerts.NotificationsActivity
import com.uniandes.interactivemapuniandes.ui.restaurants.RestaurantsActivity
import com.uniandes.interactivemapuniandes.ui.search.SearchActivity
import com.uniandes.interactivemapuniandes.ui.settings.SettingsActivity
import com.uniandes.interactivemapuniandes.ui.translator.VoiceTranslatorActivity
import com.uniandes.interactivemapuniandes.ui.schedule.ScheduleActivity

fun BottomNavigationView.setupNavigation(activity: Activity, current: String? = null) {

    val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

    when (current) {
        "explore" -> bottomNav.selectedItemId = R.id.nav_explore
        "schedules" -> bottomNav.selectedItemId = R.id.nav_schedules
        "alerts" -> bottomNav.selectedItemId = R.id.nav_alerts
        "settings" -> bottomNav.selectedItemId = R.id.nav_settings
    }

    bottomNav.setOnItemSelectedListener { item ->
        when (item.itemId) {
            R.id.nav_explore -> {
                val intent = Intent(activity, HomeActivity::class.java)
                activity.startActivity(intent)
                true
            }
            R.id.nav_schedules -> {
                val intent = Intent(activity, ScheduleActivity::class.java)
                activity.startActivity(intent)
                true
            }

            R.id.nav_alerts -> {
                val intent = Intent(activity, AlertsActivity::class.java)
                activity.startActivity(intent)
                true
            }

            R.id.nav_settings -> {
                val intent = Intent(activity, SettingsActivity::class.java)
                activity.startActivity(intent)
                true
            }

            else -> false
        }
    }

}
