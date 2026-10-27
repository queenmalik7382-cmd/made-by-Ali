package com.example.service

import android.app.KeyguardManager
import android.app.Notification
import android.content.Context
import android.os.PowerManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.MayaApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalTime

/**
 * Captures WhatsApp, SMS, Gmail, Telegram notifications into Room,
 * and reads them aloud when screen is off or locked (respecting per-app allow list & quiet hours).
 */
class MayaNotificationListenerService : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null || sbn.isOngoing) return
        val pkg = sbn.packageName ?: return
        if (pkg == packageName || pkg == "android" || pkg.contains("systemui")) return

        val extras = sbn.notification?.extras ?: return
        val sender = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim()
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()

        if (sender.isBlank() && text.isBlank()) return

        val app = application as? MayaApplication ?: return
        val appLabel = runCatching {
            val ai = packageManager.getApplicationInfo(pkg, 0)
            packageManager.getApplicationLabel(ai).toString()
        }.getOrDefault(pkg.substringAfterLast('.'))

        scope.launch {
            val settings = app.container.preferences.settingsFlow.first()
            val allowedList = settings.allowedNotificationApps
                .split(",")
                .map { it.trim().lowercase() }
                .filter { it.isNotBlank() }

            val isAppAllowed = allowedList.isEmpty() || allowedList.any { pkg.lowercase().contains(it) }
            val shouldSpeak = isAppAllowed &&
                settings.readNotificationsScreenOff &&
                isScreenOffOrLocked() &&
                !isWithinQuietHours(settings.quietHoursStart, settings.quietHoursEnd)

            app.container.repository.addCapturedNotification(
                packageName = pkg,
                appLabel = appLabel,
                sender = sender.ifBlank { appLabel },
                text = text,
                wasReadAloud = shouldSpeak
            )

            if (shouldSpeak) {
                val spoken = if (settings.readNotificationFullText) {
                    "${sender.ifBlank { appLabel }} ka $appLabel message aaya: ${text.take(140)}"
                } else {
                    "${sender.ifBlank { appLabel }} ka $appLabel par message aaya hai."
                }
                app.container.voiceEngine.speakViaTts(spoken)
            }
        }
    }

    private fun isScreenOffOrLocked(): Boolean {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        return !pm.isInteractive || km.isKeyguardLocked
    }

    private fun isWithinQuietHours(startHour: Int, endHour: Int): Boolean {
        val h = LocalTime.now().hour
        return if (startHour <= endHour) {
            h in startHour until endHour
        } else {
            h >= startHour || h < endHour
        }
    }
}
