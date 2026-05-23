package interactivemapuniandes.model.repository

import interactivemapuniandes.model.data.ScheduleDAO
import interactivemapuniandes.model.data.input.ScheduleClassInput
import interactivemapuniandes.model.data.mappers.toScheduleClassEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ManageClassesRepository(
    private val scheduleDao: ScheduleDAO
) {

    suspend fun saveClass(scheduleClassInput: ScheduleClassInput): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val currentSchedule = scheduleDao.getCurrentSchedule()
                ?: return@withContext Result.failure(
                    IllegalStateException("No current schedule found")
                )

            val scheduleClass = scheduleClassInput.toScheduleClassEntity(currentSchedule.id)
            scheduleDao.upsertClass(scheduleClass)
            Result.success(Unit)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }
}
