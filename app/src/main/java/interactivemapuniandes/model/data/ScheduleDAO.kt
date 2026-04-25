package interactivemapuniandes.model.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import interactivemapuniandes.model.entity.ScheduleClassEntity
import interactivemapuniandes.model.entity.ScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDAO {
    @Query("SELECT * FROM schedules WHERE isCurrent = 1 LIMIT 1")
    fun observeCurrentSchedule(): Flow<ScheduleEntity?>

    @Query("SELECT * FROM schedules WHERE isCurrent = 1 LIMIT 1")
    suspend fun getCurrentSchedule(): ScheduleEntity?

    @Query("SELECT * FROM schedule_classes WHERE scheduleId = :scheduleId ORDER BY startsAt ASC")
    fun observeClassesForSchedule(scheduleId: String): Flow<List<ScheduleClassEntity>>

    @Query("SELECT * FROM schedule_classes ORDER BY startsAt ASC")
    fun observeAllClasses(): Flow<List<ScheduleClassEntity>>

    @Query("SELECT * FROM schedule_classes WHERE id = :id")
    suspend fun getScheduleClass(id: String): ScheduleClassEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSchedule(schedule: ScheduleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertClasses(classes: List<ScheduleClassEntity>)

    @Query("UPDATE schedules SET isCurrent = 0")
    suspend fun clearCurrentScheduleFlag()

    @Query("DELETE FROM schedule_classes WHERE scheduleId = :scheduleId")
    suspend fun deleteClassesForSchedule(scheduleId: String)

    @Transaction
    suspend fun replaceCurrentSchedule(
        schedule: ScheduleEntity,
        classes: List<ScheduleClassEntity>
    ) {
        clearCurrentScheduleFlag()
        upsertSchedule(schedule.copy(isCurrent = true))
        deleteClassesForSchedule(schedule.id)
        upsertClasses(classes)
    }
}
