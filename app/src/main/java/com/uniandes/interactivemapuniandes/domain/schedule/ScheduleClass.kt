package com.uniandes.interactivemapuniandes.domain.schedule

data class ScheduleClass(
    val id: String,
    val title: String,
    val courseCode: String,
    val section: String?,
    val nrc: String?,
    val startsAt: String,
    val endsAt: String,
    val timezone: String,
    val rawLocation: String?,
    val roomName: String?,
    val roomCode: String?,
    val buildingName: String?,
    val buildingCode: String?,
    val instructorName: String?,
    val recurrenceDays: List<String>,
    val recurrenceUntilDate: String?
)
