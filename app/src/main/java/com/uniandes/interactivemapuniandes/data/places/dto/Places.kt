package com.uniandes.interactivemapuniandes.data.places.dto

data class Building(
    val id: String,
    val code: String,
    val name: String,
    val gridReference: String? = null,
    val aliases: List<String>? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val description: String? = null,
    val photoUrl: String? = null,
    val rooms: List<Room>? = null
)

data class Room(
    val id: String,
    val roomCode: String,
    val name: String? = null,
    val floor: String? = null,
    val building: Building? = null
)
