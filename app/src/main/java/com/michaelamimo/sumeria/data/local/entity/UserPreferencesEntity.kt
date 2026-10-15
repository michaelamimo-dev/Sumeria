package com.michaelamimo.sumeria.data.local.entity

import androidx.room.Entity
import androidx.room.ColumnInfo
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
    val reminderMinutesAfterMidnight: Int?,
    @ColumnInfo(defaultValue = "0") val updatedAtMillis: Long = 0L,
    @ColumnInfo(defaultValue = "1") val autoCloudSyncEnabled: Boolean = true,
    @ColumnInfo(defaultValue = "-1.0") val readerBrightness: Float = -1f,
    @ColumnInfo(defaultValue = "'[]'") val libraryShelvesJson: String = "[]",
    @ColumnInfo(defaultValue = "'{}'") val libraryMembershipJson: String = "{}",
    @ColumnInfo(defaultValue = "'DateAdded'") val librarySortOrder: String = "DateAdded",
    @ColumnInfo(defaultValue = "''") val audioVoiceName: String = ""
)

internal fun UserPreferencesEntity.toDomain() = UserPreferences(
    profileId, dailyGoalMinutes, yearlyBookGoal, readerTheme, readerFont, readerFontSize,
    readerLineSpacing, readerHorizontalMargin, readingStyle, textAlignment, appThemeMode,
    dailyRemindersEnabled, reminderMinutesAfterMidnight, updatedAtMillis, autoCloudSyncEnabled, readerBrightness,
    libraryShelvesJson, libraryMembershipJson, librarySortOrder, audioVoiceName
)
internal fun UserPreferences.toEntity() = UserPreferencesEntity(
    profileId, dailyGoalMinutes, yearlyBookGoal, readerTheme, readerFont, readerFontSize,
    readerLineSpacing, readerHorizontalMargin, readingStyle, textAlignment, appThemeMode,
    dailyRemindersEnabled, reminderMinutesAfterMidnight, updatedAtMillis, autoCloudSyncEnabled, readerBrightness,
    libraryShelvesJson, libraryMembershipJson, librarySortOrder, audioVoiceName
)
