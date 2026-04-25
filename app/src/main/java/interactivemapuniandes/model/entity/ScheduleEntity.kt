package interactivemapuniandes.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedules")
data class ScheduleEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val timezone: String,
    val sourceType: String?,
    val sourceFileName: String?,
    val isDefaultSample: Boolean,
    val importedAt: String?,
    val lastUpdatedAt: String?,
    @ColumnInfo(defaultValue = "1")
    val isCurrent: Boolean = true,
    val cachedAt: Long = System.currentTimeMillis()
)
