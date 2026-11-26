package com.abdelrahman.mishkat

object ReminderTiming {
    const val INTERVAL_MILLIS = 2L * 60L * 60L * 1000L

    fun nextTriggerAt(savedTriggerAt: Long, now: Long, reset: Boolean): Long =
        if (!reset && savedTriggerAt > now) savedTriggerAt else now + INTERVAL_MILLIS
}
