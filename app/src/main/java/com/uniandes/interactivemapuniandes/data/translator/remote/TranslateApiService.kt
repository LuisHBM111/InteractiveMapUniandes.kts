package com.uniandes.interactivemapuniandes.data.translator.remote

import com.uniandes.interactivemapuniandes.data.translator.dto.TranslateResponse
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
