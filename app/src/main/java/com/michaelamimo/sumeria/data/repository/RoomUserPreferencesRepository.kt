package com.michaelamimo.sumeria.data.repository

import com.michaelamimo.sumeria.data.local.dao.UserPreferencesDao
import com.michaelamimo.sumeria.data.auth.CurrentProfileProvider
import com.michaelamimo.sumeria.data.local.entity.toDomain
import com.michaelamimo.sumeria.data.local.entity.toEntity
import com.michaelamimo.sumeria.domain.model.UserPreferences
import com.michaelamimo.sumeria.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomUserPreferencesRepository @Inject constructor(
    private val dao: UserPreferencesDao,
    private val profileProvider: CurrentProfileProvider
) : UserPreferencesRepository {
    override fun observe(profileId: String): Flow<UserPreferences> {
        val resolved = resolveProfile(profileId)
        return dao.observe(resolved).map { it?.toDomain() ?: UserPreferences(profileId = resolved) }
    }
    override suspend fun get(profileId: String): UserPreferences {
        val resolved = resolveProfile(profileId)
        return dao.get(resolved)?.toDomain() ?: UserPreferences(profileId = resolved)
    }
    override suspend fun save(preferences: UserPreferences) {
        val profileId = resolveProfile(preferences.profileId)
        dao.upsert(preferences.copy(profileId = profileId, updatedAtMillis = System.currentTimeMillis()).toEntity())
    }

    private fun resolveProfile(requested: String): String =
        if (requested == UserPreferences.LOCAL_PROFILE_ID) profileProvider.currentProfileId() else requested
}
