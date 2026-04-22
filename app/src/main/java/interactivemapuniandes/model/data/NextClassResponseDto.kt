package com.uniandes.interactivemapuniandes.model.data

import com.google.gson.annotations.SerializedName

data class NextClassResponseDto(
    val hasUpcomingClass: Boolean,
    @SerializedName("class")
    val nextClass: NextClassDto? = null
)

data class NextClassDto(
    val id: String,
    val title: String,
    val courseCode: String,
    val section: String,
    val startsAt: String,
    val endsAt: String,
    val timezone: String,
    val room: RoomDto? = null,
    val occurrence: ClassOccurrenceDto? = null,
    val destination: DestinationDto? = null
)

data class RoomDto(
    val name: String,
    val roomCode: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val building: BuildingDto? = null
)

data class BuildingDto(
    val code: String,
    val name: String,
    val latitude: Double? = null,
    val longitude: Double? = null
)

data class ClassOccurrenceDto(
    val startsAt: String,
    val endsAt: String,
    val isRecurring: Boolean
)

data class DestinationDto(
    val room: DestinationRoomDto? = null,
    val building: DestinationBuildingDto? = null,
    val routeTarget: String,
    val routeTargetType: String,
    val isResolved: Boolean
)

data class DestinationRoomDto(
    val name: String,
    val roomCode: String,
    val latitude: Double? = null,
    val longitude: Double? = null
)

data class DestinationBuildingDto(
    val code: String,
    val name: String,
    val latitude: Double? = null,
    val longitude: Double? = null
)
