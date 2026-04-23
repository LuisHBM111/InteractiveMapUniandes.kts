package com.uniandes.interactivemapuniandes.model.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {
    private const val BASE_URL = "http://192.168.80.13:5000/"
    private const val TRANSLATE_BASE_URL = "https://interactive-map-uniandes-backend-1008290497746.us-central1.run.app/"

    val api: RouteApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RouteApiService::class.java)
    }

    val translateApi: TranslateApiService by lazy { // Setup service for translation endpoint
        Retrofit.Builder()
            .baseUrl(TRANSLATE_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TranslateApiService::class.java)
    }
}
