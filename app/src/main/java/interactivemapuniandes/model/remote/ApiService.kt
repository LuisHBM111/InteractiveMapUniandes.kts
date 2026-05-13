package interactivemapuniandes.model.remote

import interactivemapuniandes.model.data.dtos.NextClassDTO
import interactivemapuniandes.model.data.dtos.PreviousClassDTO
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface ApiService {

    @GET("api/v1/me/routes/to-next-class")
    suspend fun getNextClass(
        @Header("Authorization") authorization: String, @Query("from") from: String
    ): Response<NextClassDTO>

    @GET("api/v1/me/classes/previous")
    suspend fun getPreviousClass(
        @Header("Authorization") authorization: String
    ): Response<PreviousClassDTO>

}