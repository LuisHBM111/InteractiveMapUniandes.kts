package interactivemapuniandes.model.repository

import interactivemapuniandes.model.data.ScheduleDAO
import interactivemapuniandes.model.data.input.ScheduleClassInput
import interactivemapuniandes.model.data.mappers.toScheduleClassEntity
import interactivemapuniandes.model.entity.ScheduleEntity
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ManageClassesRepository(
    private val scheduleDao: ScheduleDAO
) {

    suspend fun saveClass(scheduleClassInput: ScheduleClassInput): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val currentSchedule = getOrCreateCurrentSchedule()
            val scheduleClass = scheduleClassInput.toScheduleClassEntity(currentSchedule.id)
            scheduleDao.upsertClass(scheduleClass)
            Result.success(Unit)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    private suspend fun getOrCreateCurrentSchedule(): ScheduleEntity {
        val currentSchedule = scheduleDao.getCurrentSchedule()
        if (currentSchedule != null) {
            return currentSchedule
        }

        val now = Instant.now().toString()
        val localSchedule = ScheduleEntity(
            id = UUID.randomUUID().toString(),
            name = "Local schedule",
            timezone = "America/Bogota",
            sourceType = "local",
            sourceFileName = null,
            isDefaultSample = false,
            importedAt = now,
            lastUpdatedAt = now,
            isCurrent = true
        )
        scheduleDao.upsertSchedule(localSchedule)
        return localSchedule
    }
}
