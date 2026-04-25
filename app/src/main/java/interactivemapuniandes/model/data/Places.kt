package com.uniandes.interactivemapuniandes.model.data

data class Building(
    val id: String,
    val code: String,
    val name: String,
    val gridReference: String? = null,
    val aliases: List<String>? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val description: String? = null,
    val rooms: List<Room>? = null
)

data class Room(
    val id: String,
    val roomCode: String,
    val name: String? = null,
    val floor: String? = null,
    val building: Building? = null
)

data class Restaurant(
    val id: String,
    val name: String,
    val priceLevel: Int? = null,
    val averageRating: Double? = null,
    val foodCategory: String? = null,
    val openingHours: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
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
