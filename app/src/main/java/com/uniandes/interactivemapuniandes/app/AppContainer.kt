package com.uniandes.interactivemapuniandes.app

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.uniandes.interactivemapuniandes.core.network.RetrofitInstance
import com.uniandes.interactivemapuniandes.data.auth.repository.AuthRepository
import com.uniandes.interactivemapuniandes.data.routing.repository.DefaultRouteRepository
import com.uniandes.interactivemapuniandes.data.schedule.local.AppDatabase
import com.uniandes.interactivemapuniandes.data.schedule.repository.DefaultScheduleRepository
import com.uniandes.interactivemapuniandes.domain.routing.GetGraphRouteUseCase
import com.uniandes.interactivemapuniandes.domain.routing.GetNextClassRouteUseCase
import com.uniandes.interactivemapuniandes.domain.routing.GetRouteToClassUseCase
import com.uniandes.interactivemapuniandes.domain.routing.RouteUseCases
import com.uniandes.interactivemapuniandes.domain.schedule.ClearScheduleCacheUseCase
import com.uniandes.interactivemapuniandes.domain.schedule.GetClassesForDayUseCase
import com.uniandes.interactivemapuniandes.domain.schedule.GetVisibleScheduleDaysUseCase
import com.uniandes.interactivemapuniandes.domain.schedule.ImportScheduleFileUseCase
import com.uniandes.interactivemapuniandes.domain.schedule.ObserveScheduleClassesUseCase
import com.uniandes.interactivemapuniandes.domain.schedule.RefreshCurrentScheduleUseCase
import com.uniandes.interactivemapuniandes.domain.schedule.ScheduleUseCases

object AppContainer {
    fun routeUseCases(): RouteUseCases {
        val routeRepository = DefaultRouteRepository(
            api = RetrofitInstance.api,
            authRepository = AuthRepository(FirebaseAuth.getInstance())
        )

        return RouteUseCases(
            getGraphRoute = GetGraphRouteUseCase(routeRepository),
            getNextClassRoute = GetNextClassRouteUseCase(routeRepository),
            getRouteToClass = GetRouteToClassUseCase(routeRepository)
        )
    }

    fun scheduleUseCases(context: Context): ScheduleUseCases {
        val database = AppDatabase.getInstance(context.applicationContext)
        val scheduleRepository = DefaultScheduleRepository(
            api = RetrofitInstance.scheduleApi,
            authRepository = AuthRepository(FirebaseAuth.getInstance()),
            scheduleDao = database.scheduleDao()
        )

        return ScheduleUseCases(
            observeClasses = ObserveScheduleClassesUseCase(scheduleRepository),
            refreshSchedule = RefreshCurrentScheduleUseCase(scheduleRepository),
            importScheduleFile = ImportScheduleFileUseCase(scheduleRepository),
            clearCache = ClearScheduleCacheUseCase(scheduleRepository),
            getVisibleDays = GetVisibleScheduleDaysUseCase(),
            getClassesForDay = GetClassesForDayUseCase()
        )
    }
}
