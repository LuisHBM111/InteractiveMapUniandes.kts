package com.uniandes.interactivemapuniandes.model.remote

import com.uniandes.interactivemapuniandes.model.data.TranslateResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface TranslateApiService {
    @GET("api/v1/translate")
    suspend fun translateText(
        @Query("text") text: String,
        @Query("targetLang") targetLang: String,
        @Query("sourceLang") sourceLang: String? = null
    ): Response<TranslateResponse>
}
