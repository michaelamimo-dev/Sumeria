package com.michaelamimo.sumeria.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.LocalDateTime
import java.time.ZoneId

object ReadingReminderScheduler {
    private const val PREFS = "reading_reminder"
    private const val ENABLED = "enabled"
    private const val MINUTES = "minutes"
    private const val REQUEST_CODE = 8104

    fun schedule(context: Context, enabled: Boolean, minutesAfterMidnight: Int) {
        val safeMinutes = minutesAfterMidnight.coerceIn(0, 1439)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(ENABLED, enabled)
            .putInt(MINUTES, safeMinutes)
            .apply()
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val pendingIntent = pendingIntent(context)
        if (!enabled) {
            alarmManager.cancel(pendingIntent)
            return
        }
        val now = LocalDateTime.now()
        var firstReminder = now.toLocalDate().atStartOfDay().plusMinutes(safeMinutes.toLong())
        if (!firstReminder.isAfter(now)) firstReminder = firstReminder.plusDays(1)
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            firstReminder.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    fun restoreAfterBoot(context: Context) {
        val preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        schedule(context, preferences.getBoolean(ENABLED, false), preferences.getInt(MINUTES, 20 * 60))
    }

    private fun pendingIntent(context: Context) = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, ReadingReminderReceiver::class.java).setAction(ReadingReminderReceiver.ACTION_REMIND),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

