package interactivemapuniandes.model.repository

import java.time.LocalDate

class ScheduleRepository {

    fun getVisibleDays(
        startDate: LocalDate = LocalDate.now(),
        visibleDayCount: Int = DEFAULT_VISIBLE_DAY_COUNT
    ): List<LocalDate> {
        return (0 until visibleDayCount).map { offset ->
            startDate.plusDays(offset.toLong())
        }
    }

    companion object {
        private const val DEFAULT_VISIBLE_DAY_COUNT = 10
    }
}
