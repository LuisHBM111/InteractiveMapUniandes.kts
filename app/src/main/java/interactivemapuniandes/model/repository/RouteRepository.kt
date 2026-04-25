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

    suspend fun getRouteToNextClass(from: String): Result<RouteResponse> {
        val directResult = executeAuthorizedRequest { token ->
            api.getRouteToNextClass(token.asBearerHeader(), from)
        }.map { body ->
            parseRouteResponse(body, fallbackFrom = from)
        }

        return directResult.fold(
            onSuccess = { route ->
                if (route.path.isNotEmpty() || route.from.equals(route.to, ignoreCase = true)) {
                    Result.success(route)
                } else {
                    buildRouteFromNextClassFallback(from)
                }
            },
            onFailure = {
                buildRouteFromNextClassFallback(from)
            }
        )
    }

    suspend fun getRouteToClass(classId: String, from: String): Result<RouteResponse> {
        return executeAuthorizedRequest { token ->
            api.getRouteToClass(token.asBearerHeader(), classId, from)
        }.map { body ->
            parseRouteResponse(body, fallbackFrom = from, fallbackClassId = classId)
        }
    }

    suspend fun importDefaultSchedule(): Result<Unit> {
        return executeAuthorizedRequest { token ->
            api.importDefaultSchedule(token.asBearerHeader())
        }.map { Unit }
    }

    suspend fun getNextClass(): Result<NextClassResponseDto> {
        try {
            val token = authRepository.getIdToken(forceRefresh = false)
                ?: return Result.failure(IllegalStateException("No authenticated Firebase user"))

            val firstAttempt = api.getNextClass(token.asBearerHeader())
            if (firstAttempt.isSuccessful) {
                val body = firstAttempt.body()
                    ?: return Result.failure(IllegalStateException("Backend returned an empty body"))
                return Result.success(body)
            }

            if (firstAttempt.code() == 401) {
                val refreshedToken = authRepository.getIdToken(forceRefresh = true)
                    ?: return Result.failure(IllegalStateException("Could not refresh Firebase ID token"))

                val retryAttempt = api.getNextClass(refreshedToken.asBearerHeader())
                if (retryAttempt.isSuccessful) {
                    val body = retryAttempt.body()
                        ?: return Result.failure(IllegalStateException("Backend returned an empty body"))
                    return Result.success(body)
                }

                return Result.failure(IllegalStateException(extractErrorMessage(retryAttempt)))
            }

            return Result.failure(IllegalStateException(extractErrorMessage(firstAttempt)))
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            return Result.failure(error.toRepositoryException())
        }
    }

    private suspend fun executeAuthorizedRequest(
        request: suspend (String) -> Response<JsonObject>
    ): Result<JsonObject> {
        try {
            val token = authRepository.getIdToken(forceRefresh = false)
                ?: return Result.failure(IllegalStateException("No authenticated Firebase user"))

            val firstAttempt = request(token)
            if (firstAttempt.isSuccessful) {
                val body = firstAttempt.body()
                    ?: return Result.failure(IllegalStateException("Backend returned an empty body"))
                return Result.success(body)
            }

            if (firstAttempt.code() == 401) {
                val refreshedToken = authRepository.getIdToken(forceRefresh = true)
                    ?: return Result.failure(IllegalStateException("Could not refresh Firebase ID token"))

                val retryAttempt = request(refreshedToken)
                if (retryAttempt.isSuccessful) {
                    val body = retryAttempt.body()
                        ?: return Result.failure(IllegalStateException("Backend returned an empty body"))
                    return Result.success(body)
                }

                return Result.failure(IllegalStateException(extractErrorMessage(retryAttempt)))
            }

            return Result.failure(IllegalStateException(extractErrorMessage(firstAttempt)))
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            return Result.failure(error.toRepositoryException())
        }
    }

    private suspend fun executePublicRequest(
        request: suspend () -> Response<JsonObject>
    ): Result<JsonObject> {
        return try {
            val response = request()
            if (response.isSuccessful) {
                val body = response.body()
                    ?: return Result.failure(IllegalStateException("Backend returned an empty body"))
                Result.success(body)
            } else {
                Result.failure(IllegalStateException(extractErrorMessage(response)))
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.failure(error.toRepositoryException())
        }
    }

    private suspend fun buildRouteFromNextClassFallback(from: String): Result<RouteResponse> {
        val nextClassResult = getNextClass()
        return nextClassResult.fold(
            onSuccess = { nextClassResponse ->
                if (!nextClassResponse.hasUpcomingClass || nextClassResponse.nextClass == null) {
                    return Result.failure(IllegalStateException("No upcoming class found"))
                }

                val nextClass = nextClassResponse.nextClass
                val routeTarget = nextClass.destination?.routeTarget
                    ?: nextClass.destination?.building?.code
                    ?: nextClass.room?.building?.code
                    ?: return Result.failure(
                        IllegalStateException("The next class does not have a routable destination")
                    )

                getGraphPath(from, routeTarget).map { graphRoute ->
                    graphRoute.copy(
                        classId = nextClass.id,
                        classTitle = nextClass.title
                    )
                }
            },
            onFailure = { error ->
                Result.failure(error)
            }
        )
    }

    private fun extractErrorMessage(response: Response<*>): String {
        val rawError = runCatching { response.errorBody()?.string() }.getOrNull()
        if (!rawError.isNullOrBlank()) {
            return "Backend ${response.code()}: $rawError"
        }
        return "Backend ${response.code()} error"
    }

    private fun parseRouteResponse(
        root: JsonObject,
        fallbackFrom: String,
        fallbackTo: String? = null,
        fallbackClassId: String? = null
    ): RouteResponse {
        val routeObject = root.getObject("route") ?: root
        val nextClassObject = root.getObject("nextClass") ?: root.getObject("class")
        val destinationObject = routeObject.getObject("destination")
            ?: nextClassObject?.getObject("destination")
        val destinationBuildingObject = destinationObject?.getObject("building")
        val destinationRoomObject = destinationObject?.getObject("room")

        val from = routeObject.getString(
            "from",
            "origin",
            "fromNode",
            "fromNodeCode"
        ) ?: fallbackFrom

        val path = routeObject.getStringList("path", "route", "nodes", "nodePath")
            ?: routeObject.getStringListFromObjects(
                arrayKey = "path",
                fieldCandidates = listOf("label", "name", "code", "node", "nodeCode")
            )
            ?: routeObject.getStringListFromObjects(
                arrayKey = "steps",
                fieldCandidates = listOf("node", "nodeCode", "label", "name")
            )
            ?: emptyList()

        val pathLatitudes = routeObject.getDoubleListFromObjects("path", "latitude", "lat")
        val pathLongitudes = routeObject.getDoubleListFromObjects("path", "longitude", "lng", "lon")

        val destination = routeObject.getString(
            "to",
            "destination",
            "destinationNode",
            "toNode",
            "destinationLabel"
        ) ?: destinationObject?.getString(
            "routeTarget"
        ) ?: destinationRoomObject?.getString(
            "roomCode",
            "name"
        ) ?: destinationBuildingObject?.getString(
            "code",
            "name"
        ) ?: nextClassObject?.getString(
            "roomCode",
            "roomName",
            "title",
            "courseName",
            "buildingCode"
        ) ?: fallbackTo ?: "Next class"

        val classId = nextClassObject?.getString("id", "classId") ?: fallbackClassId
        val classTitle = nextClassObject?.getString("title", "courseName", "name")

        val totalTime = routeObject.getInt(
            "totalTime",
            "total_time",
            "totalTimeSeconds",
            "estimatedDurationSeconds",
            "durationSeconds",
            "travelTimeSeconds",
            "weight"
        ) ?: routeObject.getDouble("totalTimeMinutes")?.times(60)?.toInt() ?: 0

        return RouteResponse(
            from = from,
            to = destination,
            path = path,
            totalTime = totalTime,
            classId = classId,
            classTitle = classTitle,
            pathLatitudes = pathLatitudes?.toDoubleArray(),
            pathLongitudes = pathLongitudes?.toDoubleArray()
        )
    }

    private fun String.asBearerHeader(): String = "Bearer $this"

    private fun JsonObject.getObject(key: String): JsonObject? {
        val element = get(key)
        return if (element != null && element.isJsonObject) element.asJsonObject else null
    }

    private fun JsonObject.getString(vararg candidates: String): String? {
        for (candidate in candidates) {
            val element = get(candidate)
            if (element != null && !element.isJsonNull && element.isJsonPrimitive) {
                return element.asString
            }
        }
        return null
    }

    private fun JsonObject.getInt(vararg candidates: String): Int? {
        for (candidate in candidates) {
            val element = get(candidate)
            if (element != null && !element.isJsonNull && element.isJsonPrimitive) {
                val asString = runCatching { element.asString }.getOrNull()
                val asInt = asString?.toIntOrNull()
                if (asInt != null) {
                    return asInt
                }
            }
        }
        return null
    }

    private fun JsonObject.getDouble(vararg candidates: String): Double? {
        for (candidate in candidates) {
            val element = get(candidate)
            if (element != null && !element.isJsonNull && element.isJsonPrimitive) {
                val asString = runCatching { element.asString }.getOrNull()
                val asDouble = asString?.toDoubleOrNull()
                if (asDouble != null) {
                    return asDouble
                }
            }
        }
        return null
    }

    private fun JsonObject.getStringList(vararg candidates: String): List<String>? {
        for (candidate in candidates) {
            val element = get(candidate)
            if (element != null && element.isJsonArray) {
                val values = element.asJsonArray.toStringList()
                if (values.isNotEmpty()) {
                    return values
                }
            }
        }
        return null
    }

    private fun JsonObject.getStringListFromObjects(
        arrayKey: String,
        fieldCandidates: List<String>
    ): List<String>? {
        val arrayElement = get(arrayKey)
        if (arrayElement == null || !arrayElement.isJsonArray) {
            return null
        }

        val values = arrayElement.asJsonArray.mapNotNull { item ->
            if (!item.isJsonObject) {
                return@mapNotNull null
            }

            fieldCandidates.firstNotNullOfOrNull { field ->
                val fieldElement = item.asJsonObject.get(field)
                if (fieldElement != null && !fieldElement.isJsonNull && fieldElement.isJsonPrimitive) {
                    fieldElement.asString
                } else {
                    null
                }
            }
        }

        return values.ifEmpty { null }
    }

    private fun JsonObject.getDoubleListFromObjects(
        arrayKey: String,
        vararg fieldCandidates: String
    ): List<Double>? {
        val arrayElement = get(arrayKey)
        if (arrayElement == null || !arrayElement.isJsonArray) {
            return null
        }

        val values = arrayElement.asJsonArray.mapNotNull { item ->
            if (!item.isJsonObject) {
                return@mapNotNull null
            }

            fieldCandidates.firstNotNullOfOrNull { field ->
                val fieldElement = item.asJsonObject.get(field)
                if (fieldElement != null && !fieldElement.isJsonNull && fieldElement.isJsonPrimitive) {
                    runCatching { fieldElement.asDouble }.getOrNull()
                } else {
                    null
                }
            }
        }

        return values.ifEmpty { null }
    }

    private fun JsonArray.toStringList(): List<String> {
        return mapNotNull { item ->
            if (item is JsonElement && item.isJsonPrimitive) {
                item.asString
            } else {
                null
            }
        }
    }

    private fun Exception.toRepositoryException(): IllegalStateException {
        val userMessage = when (this) {
            is SocketTimeoutException -> "The backend took too long to respond. Please try again."
            else -> message ?: "Unexpected network error"
        }

        return IllegalStateException(userMessage, this)
    }
}
