package com.uniandes.interactivemapuniandes.data.translator.dto

data class TranslateResponse(
    val original: String,
    val translated: String,
    val targetLanguage: String
)
