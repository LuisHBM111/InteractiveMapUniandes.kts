package interactivemapuniandes.utils

import android.app.Activity
import android.content.Intent
import androidx.core.content.ContextCompat.startActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.uniandes.interactivemapuniandes.R
import com.uniandes.interactivemapuniandes.ui.HomeActivity
import interactivemapuniandes.ui.SettingsActivity

fun BottomNavigationView.setupNavigation(activity: Activity, current: String) {

    val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

    when (current) {
        "explore" -> {
            bottomNav.selectedItemId = R.id.nav_explore
        }
        "schedules" -> {
            bottomNav.selectedItemId = R.id.nav_schedules
        }
        "alerts" -> {
            bottomNav.selectedItemId = R.id.nav_alerts
        }
        "settings" -> {
            bottomNav.selectedItemId = R.id.nav_settings
        }
    }

    bottomNav.setOnItemSelectedListener { item ->
        when (item.itemId) {
            R.id.nav_explore -> {
                val intent = Intent(activity, HomeActivity::class.java)
                activity.startActivity(intent)
                true
            }
            R.id.nav_schedules -> {
                val intent = Intent(activity, HomeActivity::class.java)
                activity.startActivity(intent)
                true
            }
            R.id.nav_alerts -> {
                val intent = Intent(activity, HomeActivity::class.java)
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
