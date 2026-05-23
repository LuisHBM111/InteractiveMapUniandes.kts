package com.uniandes.interactivemapuniandes.model.repository

import android.util.Log
import com.uniandes.interactivemapuniandes.model.data.RouteResponse
import interactivemapuniandes.model.data.dtos.NextClassDTO
import interactivemapuniandes.model.data.dtos.NextClassInfoDTO
import interactivemapuniandes.model.data.dtos.PreviousClassDTO
import interactivemapuniandes.model.data.dtos.SearchClassDTO
import interactivemapuniandes.model.remote.ApiService

class RouteRepository(
    private val authRepository: AuthRepository,
    private val apiService: ApiService,
) {

    suspend fun getToNextClass(): Result<NextClassDTO?> {
        Log.d("RouteDebug", "Repo getToNextClass start")

        val token = authRepository.getIdToken(forceRefresh = false) ?: return Result.failure(
            IllegalStateException("No authenticated Firebase user")
        )
        Log.d("RouteDebug", "Repo getToNextClass token received length=${token.length}")

        if (token.isEmpty()) {
            return Result.failure(IllegalStateException("No authenticated Firebase user"))
        }

        val header = "Bearer $token"

        val previousClass = getPreviousClass()
        Log.d("RouteDebug", "Repo getToNextClass previous success=${previousClass.isSuccess}")

        val from = previousClass.getOrNull()?.previousClass?.destination?.building?.code ?: "ML"
        Log.d("RouteDebug", "Repo getToNextClass request from=$from")

        val call = apiService.getToNextClass(header, from)
        Log.d("RouteDebug", "Repo getToNextClass response code=${call.code()} successful=${call.isSuccessful}")

        if (call.isSuccessful){
            val body = call.body()
            Log.d("RouteDebug", "Repo getToNextClass body hasBody=${body != null} pathNodes=${body?.path?.path?.path?.size}")
            return Result.success(body)
        }else{
            val error = call.errorBody()
            val errorText = error?.string()
            Log.e("RouteDebug", "Repo getToNextClass error code=${call.code()} body=$errorText")
            return Result.failure(IllegalStateException(errorText))
        }

    }

    suspend fun getPreviousClass(): Result<PreviousClassDTO?> {
        Log.d("RouteDebug", "Repo getPreviousClass start")
        val token = authRepository.getIdToken(forceRefresh = false) ?: return Result.failure(
            IllegalStateException("No authenticated Firebase user")
        )
        Log.d("RouteDebug", "Repo getPreviousClass token received length=${token.length}")
        val header = "Bearer $token"
        val call = apiService.getPreviousClass(header)
        Log.d("RouteDebug", "Repo getPreviousClass response code=${call.code()} successful=${call.isSuccessful}")

        if (call.isSuccessful) {
            val body = call.body()
            Log.d("RouteDebug", "Repo getPreviousClass body hasBody=${body != null} hasPrevious=${body?.hasPreviousClass} building=${body?.previousClass?.destination?.building?.code}")
            return Result.success(body)
        }
        else {
            val error = call.errorBody()
            val errorText = error?.string()
            Log.e("RouteDebug", "Repo getPreviousClass error code=${call.code()} body=$errorText")
            return Result.failure(IllegalStateException(errorText))
        }

    }

    suspend fun getSearchClass(from: String, to: String): Result<SearchClassDTO?> {
        Log.d("RouteDebug", "Repo getSearchClass start rawFrom=$from rawTo=$to")
        val safeFrom = from.ifBlank { "" }
        val safeTo = to.ifBlank { "" }
        Log.d("RouteDebug", "Repo getSearchClass request from=$safeFrom to=$safeTo")
        val call = apiService.getSearchClass(safeFrom, safeTo)
        Log.d("RouteDebug", "Repo getSearchClass response code=${call.code()} successful=${call.isSuccessful}")
        if (call.isSuccessful){
            val body = call.body()
            Log.d("RouteDebug", "Repo getSearchClass body hasBody=${body != null} pathNodes=${body?.path?.size}")
            return Result.success(body)
        }
        else{
            val error = call.errorBody()
            val errorText = error?.string()
            Log.e("RouteDebug", "Repo getSearchClass error code=${call.code()} body=$errorText")
            return Result.failure(IllegalStateException(errorText))
        }
    }

    suspend fun getGraphPath(from: String, to: String): Result<RouteResponse> {
        return getSearchClass(from, to).mapCatching { dto ->
            dto?.toRouteResponse() ?: throw IllegalStateException("Route not found")
        }
    }

    suspend fun getRouteToNextClass(from: String): Result<RouteResponse> {
        val token = authRepository.getIdToken(forceRefresh = false)
            ?: return Result.failure(IllegalStateException("No authenticated Firebase user"))
        val call = apiService.getToNextClass("Bearer $token", from)

        return if (call.isSuccessful) {
            val body = call.body()
            if (body != null) {
                Result.success(body.toRouteResponse())
            } else {
                Result.failure(IllegalStateException("Route not found"))
            }
        } else {
            val errorText = call.errorBody()?.string()
            Result.failure(IllegalStateException(errorText ?: "Could not load route"))
        }
    }

    suspend fun getRouteToClass(classId: String, from: String): Result<RouteResponse> {
        val token = authRepository.getIdToken(forceRefresh = false)
            ?: return Result.failure(IllegalStateException("No authenticated Firebase user"))
        val call = apiService.getToClass("Bearer $token", classId, from)

        return if (call.isSuccessful) {
            val body = call.body()
            if (body != null) {
                Result.success(body.toRouteResponse())
            } else {
                Result.failure(IllegalStateException("Route not found"))
            }
        } else {
            val errorText = call.errorBody()?.string()
            Result.failure(IllegalStateException(errorText ?: "Could not load route"))
        }
    }

    suspend fun getNextClass(): Result<NextClassInfoDTO?> {
        Log.d("RouteDebug", "Repo getNextClass start")
        val token = authRepository.getIdToken(forceRefresh = false) ?: return Result.failure(
            IllegalStateException("No authenticated Firebase user")
        )
        Log.d("RouteDebug", "Repo getNextClass token received length=${token.length}")
        val header = "Bearer $token"
        val call = apiService.getNextClass(header)
        Log.d("RouteDebug", "Repo getNextClass response code=${call.code()} successful=${call.isSuccessful}")

        if (call.isSuccessful) {
            val body = call.body()
            Log.d("RouteDebug", "Repo getNextClass body hasBody=${body != null} hasUpcoming=${body?.hasUpcomingClass} building=${body?.nextClass?.destination?.building?.code}")
            return Result.success(body)
        }
        else {
            val error = call.errorBody()
            val errorText = error?.string()
            Log.e("RouteDebug", "Repo getNextClass error code=${call.code()} body=$errorText")
            return Result.failure(IllegalStateException(errorText))
        }

    }

    suspend fun getToPreviousClass(): Result<PreviousClassDTO?> {
        Log.d("RouteDebug", "Repo getToPreviousClass start")

        val token = authRepository.getIdToken(forceRefresh = false) ?: return Result.failure(
            IllegalStateException("No authenticated Firebase user")
        )
        Log.d("RouteDebug", "Repo getToPreviousClass token received length=${token.length}")

        if (token.isEmpty()) {
            return Result.failure(IllegalStateException("No authenticated Firebase user"))
        }

        val header = "Bearer $token"

        val nextClass = getNextClass()
        Log.d("RouteDebug", "Repo getToPreviousClass next success=${nextClass.isSuccess}")

        val from = nextClass.getOrNull()?.nextClass?.destination?.building?.code ?: "ML"
        Log.d("RouteDebug", "Repo getToPreviousClass request from=$from")

        val call = apiService.getToPreviousClass(header, from)
        Log.d("RouteDebug", "Repo getToPreviousClass response code=${call.code()} successful=${call.isSuccessful}")

        if (call.isSuccessful){
            val body = call.body()
            Log.d("RouteDebug", "Repo getToPreviousClass body hasBody=${body != null} hasPrevious=${body?.hasPreviousClass} pathNodes=${body?.path?.path?.path?.size}")
            return Result.success(body)
        }else{
            val error = call.errorBody()
            val errorText = error?.string()
            Log.e("RouteDebug", "Repo getToPreviousClass error code=${call.code()} body=$errorText")
            return Result.failure(IllegalStateException(errorText))
        }

    }

    private fun SearchClassDTO.toRouteResponse(): RouteResponse {
        return RouteResponse(
            from = from,
            to = to,
            path = path.map { it.label },
            totalTime = totalTimeSeconds,
            pathLatitudes = path.map { it.latitude ?: Double.NaN }.toDoubleArray(),
            pathLongitudes = path.map { it.longitude ?: Double.NaN }.toDoubleArray()
        )
    }

    private fun NextClassDTO.toRouteResponse(): RouteResponse {
        val nodes = path?.path?.path.orEmpty()
        val routePath = path?.path
        return RouteResponse(
            from = routePath?.from ?: nodes.firstOrNull()?.label ?: "ML",
            to = routePath?.to ?: nodes.lastOrNull()?.label ?: nextClass?.title ?: "Destination",
            path = nodes.map { it.label },
            totalTime = routePath?.totalTimeSeconds ?: 0,
            classId = nextClass?.id,
            classTitle = nextClass?.title,
            pathLatitudes = nodes.map { it.latitude ?: Double.NaN }.toDoubleArray(),
            pathLongitudes = nodes.map { it.longitude ?: Double.NaN }.toDoubleArray()
        )
    }


}
