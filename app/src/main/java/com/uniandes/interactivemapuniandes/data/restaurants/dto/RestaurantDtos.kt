package com.uniandes.interactivemapuniandes.data.restaurants.dto

data class Restaurant(
    val id: String,
    val name: String,
    val priceLevel: Int? = null,
    val averageRating: Double? = null,
    val foodCategory: String? = null,
    val openingHours: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val photoUrl: String? = null
)

data class Review(
    val id: String,
    val rating: Int,
    val comment: String? = null,
    val createdAt: String? = null,
    val user: ReviewUser? = null
)

data class ReviewUser(
    val id: String,
    val email: String? = null
)

data class CreateReviewBody(
    val rating: Int,
    val comment: String? = null
)
