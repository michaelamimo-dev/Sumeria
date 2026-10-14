package com.michaelamimo.sumeria.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.michaelamimo.sumeria.domain.model.UserPreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CurrentProfileProvider @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) {
    fun currentProfileId(): String = firebaseAuth.currentUser?.uid ?: UserPreferences.LOCAL_PROFILE_ID
}
