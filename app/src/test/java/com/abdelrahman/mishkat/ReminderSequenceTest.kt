package com.abdelrahman.mishkat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReminderSequenceTest {
    @Test
    fun alternatesPassageAndDhikrInOrder() {
        assertEquals(ReminderItem(true, 0), ReminderSequence.itemAt(0, 40, 10))
        assertEquals(ReminderItem(false, 0), ReminderSequence.itemAt(1, 40, 10))
        assertEquals(ReminderItem(true, 1), ReminderSequence.itemAt(2, 40, 10))
        assertEquals(ReminderItem(false, 1), ReminderSequence.itemAt(3, 40, 10))
    }

    @Test
    fun wrapsEachListWithoutBreakingAlternation() {
        assertEquals(ReminderItem(true, 0), ReminderSequence.itemAt(80, 40, 10))
        assertEquals(ReminderItem(false, 0), ReminderSequence.itemAt(81, 40, 10))
    }

    @Test
    fun rejectsInvalidInput() {
        assertNull(ReminderSequence.itemAt(-1, 40, 10))
        assertNull(ReminderSequence.itemAt(0, 0, 10))
        assertNull(ReminderSequence.itemAt(0, 40, 0))
    }
}
