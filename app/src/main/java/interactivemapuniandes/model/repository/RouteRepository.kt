package com.uniandes.interactivemapuniandes.model.repository

import android.util.Log
import interactivemapuniandes.model.data.dtos.NextClassDTO
import interactivemapuniandes.model.data.dtos.PreviousClassDTO
import interactivemapuniandes.model.data.dtos.SearchClassDTO
import interactivemapuniandes.model.remote.ApiService

class RouteRepository(
    private val authRepository: AuthRepository,
    private val apiService: ApiService,
) {

    suspend fun getToNextClass(): Result<NextClassDTO?> {

        val token = authRepository.getIdToken(forceRefresh = false) ?: return Result.failure(
            IllegalStateException("No authenticated Firebase user")
        )

        if (token.isEmpty()) {
            return Result.failure(IllegalStateException("No authenticated Firebase user"))
        }

        val header = "Bearer $token"

        val previousClass = getPreviousClass()

        val from = previousClass.getOrNull()?.previousClass?.destination?.building?.code ?: "ML"

        val call = apiService.getToNextClass(header, from)

        if (call.isSuccessful){
            val body = call.body()
            return Result.success(body)
        }else{
            val error = call.errorBody()
            Log.e("RouteActivity", "Error: ${call.code()}")
            return Result.failure(IllegalStateException(error?.string()))
        }

    }

    suspend fun getPreviousClass(): Result<PreviousClassDTO?> {
        val token = authRepository.getIdToken(forceRefresh = false) ?: return Result.failure(
            IllegalStateException("No authenticated Firebase user")
        )
        val header = "Bearer $token"
        val call = apiService.getPreviousClass(header)

        if (call.isSuccessful) {
            val body = call.body()
            return Result.success(body)
        }
        else {
            val error = call.errorBody()
            Log.e("RouteActivity", "Error: ${call.code()}")
            return Result.failure(IllegalStateException(error?.string()))
        }

    }

    suspend fun getSearchClass(from: String, to: String): Result<SearchClassDTO?> {
        val safeFrom = from.ifBlank { "" }
        val safeTo = to.ifBlank { "" }
        val call = apiService.getSearchClass(safeFrom, safeTo)
        if (call.isSuccessful){
            val body = call.body()
            return Result.success(body)
        }
        else{
            val error = call.errorBody()
            Log.e("RouteActivity", "Error: ${call.code()}")
            return Result.failure(IllegalStateException(error?.string()))
        }
    }

    suspend fun getNextClass(): Result<PreviousClassDTO?> {
        val token = authRepository.getIdToken(forceRefresh = false) ?: return Result.failure(
            IllegalStateException("No authenticated Firebase user")
        )
        val header = "Bearer $token"
        val call = apiService.getNextClass(header)

        if (call.isSuccessful) {
            val body = call.body()
            return Result.success(body)
        }
        else {
            val error = call.errorBody()
            Log.e("RouteActivity", "Error: ${call.code()}")
            return Result.failure(IllegalStateException(error?.string()))
        }

    }

    suspend fun getToPreviousClass(): Result<NextClassDTO?> {

        val token = authRepository.getIdToken(forceRefresh = false) ?: return Result.failure(
            IllegalStateException("No authenticated Firebase user")
        )

        if (token.isEmpty()) {
            return Result.failure(IllegalStateException("No authenticated Firebase user"))
        }

        val header = "Bearer $token"

        val nextClass = getNextClass()

        val from = nextClass.getOrNull()?.previousClass?.destination?.building?.code ?: "ML"

        val call = apiService.getToPreviousClass(header, from)

        if (call.isSuccessful){
            val body = call.body()
            return Result.success(body)
        }else{
            val error = call.errorBody()
            Log.e("RouteActivity", "Error: ${call.code()}")
            return Result.failure(IllegalStateException(error?.string()))
        }

    }


}
