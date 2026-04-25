package com.uniandes.interactivemapuniandes

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import com.uniandes.interactivemapuniandes.model.remote.RetrofitInstance
import com.uniandes.interactivemapuniandes.model.repository.PreferencesRepository
import com.uniandes.interactivemapuniandes.utils.CrashReporter
import com.uniandes.interactivemapuniandes.utils.Telemetry
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okio.Path.Companion.toPath

class CustomApplication : Application(), SingletonImageLoader.Factory {
    override fun onCreate() {
        super.onCreate()
        RetrofitInstance.init(this)
        CrashReporter.install(this)
        Telemetry.event("app_launch")
        applySavedPrefs()
    }

    private fun applySavedPrefs() {
        val prefs = PreferencesRepository(this)
        CoroutineScope(Dispatchers.Main).launch {
            val darkMode = prefs.darkModeFlow().first()
            AppCompatDelegate.setDefaultNightMode(
                if (darkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )

            val language = prefs.languageFlow().first()
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language))
        }
    }

    override fun newImageLoader(context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .crossfade(true)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(File(context.cacheDir, "image_cache").absolutePath.toPath())
                    .maxSizePercent(0.02)
                    .build()
            }
            .components {
                add(
                    OkHttpNetworkFetcherFactory(
                        callFactory = {
                            OkHttpClient.Builder()
                                .connectTimeout(20, TimeUnit.SECONDS)
                                .readTimeout(30, TimeUnit.SECONDS)
                                .writeTimeout(30, TimeUnit.SECONDS)
                                .callTimeout(45, TimeUnit.SECONDS)
                                .build()
                        }
                    )
                )
            }
            .build()
    }
}
