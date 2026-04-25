package com.uniandes.interactivemapuniandes.model.data

data class Me(
    val id: String,
    val email: String? = null,
    val firebaseUid: String? = null,
    val authProvider: String? = null,
    val profile: UserProfile? = null,
    val preference: UserPreference? = null,
    val latestSchedule: ScheduleSummary? = null,
    val scheduleCount: Int = 0
)

data class UserProfile(
    val id: String? = null,
    val fullName: String? = null,
    val program: String? = null,
    val profileImage: String? = null
)

data class UserPreference(
    val id: String? = null,
    val language: String? = null,
    val darkModeEnabled: Boolean? = null,
    val notificationsEnabled: Boolean? = null,
    val usesMetricUnits: Boolean? = null
)

data class ScheduleSummary(
    val id: String,
    val name: String? = null,
    val importedAt: String? = null,
    val isDefaultSample: Boolean = false
)

data class UpdateProfileBody(
    val fullName: String? = null,
    val program: String? = null,
    val profileImage: String? = null
)

data class UpdatePreferencesBody(
    val language: String? = null,
    val darkModeEnabled: Boolean? = null,
    val notificationsEnabled: Boolean? = null,
    val usesMetricUnits: Boolean? = null
)

data class ScheduledClass(
    val id: String,
    val title: String,
    val courseCode: String? = null,
    val section: String? = null,
    val nrc: String? = null,
    val startsAt: String,
    val endsAt: String,
    val timezone: String? = null,
    val rawLocation: String? = null,
    val room: Room? = null,
    val destination: ClassDestination? = null
)

data class ClassDestination(
    val buildingCode: String? = null,
    val buildingName: String? = null,
    val roomCode: String? = null,
    val routeTarget: String? = null
)

data class NextClass(
    val hasUpcomingClass: Boolean,
    @com.google.gson.annotations.SerializedName("class")
    val scheduledClass: ScheduledClass? = null
)

data class UsageEventBody(
    val eventType: String,
    val feature: String? = null,
    val payload: Map<String, Any?>? = null
)
