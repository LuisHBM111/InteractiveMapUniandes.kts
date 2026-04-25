package interactivemapuniandes.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedule_classes")
data class ScheduleClassEntity(
    @PrimaryKey
    val id: String,
    val scheduleId: String,
    val title: String,
    val courseCode: String,
    val section: String?,
    val nrc: String?,
    val startsAt: String,
    val endsAt: String,
    val timezone: String,
    val rawLocation: String?,
    val roomName: String?,
    val roomCode: String?,
    val buildingName: String?,
    val buildingCode: String?,
    val instructorName: String?,
    val recurrenceDays: String?,
    val recurrenceUntilDate: String?,
    @ColumnInfo(defaultValue = "0")
    val syncVersion: Long = 0L
)
