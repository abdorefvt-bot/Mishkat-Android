package com.abdelrahman.mishkat

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object ReminderScheduler {
    private const val REQUEST_CODE = 4102
    private const val PREFERENCES = "mishkat_reminders"
    private const val KEY_NEXT_ALARM_AT = "next_alarm_at"

    fun scheduleNext(context: Context, reset: Boolean = false) {
        if (!ReminderContent.isReady()) return

        val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val savedTriggerAt = preferences.getLong(KEY_NEXT_ALARM_AT, 0L)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerAt = ReminderTiming.nextTriggerAt(savedTriggerAt, now, reset)

        try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    alarmManager.canScheduleExactAlarms() -> {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAt,
                        pendingIntent
                    )
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAt,
                        pendingIntent
                    )
                }
                else -> alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
            preferences.edit().putLong(KEY_NEXT_ALARM_AT, triggerAt).apply()
        } catch (_: SecurityException) {
            // If exact-alarm access changes between the check and scheduling, use the safe fallback.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent
                )
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
            preferences.edit().putLong(KEY_NEXT_ALARM_AT, triggerAt).apply()
        }
    }

    fun markAlarmDelivered(context: Context) {
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_NEXT_ALARM_AT)
            .apply()
    }

    fun cancel(context: Context) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
        markAlarmDelivered(context)
    }
}
