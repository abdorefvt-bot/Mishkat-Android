package com.abdelrahman.mishkat

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.webkit.JavascriptInterface
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

object PlaybackState {
    @Volatile
    var isPlaying: Boolean = false
}

object PlaybackWebViewHolder {
    @Volatile
    private var retained: android.webkit.WebView? = null

    @Synchronized
    fun take(): android.webkit.WebView? {
        val view = retained
        retained = null
        return view
    }

    @Synchronized
    fun retain(view: android.webkit.WebView) {
        retained = view
    }

    @Synchronized
    fun destroyRetained() {
        val view = retained ?: return
        retained = null
        (view.parent as? android.view.ViewGroup)?.removeView(view)
        view.stopLoading()
        view.destroy()
    }
}

class PlaybackBridge(context: Context) {
    private val appContext = context.applicationContext

    @JavascriptInterface
    fun onPlaybackChanged(playing: Boolean) {
        PlaybackState.isPlaying = playing
        val serviceIntent = Intent(appContext, PlaybackService::class.java)
        if (playing) {
            ContextCompat.startForegroundService(appContext, serviceIntent)
        } else {
            appContext.stopService(serviceIntent)
            Handler(Looper.getMainLooper()).post {
                if (!PlaybackState.isPlaying) PlaybackWebViewHolder.destroyRetained()
            }
        }
    }
}

class PlaybackService : Service() {
    private var playbackWakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        playbackWakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "$packageName:MishkatPlayback"
        ).apply {
            setReferenceCounted(false)
            acquire()
        }
    }

    override fun onDestroy() {
        playbackWakeLock?.let { lock ->
            if (lock.isHeld) lock.release()
        }
        playbackWakeLock = null
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Mishkat")
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Mishkat playback",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps audio playback active"
            setSound(null, null)
            enableVibration(false)
        }
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "mishkat_playback"
        private const val NOTIFICATION_ID = 4301
    }
}
