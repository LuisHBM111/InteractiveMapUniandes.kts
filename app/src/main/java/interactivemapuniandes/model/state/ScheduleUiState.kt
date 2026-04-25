package interactivemapuniandes.model.state

import interactivemapuniandes.model.entity.ScheduleClassEntity
import java.time.LocalDate

data class ScheduleUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val dayItems: List<ScheduleDayUi> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isImportingSchedule: Boolean = false,
    val scheduleImportSuccess: Boolean = false,
    val scheduleClasses: List<ScheduleClassEntity> = emptyList(),
    val scheduleError: String? = null,
    val isShowingCachedData: Boolean = false,
    val canRetryScheduleRefresh: Boolean = false,
    val classesForSelectedDay: List<ScheduleClassEntity> = emptyList()
)

data class ScheduleDayUi(
    val date: LocalDate,
    val dayLabel: String,
    val dayNumber: String,
    val isSelected: Boolean = false
)
