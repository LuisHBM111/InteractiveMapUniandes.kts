package com.uniandes.interactivemapuniandes.model.data

data class NotificationDto(
    val id: String,
    val type: String,
    val title: String,
    val body: String? = null,
    val icon: String? = null,
    val read: Boolean = false,
    val createdAt: String? = null
)

data class CreateNotificationBody(
    val type: String,
    val title: String,
    val body: String? = null,
    val icon: String? = null
)

data class AlertDto(
    val id: String,
    val type: String,
    val title: String,
    val body: String? = null,
    val icon: String? = null,
    val active: Boolean = true,
    val createdAt: String? = null,
    val place: Building? = null
)

data class CreateAlertBody(
    val type: String,
    val title: String,
    val body: String? = null,
    val icon: String? = null
)

data class FavoriteDto(
    val id: String,
    val createdAt: String? = null,
    val place: Building? = null
)

data class AdDto(
    val id: String,
    val title: String,
    val imageUrl: String? = null,
    val targetUrl: String? = null
)
