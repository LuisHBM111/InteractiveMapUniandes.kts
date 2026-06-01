package interactivemapuniandes.model.data.mappers

import interactivemapuniandes.model.data.input.ScheduleClassInput
import interactivemapuniandes.model.entity.ScheduleClassEntity
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import java.util.UUID

fun ScheduleClassInput.toScheduleClassEntity(scheduleId: String): ScheduleClassEntity {
    val timezone = "America/Bogota"
    val title = className.trim().required("Enter a class name.")
    val code = courseCode.trim().required("Enter a course code.")
    val building = buildingCode.trim().required("Enter a building code.")
    val selectedDays = days.filter { it.isNotBlank() }
    if (selectedDays.isEmpty()) {
        throw IllegalArgumentException("Select at least one day.")
    }

    val parsedStartDate = parseRequiredDate(startDate, "Choose a start date.")
    val parsedStartTime = parseRequiredTime(startTime, "Start time", "Choose a start time.")
    val parsedEndTime = parseRequiredTime(endTime, "End time", "Choose an end time.")
    val parsedStartDateTime = LocalDateTime.of(parsedStartDate, parsedStartTime)
    val parsedEndDateTime = LocalDateTime.of(parsedStartDate, parsedEndTime)
    if (!parsedEndDateTime.isAfter(parsedStartDateTime)) {
        throw IllegalArgumentException("End time must be after start time.")
    }

    val parsedUntilDate = parseOptionalDate(untilDate, "Until date")
    if (parsedUntilDate != null && parsedUntilDate.isBefore(parsedStartDate)) {
        throw IllegalArgumentException("Until date cannot be before start date.")
    }

    return ScheduleClassEntity(
        id = UUID.randomUUID().toString(),
        scheduleId = scheduleId,
        title = title,
        courseCode = code,
        section = section.trim().ifBlank { null },
        nrc = nrc.trim().ifBlank { null },
        startsAt = parsedStartDateTime.toInstantString(timezone),
        endsAt = parsedEndDateTime.toInstantString(timezone),
        timezone = timezone,
        rawLocation = buildRawLocation(),
        roomName = roomCode.trim().ifBlank { null },
        roomCode = roomCode.trim().ifBlank { null },
        buildingName = null,
        buildingCode = building,
        instructorName = instructor.trim().ifBlank { null },
        recurrenceDays = selectedDays.joinToString(","),
        recurrenceUntilDate = parsedUntilDate?.toEndOfDayInstantString(timezone),
        isUserCreated = true
    )
}

private fun ScheduleClassInput.buildRawLocation(): String? {
    val building = buildingCode.trim()
    val room = roomCode.trim()

    return when {
        building.isBlank() && room.isBlank() -> null
        room.isBlank() -> "Campus: CAMPUS PRINCIPAL Edificio: $building"
        else -> "Campus: CAMPUS PRINCIPAL Edificio: $building Salon: $room"
    }
}

private fun String.required(message: String): String {
    if (isBlank()) {
        throw IllegalArgumentException(message)
    }
    return this
}

private fun parseRequiredDate(dateText: String, emptyMessage: String): LocalDate {
    if (dateText.isBlank()) {
        throw IllegalArgumentException(emptyMessage)
    }
    return parseDate(dateText, "Start date")
}

private fun parseOptionalDate(dateText: String, fieldName: String): LocalDate? {
    if (dateText.isBlank()) {
        return null
    }
    return parseDate(dateText, fieldName)
}

private fun parseDate(dateText: String, fieldName: String): LocalDate {
    return try {
        LocalDate.parse(dateText.trim(), DateTimeFormatter.ofPattern("M/d/yyyy", Locale.US))
    } catch (error: DateTimeParseException) {
        throw IllegalArgumentException("$fieldName must use MM/DD/YYYY.")
    }
}

private fun parseRequiredTime(timeText: String, fieldName: String, emptyMessage: String): LocalTime {
    if (timeText.isBlank()) {
        throw IllegalArgumentException(emptyMessage)
    }

    return try {
        LocalTime.parse(
            timeText.trim().uppercase(Locale.US),
            DateTimeFormatter.ofPattern("h:mm a", Locale.US)
        )
    } catch (error: DateTimeParseException) {
        throw IllegalArgumentException("$fieldName must use HH:MM AM/PM.")
    }
}

private fun LocalDateTime.toInstantString(timezone: String): String {
    return atZone(ZoneId.of(timezone))
        .toInstant()
        .toString()
}

private fun LocalDate.toEndOfDayInstantString(timezone: String): String {
    return LocalDateTime.of(this, LocalTime.of(23, 59))
        .atZone(ZoneId.of(timezone))
        .toInstant()
        .toString()
}
