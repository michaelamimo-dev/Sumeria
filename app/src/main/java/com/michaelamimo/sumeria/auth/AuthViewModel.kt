package com.michaelamimo.sumeria.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthState {
    data object Idle : AuthState()
    data object Loading : AuthState()
    data object Authenticated : AuthState()
    data object PasswordResetSent : AuthState()
    data object SignedOut : AuthState()
    data class AccountActionCompleted(val message: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _user = MutableStateFlow(authRepository.currentUser)
    val user: StateFlow<AuthenticatedUser?> = _user.asStateFlow()

    val isUserLoggedIn: Boolean
        get() = authRepository.currentUser != null

    init {
        viewModelScope.launch {
            authRepository.observeUser().collect { current -> _user.value = current }
        }
    }

    fun login(email: String, pass: String) = performAuth {
        if (email.isBlank() || pass.isBlank()) error("Enter your email and password.")
        authRepository.signIn(email, pass)
    }

    fun signUp(email: String, pass: String) = performAuth {
        if (email.isBlank() || pass.isBlank()) error("Enter your email and password.")
        authRepository.signUp(email, pass)
    }

    fun sendPasswordReset(email: String) {
        if (email.isBlank()) {
            _authState.value = AuthState.Error("Enter your email address first.")
            return
        }
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                authRepository.sendPasswordReset(email)
                _authState.value = AuthState.PasswordResetSent
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _authState.value = AuthState.Error(error.localizedMessage ?: "Password reset could not be sent.")
            }
        }
    }

    fun signOut() {
        authRepository.signOut()
        _authState.value = AuthState.SignedOut
    }

    fun providerNotConfigured(provider: String) {
        _authState.value = AuthState.Error("$provider sign-in has not been configured for this Sumeria Firebase project yet.")
    }

    fun updateDisplayName(name: String) = performAccountAction("Profile updated") {
        require(name.isNotBlank()) { "Enter a display name." }
        authRepository.updateDisplayName(name)
    }

    fun updateEmail(email: String, currentPassword: String) = performAccountAction("Email updated") {
        require(email.isNotBlank()) { "Enter your new email address." }
        require(currentPassword.isNotBlank()) { "Enter your current password to confirm this change." }
        authRepository.updateEmail(email, currentPassword)
    }

    fun updatePassword(currentPassword: String, newPassword: String) = performAccountAction("Password updated") {
        require(currentPassword.isNotBlank()) { "Enter your current password." }
        require(newPassword.length >= 8) { "Use at least 8 characters for your new password." }
        authRepository.updatePassword(currentPassword, newPassword)
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }

    private fun performAuth(action: suspend () -> AuthenticatedUser) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                action()
                _authState.value = AuthState.Authenticated
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _authState.value = AuthState.Error(error.localizedMessage ?: "Authentication failed.")
            }
        }
    }

    private fun performAccountAction(successMessage: String, action: suspend () -> Unit) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                action()
                _user.value = authRepository.currentUser
                _authState.value = AuthState.AccountActionCompleted(successMessage)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _authState.value = AuthState.Error(error.localizedMessage ?: "The account change could not be completed.")
            }
        }
    }
}
