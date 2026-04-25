package interactivemapuniandes.model.data

import interactivemapuniandes.model.entity.ScheduleClassEntity
import interactivemapuniandes.model.entity.ScheduleEntity

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
