package com.michaelamimo.sumeria.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.michaelamimo.sumeria.auth.AuthRepository
import com.michaelamimo.sumeria.auth.AuthenticatedUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : AuthRepository {
    override val currentUser: AuthenticatedUser?
        get() = firebaseAuth.currentUser?.toDomain()

    override fun observeUser(): Flow<AuthenticatedUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser?.toDomain())
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    override suspend fun signIn(email: String, password: String): AuthenticatedUser {
        val result = firebaseAuth.signInWithEmailAndPassword(email.trim(), password).await()
        return result.user?.toDomain() ?: error("Firebase did not return an account.")
    }

    override suspend fun signUp(email: String, password: String): AuthenticatedUser {
        val result = firebaseAuth.createUserWithEmailAndPassword(email.trim(), password).await()
        return result.user?.toDomain() ?: error("Firebase did not return an account.")
    }

    override suspend fun sendPasswordReset(email: String) {
        firebaseAuth.sendPasswordResetEmail(email.trim()).await()
    }

    override suspend fun updateDisplayName(name: String) {
        val user = firebaseAuth.currentUser ?: error("Sign in before editing your profile.")
        user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name.trim()).build()).await()
    }

    override suspend fun updateEmail(newEmail: String, currentPassword: String) {
        val user = firebaseAuth.currentUser ?: error("Sign in before changing your email.")
        val email = user.email ?: error("This account does not have an email address to reauthenticate with.")
        user.reauthenticate(EmailAuthProvider.getCredential(email, currentPassword)).await()
        user.updateEmail(newEmail.trim()).await()
    }

    override suspend fun updatePassword(currentPassword: String, newPassword: String) {
        val user = firebaseAuth.currentUser ?: error("Sign in before changing your password.")
        val email = user.email ?: error("This account does not have an email address to reauthenticate with.")
        user.reauthenticate(EmailAuthProvider.getCredential(email, currentPassword)).await()
        user.updatePassword(newPassword).await()
    }

    override fun signOut() = firebaseAuth.signOut()

    private fun FirebaseUser.toDomain() = AuthenticatedUser(uid, email, displayName)
}
