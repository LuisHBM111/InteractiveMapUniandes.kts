package com.uniandes.interactivemapuniandes.model.repository

import android.util.Log
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.uniandes.interactivemapuniandes.model.data.NextClassResponseDto
import com.uniandes.interactivemapuniandes.model.data.RouteResponse
import com.uniandes.interactivemapuniandes.model.remote.RouteApiService
import interactivemapuniandes.model.data.dtos.NextClassDTO
import interactivemapuniandes.model.data.dtos.PreviousClassDTO
import interactivemapuniandes.model.remote.ApiService
import interactivemapuniandes.utils.ListsAdapter
import kotlinx.coroutines.CancellationException
import java.net.SocketTimeoutException
import retrofit2.Response

class RouteRepository(
    private val api: RouteApiService,
    private val authRepository: AuthRepository,
    private val apiService: ApiService,
) {

    suspend fun getNextClass(): Result<NextClassDTO?> {

        val token = authRepository.getIdToken(forceRefresh = false) ?: return Result.failure(
            IllegalStateException("No authenticated Firebase user")
        )

        val header = "Bearer $token"

        val previousClass = getPreviousClass()

        if (previousClass.isSuccess) {
            val previousClassDTO = previousClass.getOrNull()
            val from = previousClassDTO?.previousClass?.destination?.building?.code ?: "ML"
            if (previousClassDTO != null) {
                val call = apiService.getNextClass(header, from)
                if (call.isSuccessful) {
                    val body = call.body()
                    return Result.success(body)
                } else {
                    val error = call.errorBody()
                    Log.e("RouteActivity", "Error: ${error.toString()}")
                    Log.e("RouteActivity", "Error: ${call.code()}")
                    return Result.failure(IllegalStateException(error.toString()))
                }
            }
            else{
                val call = apiService.getNextClass(header, "ML")
                if (call.isSuccessful) {
                    val body = call.body()
                    return Result.success(body)
                }
                else{
                    val error = call.errorBody()
                    Log.e("RouteActivity", "Error: ${error.toString()}")
                    Log.e("RouteActivity", "Error: ${call.code()}")
                    return Result.failure(IllegalStateException(error.toString()))
                }
            }
        }

        return Result.failure(IllegalStateException("No authenticated Firebase user"))
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
            Log.e("RouteActivity", "Error: ${error.toString()}")
            Log.e("RouteActivity", "Error: ${call.code()}")
            return Result.failure(IllegalStateException(error.toString()))
        }

    }


}
