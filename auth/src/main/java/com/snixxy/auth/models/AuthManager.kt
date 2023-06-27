package com.snixxy.auth.models

import kotlinx.coroutines.flow.Flow

interface AuthManager {
    fun checkAuth(): Boolean

    fun resetPassword(userName: String): Flow<NetworkResult<Void>>

    fun signIn(userName: String, password: String): Flow<NetworkResult<Void>>

    fun signUp(userName: String, password: String): Flow<NetworkResult<Void>>

    fun signOut(): Flow<NetworkResult<Void>>

    fun getUserEmail(): String
}
