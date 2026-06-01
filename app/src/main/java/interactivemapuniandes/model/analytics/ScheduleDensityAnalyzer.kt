package interactivemapuniandes.model.analytics

import interactivemapuniandes.model.entity.ScheduleClassEntity
import interactivemapuniandes.model.state.RecommendedClassDayUi
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

class ScheduleDensityAnalyzer {

    fun recommendDayToAddClass(classes: List<ScheduleClassEntity>): RecommendedClassDayUi? {
        if (classes.isEmpty()) {
            return null
        }

        val classCountByDay = Weekdays.associateWith { 0 }.toMutableMap()

        classes.forEach { scheduleClass ->
            scheduleClass.weekdays().forEach { day ->
                classCountByDay[day] = classCountByDay.getValue(day) + 1
            }
        }

        val recommendedDay = classCountByDay.minByOrNull { it.value } ?: return null
        val dayLabel = recommendedDay.key.getDisplayName(TextStyle.FULL, Locale.ENGLISH)

        return RecommendedClassDayUi(
            dayLabel = dayLabel,
            classCount = recommendedDay.value,
            reason = "$dayLabel has ${recommendedDay.value} scheduled classes."
        )
    }

    private fun ScheduleClassEntity.weekdays(): List<DayOfWeek> {
        val recurrenceWeekdays = recurrenceDays
            ?.split(",")
            ?.mapNotNull { it.trim().toDayOfWeekOrNull() }
            ?.filter { it in Weekdays }
            .orEmpty()

        if (recurrenceWeekdays.isNotEmpty()) {
            return recurrenceWeekdays
        }

        return localStartDayOfWeek()
            ?.takeIf { it in Weekdays }
            ?.let { listOf(it) }
            .orEmpty()
    }

    private fun String.toDayOfWeekOrNull(): DayOfWeek? {
        return when (this) {
            "MO" -> DayOfWeek.MONDAY
            "TU" -> DayOfWeek.TUESDAY
            "WE" -> DayOfWeek.WEDNESDAY
            "TH" -> DayOfWeek.THURSDAY
            "FR" -> DayOfWeek.FRIDAY
            else -> null
        }
    }

    private fun ScheduleClassEntity.localStartDayOfWeek(): DayOfWeek? {
        return try {
            Instant.parse(startsAt)
                .atZone(ZoneId.of(timezone))
                .dayOfWeek
        } catch (error: Exception) {
            null
        }
    }

    private companion object {
        val Weekdays = listOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY
        )
    }
}
