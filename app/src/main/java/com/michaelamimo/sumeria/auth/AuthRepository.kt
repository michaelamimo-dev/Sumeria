package com.michaelamimo.sumeria.auth

import kotlinx.coroutines.flow.Flow

data class AuthenticatedUser(
    val id: String,
    val email: String?,
    val displayName: String?
)

interface AuthRepository {
    val currentUser: AuthenticatedUser?
    fun observeUser(): Flow<AuthenticatedUser?>
    suspend fun signIn(email: String, password: String): AuthenticatedUser
    suspend fun signUp(email: String, password: String): AuthenticatedUser
    suspend fun sendPasswordReset(email: String)
    suspend fun updateDisplayName(name: String)
    suspend fun updateEmail(newEmail: String, currentPassword: String)
    suspend fun updatePassword(currentPassword: String, newPassword: String)
    fun signOut()
}
