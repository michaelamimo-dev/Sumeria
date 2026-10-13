package com.michaelamimo.sumeria.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.michaelamimo.sumeria.data.local.entity.UserPreferencesEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserPreferencesDao {
    @Query("SELECT * FROM user_preferences WHERE profileId = :profileId LIMIT 1") fun observe(profileId: String): Flow<UserPreferencesEntity?>
    @Query("SELECT * FROM user_preferences WHERE profileId = :profileId LIMIT 1") suspend fun get(profileId: String): UserPreferencesEntity?
    @Upsert suspend fun upsert(preferences: UserPreferencesEntity)
}
