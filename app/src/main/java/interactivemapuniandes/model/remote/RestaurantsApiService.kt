package com.uniandes.interactivemapuniandes.model.remote

import com.uniandes.interactivemapuniandes.model.data.CreateReviewBody
import com.uniandes.interactivemapuniandes.model.data.Restaurant
import com.uniandes.interactivemapuniandes.model.data.Review
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface RestaurantsApiService {
    @GET("api/v1/restaurants")
    suspend fun list(): Response<List<Restaurant>>

    @GET("api/v1/restaurants/{id}")
    suspend fun getOne(@Path("id") id: String): Response<Restaurant>

    @GET("api/v1/restaurants/{id}/reviews")
    suspend fun listReviews(@Path("id") id: String): Response<List<Review>>

    @POST("api/v1/restaurants/{id}/reviews")
    suspend fun createReview(
        @Path("id") id: String,
        @Body body: CreateReviewBody
    ): Response<Review>
}
