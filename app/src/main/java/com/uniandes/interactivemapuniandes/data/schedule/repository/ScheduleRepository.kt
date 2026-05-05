package com.uniandes.interactivemapuniandes.data.schedule.repository

import com.uniandes.interactivemapuniandes.domain.schedule.ScheduleClass
import kotlinx.coroutines.flow.Flow

interface ScheduleRepository {
    fun observeAllClasses(): Flow<List<ScheduleClass>>

    suspend fun refreshCurrentSchedule(): Result<Unit>

    suspend fun importScheduleFile(
        fileName: String,
        mimeType: String?,
        fileBytes: ByteArray
    ): Result<Unit>

    suspend fun clearLocalCache(): Result<Unit>
}
