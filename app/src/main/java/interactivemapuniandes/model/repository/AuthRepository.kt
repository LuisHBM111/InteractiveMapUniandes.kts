package com.uniandes.interactivemapuniandes.model.repository

import com.google.firebase.auth.FirebaseAuth
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

class AuthRepository(private val auth: FirebaseAuth) {

    fun login(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, null)
                } else {
                    onResult(false, task.exception?.localizedMessage ?: "Login failed")
                }
            }
    }

    fun sendPasswordResetEmail(email: String, onResult: (Boolean, String?) -> Unit) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, null)
                } else {
                    onResult(false, task.exception?.localizedMessage ?: "Password reset email failed")
                }
            }

    }

    suspend fun getIdToken(forceRefresh: Boolean = false): String? =
        suspendCancellableCoroutine { continuation ->
            val currentUser = auth.currentUser

            if (currentUser == null) {
                continuation.resume(null)
                return@suspendCancellableCoroutine
            }

            currentUser.getIdToken(forceRefresh)
                .addOnSuccessListener { result ->
                    continuation.resume(result.token)
                }
                .addOnFailureListener { error ->
                    continuation.resumeWithException(error)
                }
        }
}
