package interactivemapuniandes.viewmodel

import interactivemapuniandes.model.repository.ScheduleRepository
import interactivemapuniandes.model.entity.ScheduleClassEntity
import interactivemapuniandes.model.state.ScheduleDayUi
import interactivemapuniandes.model.state.ScheduleUiState
import java.io.IOException
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeParseException
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

    suspend fun observeScheduleClasses() {
        scheduleRepository.observeAllClasses().collect { classes ->
            val selectedDate = _uiState.value.selectedDate
            _uiState.value = _uiState.value.copy(
                scheduleClasses = classes,
                classesForSelectedDay = classes.filterByDate(selectedDate)
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

        val result = scheduleRepository.refreshCurrentSchedule()

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

        val result = scheduleRepository.importScheduleFile(
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
            },
            classesForSelectedDay = _uiState.value.scheduleClasses.filterByDate(date)
        )
    }

    private fun List<ScheduleClassEntity>.filterByDate(date: LocalDate): List<ScheduleClassEntity> {
        return filter { scheduleClass ->
            scheduleClass.occursOn(date)
        }.sortedBy { scheduleClass ->
            scheduleClass.startsAt
        }
    }

    private fun ScheduleClassEntity.occursOn(date: LocalDate): Boolean {
        val startDate = localStartDate() ?: return false
        if (date < startDate) {
            return false
        }

        val untilDate = recurrenceUntilDate?.toLocalDateOrNull(timezone)
        if (untilDate != null && date > untilDate) {
            return false
        }

        val days = recurrenceDays
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            .orEmpty()

        if (days.isEmpty()) {
            return startDate == date
        }

        return date.dayOfWeek.toBackendDayCode() in days
    }

    private fun ScheduleClassEntity.localStartDate(): LocalDate? {
        return try {
            Instant.parse(startsAt)
                .atZone(ZoneId.of(timezone))
                .toLocalDate()
        } catch (error: DateTimeParseException) {
            null
        } catch (error: Exception) {
            null
        }
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

    suspend fun clearLocalCache(){
        _uiState.value = _uiState.value.copy(
            isRefreshing = true,
            scheduleError = null,
            scheduleImportSuccess = false,
            canRetryScheduleRefresh = false
        )

        val result = scheduleRepository.clearLocalCache()

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
                    scheduleError = "We couldn't clear the saved schedule. Please try again.",
                    canRetryScheduleRefresh = false,
                    isShowingCachedData = false
                )
            }
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
