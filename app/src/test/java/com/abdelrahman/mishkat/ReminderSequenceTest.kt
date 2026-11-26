package com.abdelrahman.mishkat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderSequenceTest {
    @Test
    fun startsWithDhikrThenAlternatesWithQuran() {
        assertEquals(ReminderItem(false, 0), ReminderSequence.itemAt(0, 40, 10))
        assertEquals(ReminderItem(true, 0), ReminderSequence.itemAt(1, 40, 10))
        assertEquals(ReminderItem(false, 1), ReminderSequence.itemAt(2, 40, 10))
        assertEquals(ReminderItem(true, 1), ReminderSequence.itemAt(3, 40, 10))
    }

    @Test
    fun wrapsEachListWithoutBreakingAlternation() {
        assertEquals(ReminderItem(false, 0), ReminderSequence.itemAt(20, 40, 10))
        assertEquals(ReminderItem(true, 10), ReminderSequence.itemAt(21, 40, 10))
    }

    @Test
    fun rejectsInvalidInput() {
        assertNull(ReminderSequence.itemAt(-1, 40, 10))
        assertNull(ReminderSequence.itemAt(0, 0, 10))
        assertNull(ReminderSequence.itemAt(0, 40, 0))
    }

    @Test
    fun exactUserSuppliedListsAreCompleteAndKeepIntentionalDuplicate() {
        assertEquals(40, ReminderContent.quranPassages.size)
        assertEquals(10, ReminderContent.adhkar.size)
        assertEquals(ReminderContent.adhkar[0], ReminderContent.adhkar[5])
        assertFalse(ReminderContent.quranPassages.any { it.contains("فَاتَّقُوا اللَّهَ مَا اسْتَطَعْتُمْ") })
        assertTrue(ReminderContent.isReady())
    }

    @Test
    fun reminderIntervalIsExactlyOneHour() {
        assertEquals(3_600_000L, ReminderTiming.INTERVAL_MILLIS)
    }

    @Test
    fun reopeningAppPreservesTheExistingReminderDeadline() {
        assertEquals(9_000L, ReminderTiming.nextTriggerAt(9_000L, 5_000L, false))
    }

    @Test
    fun expiredOrExplicitlyResetReminderGetsRescheduledOneHourLater() {
        assertEquals(3_605_000L, ReminderTiming.nextTriggerAt(4_000L, 5_000L, false))
        assertEquals(3_605_000L, ReminderTiming.nextTriggerAt(9_000L, 5_000L, true))
    }
}
