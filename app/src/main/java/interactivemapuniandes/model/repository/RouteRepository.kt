package com.uniandes.interactivemapuniandes.model.repository

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.uniandes.interactivemapuniandes.model.data.NextClassResponseDto
import com.uniandes.interactivemapuniandes.model.data.RouteResponse
import com.uniandes.interactivemapuniandes.model.remote.RouteApiService
import kotlinx.coroutines.CancellationException
import java.net.SocketTimeoutException
import retrofit2.Response

class RouteRepository(
    private val api: RouteApiService,
    private val authRepository: AuthRepository
) {
    suspend fun getGraphPath(from: String, to: String): Result<RouteResponse> {
        return executePublicRequest {
            api.getGraphPath(from, to)
        }.map { body ->
            parseRouteResponse(body, fallbackFrom = from, fallbackTo = to)
        }
    }
}
