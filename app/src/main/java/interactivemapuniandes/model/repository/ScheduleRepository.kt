package interactivemapuniandes.model.repository

import com.uniandes.interactivemapuniandes.model.remote.RouteApiService
import com.uniandes.interactivemapuniandes.model.repository.AuthRepository
import interactivemapuniandes.model.data.ScheduleDAO
import interactivemapuniandes.model.data.ScheduleDTO
import interactivemapuniandes.model.data.toEntity
import interactivemapuniandes.model.entity.ScheduleClassEntity
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response

class ScheduleRepository (
    private val api: RouteApiService,
    private val authRepository: AuthRepository,
    private val scheduleDao: ScheduleDAO,
) {

    suspend fun clearLocalCache(): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            scheduleDao.clearScheduleCache()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeAllClasses(): Flow<List<ScheduleClassEntity>> {
        return scheduleDao.observeAllClasses()
    }

    suspend fun refreshCurrentSchedule(): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext executeAuthorizedRequest { authorization ->
            api.getCurrentSchedule(authorization)
        }.map { scheduleDto ->
            saveSchedule(scheduleDto)
        }
    }

    suspend fun importScheduleFile(
        fileName: String,
        mimeType: String?,
        fileBytes: ByteArray,
        scheduleName: String = "Imported schedule",
        timezone: String = "America/Bogota",
        replaceExisting: Boolean = true
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val fileRequestBody = fileBytes.toRequestBody(
            (mimeType ?: DEFAULT_ICS_MIME_TYPE).toMediaTypeOrNull()
        )
        val filePart = MultipartBody.Part.createFormData(
            name = "file",
            filename = fileName.ifBlank { DEFAULT_ICS_FILE_NAME },
            body = fileRequestBody
        )

        val uploadResult = executeAuthorizedUnitRequest { authorization ->
            api.importScheduleFile(
                authorization = authorization,
                file = filePart,
                name = scheduleName.toPlainTextRequestBody(),
                timezone = timezone.toPlainTextRequestBody(),
                replaceExisting = replaceExisting.toString().toPlainTextRequestBody()
            )
        }

        if (uploadResult.isFailure) {
            return@withContext uploadResult
        }

        refreshCurrentSchedule()
    }

    private suspend fun saveSchedule(scheduleDto: ScheduleDTO) {
        val scheduleEntity = scheduleDto.toEntity()
        val scheduleClassEntities = scheduleDto.classes.map { dto ->
            dto.toEntity(scheduleEntity.id)
        }

        scheduleDao.replaceCurrentSchedule(scheduleEntity, scheduleClassEntities)
    }

    private suspend fun <T> executeAuthorizedRequest(
        request: suspend (String) -> Response<T>
    ): Result<T> {
        return executeAuthorizedResponse(
            request = request,
            requireBody = true
        )
    }

    private suspend fun executeAuthorizedUnitRequest(
        request: suspend (String) -> Response<*>
    ): Result<Unit> {
        return try {
            val token = authRepository.getIdToken(forceRefresh = false)
                ?: return Result.failure(IllegalStateException("No authenticated Firebase user"))

            val firstAttempt = request("Bearer $token")
            if (firstAttempt.isSuccessful) {
                return Result.success(Unit)
            }

            if (firstAttempt.code() == 401) {
                val refreshedToken = authRepository.getIdToken(forceRefresh = true)
                    ?: return Result.failure(IllegalStateException("Could not refresh Firebase ID token"))

                val retryAttempt = request("Bearer $refreshedToken")
                if (retryAttempt.isSuccessful) {
                    return Result.success(Unit)
                }
            }

            Result.failure(IllegalStateException("Could not import schedule"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun <T> executeAuthorizedResponse(
        request: suspend (String) -> Response<T>,
        requireBody: Boolean
    ): Result<T> {
        return try {
            val token = authRepository.getIdToken(forceRefresh = false)
                ?: return Result.failure(IllegalStateException("No authenticated Firebase user"))

            val firstAttempt = request("Bearer $token")
            if (firstAttempt.isSuccessful) {
                return firstAttempt.toResult(requireBody)
            }

            if (firstAttempt.code() == 401) {
                val refreshedToken = authRepository.getIdToken(forceRefresh = true)
                    ?: return Result.failure(IllegalStateException("Could not refresh Firebase ID token"))

                val retryAttempt = request("Bearer $refreshedToken")
                return retryAttempt.toResult(requireBody)
            }

            Result.failure(IllegalStateException("Could not refresh schedule"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun <T> Response<T>.toResult(requireBody: Boolean): Result<T> {
        if (!isSuccessful) {
            return Result.failure(IllegalStateException("Could not refresh schedule"))
        }

        val responseBody = body()
        if (responseBody == null && requireBody) {
            return Result.failure(IllegalStateException("Backend returned an empty body"))
        }

        @Suppress("UNCHECKED_CAST")
        return Result.success(responseBody as T)
    }

    private fun String.toPlainTextRequestBody(): RequestBody {
        return toRequestBody("text/plain".toMediaType())
    }


    fun getVisibleDays(
        startDate: LocalDate = LocalDate.now(),
        visibleDayCount: Int = DEFAULT_VISIBLE_DAY_COUNT
    ): List<LocalDate> {
        return (0 until visibleDayCount).map { offset ->
            startDate.plusDays(offset.toLong())
        }
    }

    companion object {
        private const val DEFAULT_VISIBLE_DAY_COUNT = 10
        private const val DEFAULT_ICS_MIME_TYPE = "text/calendar"
        private const val DEFAULT_ICS_FILE_NAME = "schedule.ics"
    }




}
