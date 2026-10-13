package com.michaelamimo.sumeria.data.repository

import com.michaelamimo.sumeria.data.local.dao.UserPreferencesDao
import com.michaelamimo.sumeria.data.local.entity.toDomain
import com.michaelamimo.sumeria.data.local.entity.toEntity
import com.michaelamimo.sumeria.domain.model.UserPreferences
import com.michaelamimo.sumeria.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomUserPreferencesRepository @Inject constructor(private val dao: UserPreferencesDao) : UserPreferencesRepository {
    override fun observe(profileId: String): Flow<UserPreferences> =
        dao.observe(profileId).map { it?.toDomain() ?: UserPreferences(profileId = profileId) }
    override suspend fun get(profileId: String): UserPreferences = dao.get(profileId)?.toDomain() ?: UserPreferences(profileId = profileId)
    override suspend fun save(preferences: UserPreferences) = dao.upsert(preferences.toEntity())
}
