package interactivemapuniandes.model.data.mappers

import interactivemapuniandes.model.data.input.ScheduleClassInput
import interactivemapuniandes.model.entity.ScheduleClassEntity
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

fun ScheduleClassInput.toScheduleClassEntity(scheduleId: String): ScheduleClassEntity {
    val timezone = "America/Bogota"

    return ScheduleClassEntity(
        id = UUID.randomUUID().toString(),
        scheduleId = scheduleId,
        title = className.trim(),
        courseCode = courseCode.trim(),
        section = section.trim().ifBlank { null },
        nrc = nrc.trim().ifBlank { null },
        startsAt = toInstantString(startDate, startTime, timezone),
        endsAt = toInstantString(startDate, endTime, timezone),
        timezone = timezone,
        rawLocation = buildRawLocation(),
        roomName = roomCode.trim().ifBlank { null },
        roomCode = roomCode.trim().ifBlank { null },
        buildingName = null,
        buildingCode = buildingCode.trim().ifBlank { null },
        instructorName = instructor.trim().ifBlank { null },
        recurrenceDays = days.joinToString(","),
        recurrenceUntilDate = toEndOfDayInstantString(untilDate, timezone),
        isUserCreated = true
    )
}

private fun ScheduleClassInput.buildRawLocation(): String? {
    val building = buildingCode.trim()
    val room = roomCode.trim()

    return when {
        building.isBlank() && room.isBlank() -> null
        room.isBlank() -> "Campus: CAMPUS PRINCIPAL Edificio: $building"
        else -> "Campus: CAMPUS PRINCIPAL Edificio: $building Salón: $room"
    }
}

private fun toInstantString(dateText: String, timeText: String, timezone: String): String {
    val date = LocalDate.parse(dateText.trim(), DateTimeFormatter.ofPattern("M/d/yyyy", Locale.US))
    val time = LocalTime.parse(
        timeText.trim().uppercase(Locale.US),
        DateTimeFormatter.ofPattern("h:mm a", Locale.US)
    )

    return LocalDateTime.of(date, time)
        .atZone(ZoneId.of(timezone))
        .toInstant()
        .toString()
}

private fun toEndOfDayInstantString(dateText: String, timezone: String): String? {
    if (dateText.isBlank()) {
        return null
    }

    val date = LocalDate.parse(dateText.trim(), DateTimeFormatter.ofPattern("M/d/yyyy", Locale.US))
    return LocalDateTime.of(date, LocalTime.of(23, 59))
        .atZone(ZoneId.of(timezone))
        .toInstant()
        .toString()
}
