package com.uniandes.interactivemapuniandes.data.schedule.mapper

import com.uniandes.interactivemapuniandes.data.schedule.dto.ScheduleClassDto
import com.uniandes.interactivemapuniandes.data.schedule.dto.ScheduleDTO
import com.uniandes.interactivemapuniandes.data.schedule.entity.ScheduleClassEntity
import com.uniandes.interactivemapuniandes.data.schedule.entity.ScheduleEntity
import com.uniandes.interactivemapuniandes.domain.schedule.ScheduleClass

fun ScheduleDTO.toEntity(): ScheduleEntity {
    return ScheduleEntity(
        id = id,
        name = name,
        timezone = timezone,
        sourceType = sourceType,
        sourceFileName = sourceFileName,
        isDefaultSample = isDefaultSample,
        importedAt = importedAt,
        lastUpdatedAt = lastUpdatedAt,
        isCurrent = true
    )
}

fun ScheduleClassDto.toEntity(scheduleId: String): ScheduleClassEntity {
    return ScheduleClassEntity(
        id = id,
        scheduleId = scheduleId,
        title = title,
        courseCode = courseCode,
        section = section,
        nrc = nrc,
        startsAt = startsAt,
        endsAt = endsAt,
        timezone = timezone,
        rawLocation = rawLocation,
        roomName = room?.name,
        roomCode = room?.roomCode,
        buildingName = room?.building?.name,
        buildingCode = room?.building?.code,
        instructorName = instructors.firstOrNull()?.fullName,
        recurrenceDays = recurrenceRule?.byDay?.joinToString(","),
        recurrenceUntilDate = recurrenceRule?.untilDate
    )
}

fun ScheduleClassEntity.toDomain(): ScheduleClass {
    return ScheduleClass(
        id = id,
        title = title,
        courseCode = courseCode,
        section = section,
        nrc = nrc,
        startsAt = startsAt,
        endsAt = endsAt,
        timezone = timezone,
        rawLocation = rawLocation,
        roomName = roomName,
        roomCode = roomCode,
        buildingName = buildingName,
        buildingCode = buildingCode,
        instructorName = instructorName,
        recurrenceDays = recurrenceDays
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            .orEmpty(),
        recurrenceUntilDate = recurrenceUntilDate
    )
}
