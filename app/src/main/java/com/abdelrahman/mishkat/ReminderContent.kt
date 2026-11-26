package com.abdelrahman.mishkat

/**
 * Canonical reminder text data.
 *
 * The exact user-approved Quran passages and adhkar must be inserted verbatim here.
 * Deliberately left empty until the original text list can be recovered and verified;
 * do not substitute paraphrases, reconstructed verses, or guessed wording.
 */
object ReminderContent {
    val quranPassages: List<String> = emptyList()
    val adhkar: List<String> = emptyList()

    fun isReady(): Boolean = quranPassages.size == 40 && adhkar.size == 10
}
