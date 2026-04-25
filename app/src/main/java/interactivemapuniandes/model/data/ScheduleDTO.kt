package interactivemapuniandes.model.data

data class ScheduleDTO(
    val id: String,
    val name: String,
    val timezone: String,
    val sourceType: String? = null,
    val sourceFileName: String? = null,
    val isDefaultSample: Boolean = false,
    val importedAt: String? = null,
    val lastUpdatedAt: String? = null,
    val classes: List<ScheduleClassDto> = emptyList()
)

data class ScheduleClassDto(
    val id: String,
    val title: String,
    val courseCode: String,
    val section: String? = null,
    val nrc: String? = null,
    val startsAt: String,
    val endsAt: String,
    val timezone: String,
    val rawLocation: String? = null,
    val room: ScheduleRoomDto? = null,
    val instructors: List<ScheduleInstructorDto> = emptyList(),
    val recurrenceRule: ScheduleRecurrenceRuleDto? = null
)

data class ScheduleRoomDto(
    val name: String,
    val roomCode: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val floor: String? = null,
    val building: ScheduleBuildingDto? = null
)

data class ScheduleBuildingDto(
    val name: String,
    val code: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val gridReference: String? = null
)

data class ScheduleInstructorDto(
    val fullName: String,
    val email: String? = null,
    val department: String? = null
)

data class ScheduleRecurrenceRuleDto(
    val frequency: String,
    val interval: Int,
    val byDay: List<String> = emptyList(),
    val untilDate: String? = null,
    val timezone: String
)