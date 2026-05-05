package com.uniandes.interactivemapuniandes.domain.routing

import com.uniandes.interactivemapuniandes.data.routing.repository.RouteRepository

class GetGraphRouteUseCase(
    private val routeRepository: RouteRepository
) {
    suspend operator fun invoke(from: String, to: String): Result<Route> {
        return routeRepository.getGraphPath(from, to)
    }
}

class GetNextClassRouteUseCase(
    private val routeRepository: RouteRepository
) {
    suspend operator fun invoke(from: String): Result<Route> {
        return routeRepository.getRouteToNextClass(from)
    }
}

class GetRouteToClassUseCase(
    private val routeRepository: RouteRepository
) {
    suspend operator fun invoke(classId: String, from: String): Result<Route> {
        return routeRepository.getRouteToClass(classId, from)
    }
}

data class RouteUseCases(
    val getGraphRoute: GetGraphRouteUseCase,
    val getNextClassRoute: GetNextClassRouteUseCase,
    val getRouteToClass: GetRouteToClassUseCase
)
