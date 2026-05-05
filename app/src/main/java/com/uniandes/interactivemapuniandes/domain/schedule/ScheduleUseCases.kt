package com.uniandes.interactivemapuniandes.domain.schedule

import com.uniandes.interactivemapuniandes.data.schedule.repository.ScheduleRepository
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeParseException
import kotlinx.coroutines.flow.Flow

class ObserveScheduleClassesUseCase(
    private val scheduleRepository: ScheduleRepository
) {
    operator fun invoke(): Flow<List<ScheduleClass>> {
        return scheduleRepository.observeAllClasses()
    }
}

class RefreshCurrentScheduleUseCase(
    private val scheduleRepository: ScheduleRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return scheduleRepository.refreshCurrentSchedule()
    }
}

class ImportScheduleFileUseCase(
    private val scheduleRepository: ScheduleRepository
) {
    suspend operator fun invoke(
        fileName: String,
        mimeType: String?,
        fileBytes: ByteArray
    ): Result<Unit> {
        return scheduleRepository.importScheduleFile(fileName, mimeType, fileBytes)
    }
}

class ClearScheduleCacheUseCase(
    private val scheduleRepository: ScheduleRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return scheduleRepository.clearLocalCache()
    }
}

class GetVisibleScheduleDaysUseCase {
    operator fun invoke(
        startDate: LocalDate = LocalDate.now(),
        visibleDayCount: Int = DEFAULT_VISIBLE_DAY_COUNT
    ): List<LocalDate> {
        return (0 until visibleDayCount).map { offset ->
            startDate.plusDays(offset.toLong())
        }
    }

    private companion object {
        const val DEFAULT_VISIBLE_DAY_COUNT = 10
    }
}

class GetClassesForDayUseCase {
    operator fun invoke(
        classes: List<ScheduleClass>,
        date: LocalDate
    ): List<ScheduleClass> {
        return classes
            .filter { scheduleClass -> scheduleClass.occursOn(date) }
            .sortedBy { scheduleClass -> scheduleClass.startsAt }
    }

    private fun ScheduleClass.occursOn(date: LocalDate): Boolean {
        val startDate = localStartDate() ?: return false
        if (date < startDate) {
            return false
        }

        val untilDate = recurrenceUntilDate?.toLocalDateOrNull(timezone)
        if (untilDate != null && date > untilDate) {
            return false
        }

        if (recurrenceDays.isEmpty()) {
            return startDate == date
        }

        return date.dayOfWeek.toBackendDayCode() in recurrenceDays
    }

    private fun ScheduleClass.localStartDate(): LocalDate? {
        return startsAt.toLocalDateOrNull(timezone)
    }

    private fun String.toLocalDateOrNull(timezone: String): LocalDate? {
        return try {
            Instant.parse(this)
                .atZone(ZoneId.of(timezone))
                .toLocalDate()
        } catch (error: DateTimeParseException) {
            null
        } catch (error: Exception) {
            null
        }
    }

    private fun DayOfWeek.toBackendDayCode(): String {
        return when (this) {
            DayOfWeek.MONDAY -> "MO"
            DayOfWeek.TUESDAY -> "TU"
            DayOfWeek.WEDNESDAY -> "WE"
            DayOfWeek.THURSDAY -> "TH"
            DayOfWeek.FRIDAY -> "FR"
            DayOfWeek.SATURDAY -> "SA"
            DayOfWeek.SUNDAY -> "SU"
        }
    }
}

data class ScheduleUseCases(
    val observeClasses: ObserveScheduleClassesUseCase,
    val refreshSchedule: RefreshCurrentScheduleUseCase,
    val importScheduleFile: ImportScheduleFileUseCase,
    val clearCache: ClearScheduleCacheUseCase,
    val getVisibleDays: GetVisibleScheduleDaysUseCase,
    val getClassesForDay: GetClassesForDayUseCase
)
