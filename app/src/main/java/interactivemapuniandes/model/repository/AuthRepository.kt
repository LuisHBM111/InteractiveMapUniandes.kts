package com.uniandes.interactivemapuniandes.model.repository

import com.google.firebase.auth.FirebaseAuth

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
}
