package com.michaelamimo.sumeria.domain.repository

import com.michaelamimo.sumeria.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    fun observe(profileId: String = UserPreferences.LOCAL_PROFILE_ID): Flow<UserPreferences>
    suspend fun get(profileId: String = UserPreferences.LOCAL_PROFILE_ID): UserPreferences
    suspend fun save(preferences: UserPreferences)
}
