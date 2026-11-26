package com.abdelrahman.mishkat

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    private var webView: WebView? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.BLACK
        window.navigationBarColor = android.graphics.Color.BLACK

        requestNotificationPermissionIfNeeded()
        ReminderScheduler.scheduleNext(this)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            requestExactAlarmAccessIfNeeded()
        }

        val retainedView = PlaybackWebViewHolder.take()
        val view = retainedView ?: WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.loadsImagesAutomatically = true
            settings.javaScriptCanOpenWindowsAutomatically = false
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.setSupportMultipleWindows(false)
            settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
            addJavascriptInterface(PlaybackBridge(applicationContext), "MishkatPlayback")
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    super.onPageFinished(view, url)
                    view.evaluateJavascript(MEDIA_TRACKING_SCRIPT, null)
                }
            }
            webChromeClient = WebChromeClient()
        }

        view.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        webView = view
        setContentView(view)

        // Preserve the native status-bar inset fix without changing website CSS or breakpoints.
        ViewCompat.setOnApplyWindowInsetsListener(view) { target, insets ->
            val topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            if (target.paddingTop != topInset) target.setPadding(0, topInset, 0, 0)
            insets
        }
        ViewCompat.requestApplyInsets(view)

        if (retainedView == null) {
            val restored = savedInstanceState?.let { view.restoreState(it) }
            if (restored == null) view.loadUrl(BuildConfig.WEB_URL)
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Keep the WebView alive in the background while its audio is playing.
                if (PlaybackState.isPlaying) {
                    moveTaskToBack(true)
                    return
                }
                val current = webView
                if (current?.canGoBack() == true) current.goBack() else finish()
            }
        })
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_NOTIFICATIONS
            )
        }
    }

    private fun requestExactAlarmAccessIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        if (alarmManager.canScheduleExactAlarms()) return

        val preferences = getSharedPreferences(PREFERENCES, MODE_PRIVATE)
        if (preferences.getBoolean(KEY_EXACT_ALARM_PROMPTED, false)) return

        preferences.edit().putBoolean(KEY_EXACT_ALARM_PROMPTED, true).apply()
        try {
            startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                }
            )
        } catch (_: Exception) {
            // The scheduler safely falls back to an inexact, doze-aware alarm.
        }
    }

    override fun onResume() {
        super.onResume()
        webView?.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
            val preferences = getSharedPreferences(PREFERENCES, MODE_PRIVATE)
            if (alarmManager.canScheduleExactAlarms() &&
                !preferences.getBoolean(KEY_EXACT_ALARM_ENABLED, false)
            ) {
                preferences.edit().putBoolean(KEY_EXACT_ALARM_ENABLED, true).apply()
                ReminderScheduler.scheduleNext(this, reset = true)
            }
        }
    }

    override fun onPause() {
        // Do not tell WebView to pause media while its native playback service is active.
        if (!PlaybackState.isPlaying) webView?.onPause()
        super.onPause()
    }

    @Deprecated("Deprecated in Android API")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_NOTIFICATIONS) requestExactAlarmAccessIfNeeded()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        webView?.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        val current = webView
        if (current != null) {
            (current.parent as? ViewGroup)?.removeView(current)
            if (PlaybackState.isPlaying) {
                // Retain the playing WebView so task dismissal/recreation does not kill the stream.
                PlaybackWebViewHolder.retain(current)
            } else {
                current.stopLoading()
                current.destroy()
            }
        }
        webView = null
        super.onDestroy()
    }

    companion object {
        private const val REQUEST_NOTIFICATIONS = 7301
        private const val PREFERENCES = "mishkat_reminders"
        private const val KEY_EXACT_ALARM_PROMPTED = "exact_alarm_prompted"
        private const val KEY_EXACT_ALARM_ENABLED = "exact_alarm_enabled"

        private val MEDIA_TRACKING_SCRIPT = """
            (function () {
              if (window.__mishkatMediaTrackingInstalled) return;
              window.__mishkatMediaTrackingInstalled = true;
              let lastState = null;
              const activeSources = new Set();
              const activeContexts = new Set();
              function isPlaying() {
                const mediaActive = Array.from(document.querySelectorAll('audio,video'))
                  .some(function (media) { return !media.paused && !media.ended; });
                return mediaActive || activeSources.size > 0;
              }
              function resumeBackgroundAudio() {
                if (!document.hidden) return;
                activeContexts.forEach(function (context) {
                  if (context && context.state === 'suspended') {
                    context.resume().catch(function () {});
                  }
                });
              }
              function report() {
                const active = isPlaying();
                if (active === lastState) return;
                lastState = active;
                try {
                  if (window.MishkatPlayback) {
                    window.MishkatPlayback.onPlaybackChanged(active);
                  }
                } catch (error) {}
              }
              function observe(media) {
                if (media.__mishkatObserved) return;
                media.__mishkatObserved = true;
                ['play','playing','pause','ended','emptied','abort','waiting'].forEach(function (eventName) {
                  media.addEventListener(eventName, function () {
                    window.setTimeout(report, 80);
                  });
                });
              }
              function scan(root) {
                if (root && root.matches && root.matches('audio,video')) observe(root);
                if (root && root.querySelectorAll) {
                  root.querySelectorAll('audio,video').forEach(observe);
                }
                report();
              }
              scan(document);
              if (window.AudioScheduledSourceNode &&
                  !window.AudioScheduledSourceNode.prototype.__mishkatWrapped) {
                const sourcePrototype = window.AudioScheduledSourceNode.prototype;
                const originalStart = sourcePrototype.start;
                sourcePrototype.start = function () {
                  const source = this;
                  const result = originalStart.apply(source, arguments);
                  activeSources.add(source);
                  if (source.context) {
                    activeContexts.add(source.context);
                    if (source.context.addEventListener) {
                      source.context.addEventListener('statechange', report);
                    }
                  }
                  source.addEventListener('ended', function () {
                    activeSources.delete(source);
                    if (source.context &&
                        !Array.from(activeSources).some(function (item) {
                          return item.context === source.context;
                        })) {
                      activeContexts.delete(source.context);
                    }
                    report();
                  }, { once: true });
                  report();
                  return result;
                };
                sourcePrototype.__mishkatWrapped = true;
              }
              new MutationObserver(function (records) {
                records.forEach(function (record) {
                  record.addedNodes.forEach(function (node) {
                    if (node.nodeType === 1) scan(node);
                  });
                });
              }).observe(document.documentElement, { childList: true, subtree: true });
              document.addEventListener('visibilitychange', function () {
                resumeBackgroundAudio();
                window.setTimeout(report, 150);
              });
              window.setInterval(report, 1500);
            })();
        """.trimIndent()
    }
}
