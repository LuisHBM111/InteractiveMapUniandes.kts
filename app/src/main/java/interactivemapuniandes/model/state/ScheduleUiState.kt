package interactivemapuniandes.model.state

import java.time.LocalDate

data class ScheduleUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val dayItems: List<ScheduleDayUi> = emptyList()
)

data class ScheduleDayUi(
    val date: LocalDate,
    val dayLabel: String,
    val dayNumber: String,
    val isSelected: Boolean = false
)
