package com.michaelamimo.sumeria.domain.model

/** Persisted app and reader preferences. The local profile key supports account scoping later. */
data class UserPreferences(
    val profileId: String = LOCAL_PROFILE_ID,
    val dailyGoalMinutes: Int = 15,
    val yearlyBookGoal: Int = 12,
    val readerTheme: String = "Light",
    val readerFont: String = "System",
    val readerFontSize: Int = 18,
    val readerLineSpacing: Float = 1.6f,
    val readerHorizontalMargin: Int = 24,
    val readingStyle: String = "Scroll",
    val textAlignment: String = "Justified",
    val appThemeMode: String = "System",
    val dailyRemindersEnabled: Boolean = false,
    val reminderMinutesAfterMidnight: Int? = null
) {
    companion object {
        const val LOCAL_PROFILE_ID = "local"
    }
}
