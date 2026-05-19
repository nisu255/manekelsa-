package com.manekelsa.app.data.repository

import com.google.firebase.auth.FirebaseUser
import com.manekelsa.app.data.firebase.FirebaseService
import com.manekelsa.app.util.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository(private val firebaseService: FirebaseService) {

    fun getCurrentUser(): FirebaseUser? = firebaseService.getCurrentUser()

    fun isLoggedIn(): Boolean = firebaseService.getCurrentUser() != null

    suspend fun signInAnonymously(): Resource<FirebaseUser> = withContext(Dispatchers.IO) {
        val result = firebaseService.signInAnonymously()
        if (result.isSuccess) Resource.Success(result.getOrThrow())
        else Resource.Error(result.exceptionOrNull()?.message ?: "Sign in failed")
    }

    fun signOut() = firebaseService.signOut()
}
