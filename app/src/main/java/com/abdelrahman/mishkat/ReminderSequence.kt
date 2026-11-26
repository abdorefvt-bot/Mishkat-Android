package com.abdelrahman.mishkat

/**
 * Deterministic alternating sequence:
 * Quran passage 0, dhikr 0, Quran passage 1, dhikr 1, ...
 */
data class ReminderItem(val isQuran: Boolean, val index: Int)

object ReminderSequence {
    fun itemAt(sequence: Long, quranCount: Int, dhikrCount: Int): ReminderItem? {
        if (sequence < 0L || quranCount <= 0 || dhikrCount <= 0) return null
        val index = (sequence / 2L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        return if (sequence % 2L == 0L) {
            ReminderItem(isQuran = true, index = index % quranCount)
        } else {
            ReminderItem(isQuran = false, index = index % dhikrCount)
        }
    }
}
