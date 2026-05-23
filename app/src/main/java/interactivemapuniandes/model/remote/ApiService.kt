package interactivemapuniandes.model.remote

import interactivemapuniandes.model.data.dtos.NextClassDTO
import interactivemapuniandes.model.data.dtos.NextClassInfoDTO
import interactivemapuniandes.model.data.dtos.PreviousClassDTO
import interactivemapuniandes.model.data.dtos.SearchClassDTO
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface ApiService {

    @GET("api/v1/me/routes/to-next-class")
    suspend fun getToNextClass(
        @Header("Authorization") authorization: String, @Query("from") from: String
    ): Response<NextClassDTO>

    @GET("api/v1/me/routes/to-previous-class")
    suspend fun getToPreviousClass(
        @Header("Authorization") authorization: String, @Query("from") from: String
    ): Response<PreviousClassDTO>

    @GET("api/v1/me/classes/previous")
    suspend fun getPreviousClass(
        @Header("Authorization") authorization: String
    ): Response<PreviousClassDTO>

    @GET("api/v1/me/classes/next")
    suspend fun getNextClass(
        @Header("Authorization") authorization: String
    ): Response<NextClassInfoDTO>

    @GET("api/v1/me/routes/to-class/{classId}")
    suspend fun getToClass(
        @Header("Authorization") authorization: String,
        @retrofit2.http.Path("classId") classId: String,
        @Query("from") from: String
    ): Response<NextClassDTO>

    @GET("api/v1/routes/path")
    suspend fun getSearchClass(@Query("from") from: String, @Query("to") to: String): Response<SearchClassDTO>

}
