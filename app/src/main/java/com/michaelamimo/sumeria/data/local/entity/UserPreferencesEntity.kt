package com.michaelamimo.sumeria.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.michaelamimo.sumeria.domain.model.UserPreferences

@Entity(tableName = "user_preferences")
data class UserPreferencesEntity(
    @PrimaryKey val profileId: String,
    val dailyGoalMinutes: Int,
    val yearlyBookGoal: Int,
    val readerTheme: String,
    val readerFont: String,
    val readerFontSize: Int,
    val readerLineSpacing: Float,
    val readerHorizontalMargin: Int,
    val readingStyle: String,
    val textAlignment: String,
    val appThemeMode: String,
    val dailyRemindersEnabled: Boolean,
    val reminderMinutesAfterMidnight: Int?
)

internal fun UserPreferencesEntity.toDomain() = UserPreferences(
    profileId, dailyGoalMinutes, yearlyBookGoal, readerTheme, readerFont, readerFontSize,
    readerLineSpacing, readerHorizontalMargin, readingStyle, textAlignment, appThemeMode,
    dailyRemindersEnabled, reminderMinutesAfterMidnight
)
internal fun UserPreferences.toEntity() = UserPreferencesEntity(
    profileId, dailyGoalMinutes, yearlyBookGoal, readerTheme, readerFont, readerFontSize,
    readerLineSpacing, readerHorizontalMargin, readingStyle, textAlignment, appThemeMode,
    dailyRemindersEnabled, reminderMinutesAfterMidnight
)
