package com.uniandes.interactivemapuniandes.model.remote

import com.uniandes.interactivemapuniandes.model.data.Building
import com.uniandes.interactivemapuniandes.model.data.Room
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface PlacesApiService {
    @GET("api/v1/places/buildings")
    suspend fun listBuildings(@Query("search") search: String? = null): Response<List<Building>>

    @GET("api/v1/places/buildings/{id}")
    suspend fun getBuilding(@Path("id") id: String): Response<Building>

    @GET("api/v1/places/buildings/{id}/rooms")
    suspend fun listRoomsInBuilding(@Path("id") id: String): Response<List<Room>>

    @GET("api/v1/places/rooms")
    suspend fun listRooms(@Query("search") search: String? = null): Response<List<Room>>
}
