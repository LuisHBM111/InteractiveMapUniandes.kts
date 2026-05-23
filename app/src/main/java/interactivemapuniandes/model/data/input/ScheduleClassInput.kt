package interactivemapuniandes.model.data.input

data class ScheduleClassInput(
    val className: String,
    val courseCode: String,
    val section: String,
    val nrc: String,
    val instructor: String,
    val days: List<String>,
    val startTime: String,
    val endTime: String,
    val startDate: String,
    val untilDate: String,
    val buildingCode: String,
    val roomCode: String
)
