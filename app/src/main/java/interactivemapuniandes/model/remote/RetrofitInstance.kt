package com.uniandes.interactivemapuniandes.model.remote

import android.content.Context
import com.uniandes.interactivemapuniandes.BuildConfig
import okhttp3.Cache
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

object RetrofitInstance {
    private const val CACHE_SIZE_BYTES = 10L * 1024 * 1024 // 10 MB disk cache
    private val BASE_URL: String = BuildConfig.BACKEND_BASE_URL

    private var appContext: Context? = null // Set by CustomApplication on startup

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private val httpClient: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor()) // Attach Firebase ID token
            .addInterceptor(TelemetryInterceptor()) // Log responseMs to analytics

        appContext?.let { ctx -> // Only enable disk cache if we have a Context
            val cacheDir = File(ctx.cacheDir, "http-cache")
            builder.cache(Cache(cacheDir, CACHE_SIZE_BYTES))
        }
        builder.build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val api: RouteApiService by lazy { retrofit.create(RouteApiService::class.java) }

    val translateApi: TranslateApiService by lazy { retrofit.create(TranslateApiService::class.java) }

    val placesApi: PlacesApiService by lazy { retrofit.create(PlacesApiService::class.java) }

    val meApi: MeApiService by lazy { retrofit.create(MeApiService::class.java) }

    val analyticsApi: AnalyticsApiService by lazy { retrofit.create(AnalyticsApiService::class.java) }

    val restaurantsApi: RestaurantsApiService by lazy { retrofit.create(RestaurantsApiService::class.java) }

    val notificationsApi: NotificationsApiService by lazy { retrofit.create(NotificationsApiService::class.java) }

    val alertsApi: AlertsApiService by lazy { retrofit.create(AlertsApiService::class.java) }

    val favoritesApi: FavoritesApiService by lazy { retrofit.create(FavoritesApiService::class.java) }

    val adsApi: AdsApiService by lazy { retrofit.create(AdsApiService::class.java) }
}
