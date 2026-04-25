package com.uniandes.interactivemapuniandes.model.remote

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import okhttp3.Interceptor
import okhttp3.Response

// Attaches the Firebase ID token to every request so /me/* endpoints accept us.
class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val token = fetchToken() // Blocks the OkHttp thread, fine on bg
        val request = if (token != null) {
            original.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            original // No user logged in, send as-is
        }
        return chain.proceed(request)
    }

    private fun fetchToken(): String? {
        val user = FirebaseAuth.getInstance().currentUser ?: return null
        return try {
            runBlocking { user.getIdToken(false).await().token } // Use cached token
        } catch (e: Exception) {
            null // Fall through if refresh fails
        }
    }
}
