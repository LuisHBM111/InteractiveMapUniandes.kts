package com.uniandes.interactivemapuniandes

import android.app.Application
import android.content.Context
import coil3.disk.DiskCache
import coil3.ImageLoader
import coil3.memory.MemoryCache
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import java.io.File
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import okio.Path.Companion.toPath

class CustomApplication : Application(), SingletonImageLoader.Factory {
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
