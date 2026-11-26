package com.abdelrahman.mishkat

object ReminderTiming {
    const val INTERVAL_MILLIS = 2L * 60L * 60L * 1000L

    fun shouldKeepExistingAlarm(savedTriggerAt: Long, now: Long, reset: Boolean): Boolean =
        !reset && savedTriggerAt > now
}
