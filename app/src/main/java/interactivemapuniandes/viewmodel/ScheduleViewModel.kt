package interactivemapuniandes.viewmodel

import interactivemapuniandes.model.repository.ScheduleRepository
import interactivemapuniandes.model.state.ScheduleDayUi
import interactivemapuniandes.model.state.ScheduleUiState
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ScheduleViewModel(
    private val scheduleRepository: ScheduleRepository
) {

    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    init {
        loadScheduleOverview()
    }

    fun loadScheduleOverview() {
        val selectedDate = _uiState.value.selectedDate
        val visibleDays = scheduleRepository.getVisibleDays()

        _uiState.value = _uiState.value.copy(
            dayItems = visibleDays.map { date ->
                date.toScheduleDayUi(selectedDate)
            }
        )
    }

    fun selectDate(date: LocalDate) {
        if (_uiState.value.selectedDate == date) {
            return
        }

        val visibleDays = scheduleRepository.getVisibleDays()
        _uiState.value = _uiState.value.copy(
            selectedDate = date,
            dayItems = visibleDays.map { visibleDate ->
                visibleDate.toScheduleDayUi(date)
            }
        )
    }

    private fun LocalDate.toScheduleDayUi(selectedDate: LocalDate): ScheduleDayUi {
        return ScheduleDayUi(
            date = this,
            dayLabel = dayOfWeek
                .getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                .uppercase(Locale.ENGLISH),
            dayNumber = dayOfMonth.toString(),
            isSelected = this == selectedDate
        )
    }
}
