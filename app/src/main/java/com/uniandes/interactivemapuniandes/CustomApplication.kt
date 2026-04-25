package com.uniandes.interactivemapuniandes

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.request.crossfade
import com.uniandes.interactivemapuniandes.model.remote.RetrofitInstance
import com.uniandes.interactivemapuniandes.model.repository.PreferencesRepository
import com.uniandes.interactivemapuniandes.utils.CrashReporter
import com.uniandes.interactivemapuniandes.utils.Telemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CustomApplication : Application(), SingletonImageLoader.Factory {
    override fun onCreate() {
        super.onCreate()
        RetrofitInstance.init(this) // OkHttp disk cache needs cacheDir
        CrashReporter.install(this) // Save uncaught crashes; flush previous on launch
        Telemetry.event("app_launch")
        applySavedPrefs() // Pull dark mode + language from DataStore on startup
    }

    private fun applySavedPrefs() {
        val prefs = PreferencesRepository(this)
        CoroutineScope(Dispatchers.Main).launch {
            val dark = prefs.darkModeFlow().first()
            AppCompatDelegate.setDefaultNightMode(
                if (dark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
            val lang = prefs.languageFlow().first()
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(lang))
        }
    }

    override fun newImageLoader(context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .crossfade(true)
            .build()
    }
}
