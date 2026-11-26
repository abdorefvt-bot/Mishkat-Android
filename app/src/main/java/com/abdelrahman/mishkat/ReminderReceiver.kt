package com.abdelrahman.mishkat

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        ReminderScheduler.markAlarmDelivered(context)
        if (!ReminderContent.isReady()) return

        val preferences = context.getSharedPreferences("mishkat_reminders", Context.MODE_PRIVATE)
        val sequence = preferences.getLong("sequence", 0L)
        val item = ReminderSequence.itemAt(
            sequence,
            ReminderContent.quranPassages.size,
            ReminderContent.adhkar.size
        ) ?: return
        val isVerse = item.isQuran
        val text = if (isVerse) {
            ReminderContent.quranPassages[item.index]
        } else {
            ReminderContent.adhkar[item.index]
        }

        createChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(if (isVerse) "آية قرآنية" else "ذِكر")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            preferences.edit().putLong("sequence", sequence + 1L).apply()
        } catch (_: SecurityException) {
            // Android 13+ notification permission may be denied by the user.
        }

        ReminderScheduler.scheduleNext(context)
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Mishkat reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "Quran passages and dhikr reminders" }
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "mishkat_reminders"
        private const val NOTIFICATION_ID = 4201
    }
}
