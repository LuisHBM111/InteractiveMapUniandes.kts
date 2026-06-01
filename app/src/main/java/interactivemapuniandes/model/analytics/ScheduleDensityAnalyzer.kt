package interactivemapuniandes.model.analytics

import android.util.LruCache
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
            return RecommendationCache.get(LAST_RECOMMENDATION_KEY)?.copy(
                isFromCache = true,
                reason = "Cached recommendation from the last saved schedule."
            )
        }

        val cacheKey = classes.toDensityCacheKey()
        RecommendationCache.get(cacheKey)?.let { cachedRecommendation ->
            return cachedRecommendation
        }

        val classCountByDay = Weekdays.associateWith { 0 }.toMutableMap()

        classes.forEach { scheduleClass ->
            scheduleClass.weekdays().forEach { day ->
                classCountByDay[day] = classCountByDay.getValue(day) + 1
            }
        }

        val recommendedDay = classCountByDay.minByOrNull { it.value } ?: return null
        val dayLabel = recommendedDay.key.getDisplayName(TextStyle.FULL, Locale.ENGLISH)

        val recommendation = RecommendedClassDayUi(
            dayLabel = dayLabel,
            classCount = recommendedDay.value,
            reason = "$dayLabel has ${recommendedDay.value} scheduled classes."
        )
        RecommendationCache.put(cacheKey, recommendation)
        RecommendationCache.put(LAST_RECOMMENDATION_KEY, recommendation)
        return recommendation
    }

    private fun List<ScheduleClassEntity>.toDensityCacheKey(): String {
        return sortedBy { it.id }.joinToString(separator = "|") { scheduleClass ->
            listOf(
                scheduleClass.id,
                scheduleClass.startsAt,
                scheduleClass.recurrenceDays.orEmpty(),
                scheduleClass.recurrenceUntilDate.orEmpty()
            ).joinToString(separator = "#")
        }
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
        private const val MAX_CACHE_ENTRIES = 16
        private const val LAST_RECOMMENDATION_KEY = "last_density_recommendation"
        private val RecommendationCache = LruCache<String, RecommendedClassDayUi>(MAX_CACHE_ENTRIES)

        val Weekdays = listOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY
        )
    }
}
