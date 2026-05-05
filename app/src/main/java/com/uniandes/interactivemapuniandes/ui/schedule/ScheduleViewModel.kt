package com.uniandes.interactivemapuniandes.ui.schedule

import com.uniandes.interactivemapuniandes.domain.schedule.ScheduleUseCases
import java.io.IOException
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ScheduleViewModel(
    private val scheduleUseCases: ScheduleUseCases
) {

    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    init {
        loadScheduleOverview()
    }

    suspend fun observeScheduleClasses() {
        scheduleUseCases.observeClasses().collect { classes ->
            val selectedDate = _uiState.value.selectedDate
            _uiState.value = _uiState.value.copy(
                scheduleClasses = classes,
                classesForSelectedDay = scheduleUseCases.getClassesForDay(classes, selectedDate)
            )
        }
    }

    suspend fun loadSchedule() {
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            isRefreshing = true,
            isImportingSchedule = false,
            scheduleImportSuccess = false,
            scheduleError = null,
            canRetryScheduleRefresh = false,
            isShowingCachedData = false
        )

        val result = scheduleUseCases.refreshSchedule()

        _uiState.value = result.fold(
            onSuccess = {
                _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    isImportingSchedule = false,
                    scheduleImportSuccess = false,
                    scheduleError = null,
                    canRetryScheduleRefresh = false,
                    isShowingCachedData = false
                )
            },
            onFailure = { error ->
                _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    isImportingSchedule = false,
                    scheduleImportSuccess = false,
                    scheduleError = error.toScheduleUserMessage(),
                    canRetryScheduleRefresh = true,
                    isShowingCachedData = _uiState.value.scheduleClasses.isNotEmpty()
                )
            }
        )
    }

    suspend fun importScheduleFile(
        fileName: String,
        mimeType: String?,
        fileBytes: ByteArray
    ) {
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            isRefreshing = true,
            isImportingSchedule = true,
            scheduleImportSuccess = false,
            scheduleError = null,
            canRetryScheduleRefresh = false,
            isShowingCachedData = false
        )

        val result = scheduleUseCases.importScheduleFile(
            fileName = fileName,
            mimeType = mimeType,
            fileBytes = fileBytes
        )

        _uiState.value = result.fold(
            onSuccess = {
                _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    isImportingSchedule = false,
                    scheduleImportSuccess = true,
                    scheduleError = null,
                    canRetryScheduleRefresh = false,
                    isShowingCachedData = false
                )
            },
            onFailure = { error ->
                _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    isImportingSchedule = false,
                    scheduleImportSuccess = false,
                    scheduleError = error.toScheduleUserMessage(),
                    canRetryScheduleRefresh = false,
                    isShowingCachedData = _uiState.value.scheduleClasses.isNotEmpty()
                )
            }
        )
    }

    fun clearScheduleImportSuccess() {
        if (_uiState.value.scheduleImportSuccess) {
            _uiState.value = _uiState.value.copy(scheduleImportSuccess = false)
        }
    }

    fun loadScheduleOverview() {
        val selectedDate = _uiState.value.selectedDate
        val visibleDays = scheduleUseCases.getVisibleDays()

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

        val visibleDays = scheduleUseCases.getVisibleDays()
        _uiState.value = _uiState.value.copy(
            selectedDate = date,
            dayItems = visibleDays.map { visibleDate ->
                visibleDate.toScheduleDayUi(date)
            },
            classesForSelectedDay = scheduleUseCases.getClassesForDay(
                _uiState.value.scheduleClasses,
                date
            )
        )
    }

    suspend fun clearLocalCache() {
        _uiState.value = _uiState.value.copy(
            isRefreshing = true,
            scheduleError = null,
            scheduleImportSuccess = false,
            canRetryScheduleRefresh = false
        )

        val result = scheduleUseCases.clearCache()

        _uiState.value = result.fold(
            onSuccess = {
                _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    isImportingSchedule = false,
                    scheduleImportSuccess = false,
                    scheduleError = null,
                    canRetryScheduleRefresh = false,
                    isShowingCachedData = false
                )
            },
            onFailure = {
                _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    isImportingSchedule = false,
                    scheduleImportSuccess = false,
                    scheduleError = "We couldn't clear the saved schedule. Please try again.",
                    canRetryScheduleRefresh = false,
                    isShowingCachedData = false
                )
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

    private fun Throwable.toScheduleUserMessage(): String {
        return when {
            this is IOException -> "We couldn't connect. Check your internet and try again."
            message == "No authenticated Firebase user" -> "Please log in again to load your schedule."
            message == "Backend returned an empty body" -> "Your schedule is not available yet."
            message == "Could not import schedule" -> "We couldn't import your schedule. Please try another .ics file."
            else -> "We couldn't update your schedule. Please try again."
        }
    }
}
