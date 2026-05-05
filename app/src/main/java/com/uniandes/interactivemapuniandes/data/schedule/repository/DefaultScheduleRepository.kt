package com.uniandes.interactivemapuniandes.data.schedule.repository

import com.uniandes.interactivemapuniandes.data.auth.repository.AuthRepository
import com.uniandes.interactivemapuniandes.data.schedule.local.ScheduleDAO
import com.uniandes.interactivemapuniandes.data.schedule.dto.ScheduleDTO
import com.uniandes.interactivemapuniandes.data.schedule.mapper.toEntity
import com.uniandes.interactivemapuniandes.data.schedule.mapper.toDomain
import com.uniandes.interactivemapuniandes.data.schedule.remote.ScheduleApiService
import com.uniandes.interactivemapuniandes.domain.schedule.ScheduleClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response

class DefaultScheduleRepository (
    private val api: ScheduleApiService,
    private val authRepository: AuthRepository,
    private val scheduleDao: ScheduleDAO,
) : ScheduleRepository {

    override suspend fun clearLocalCache(): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            scheduleDao.clearScheduleCache()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeAllClasses(): Flow<List<ScheduleClass>> {
        return scheduleDao.observeAllClasses()
            .map { entities -> entities.map { entity -> entity.toDomain() } }
    }

    override suspend fun refreshCurrentSchedule(): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext executeAuthorizedRequest { authorization ->
            api.getCurrentSchedule(authorization)
        }.map { scheduleDto ->
            saveSchedule(scheduleDto)
        }
    }

    override suspend fun importScheduleFile(
        fileName: String,
        mimeType: String?,
        fileBytes: ByteArray,
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
                name = DEFAULT_SCHEDULE_NAME.toPlainTextRequestBody(),
                timezone = DEFAULT_TIMEZONE.toPlainTextRequestBody(),
                replaceExisting = DEFAULT_REPLACE_EXISTING.toString().toPlainTextRequestBody()
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
    companion object {
        private const val DEFAULT_ICS_MIME_TYPE = "text/calendar"
        private const val DEFAULT_ICS_FILE_NAME = "schedule.ics"
        private const val DEFAULT_SCHEDULE_NAME = "Imported schedule"
        private const val DEFAULT_TIMEZONE = "America/Bogota"
        private const val DEFAULT_REPLACE_EXISTING = true
    }




}
