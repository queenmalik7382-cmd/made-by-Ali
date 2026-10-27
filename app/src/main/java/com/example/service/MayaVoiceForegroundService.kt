package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.provider.ContactsContract
import android.telecom.TelecomManager
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.MayaApplication
import com.example.R
import com.example.core.FlavorConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Foreground service (type = microphone) keeping Maya alive when screen is off,
 * monitoring incoming calls for caller announcement + optional experimental AI Receptionist mode,
 * and providing a persistent Emergency Stop ("Ruko") notification button.
 */
class MayaVoiceForegroundService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var telephonyCallback31: Any? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForegroundSafely()
        registerCallStateObserver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SERVICE) {
            (application as? MayaApplication)?.container?.voiceEngine?.stopEverything()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        startForegroundSafely()
        return START_STICKY
    }

    private fun startForegroundSafely() {
        val notification = buildPersistentNotification()
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val hasMic = ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
                if (hasMic) {
                    startForeground(
                        NOTIF_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                    )
                } else {
                    stopSelf()
                }
            } else {
                startForeground(NOTIF_ID, notification)
            }
        }
    }

    private fun buildPersistentNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            100,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stopIntent = PendingIntent.getBroadcast(
            this,
            101,
            Intent(this, RoutineAlarmReceiver::class.java).apply {
                action = RoutineAlarmReceiver.ACTION_EMERGENCY_STOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Maya Live Hai")
            .setContentText("Background voice aur caller/notification assistant active hai.")
            .setOngoing(true)
            .setContentIntent(openIntent)
            .addAction(
                android.R.drawable.ic_media_pause,
                getString(R.string.emergency_stop_action),
                stopIntent
            )
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val ch = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.fg_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.fg_channel_desc)
            }
            nm.createNotificationChannel(ch)
        }
    }

    private fun registerCallStateObserver() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val tm = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val cb = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                override fun onCallStateChanged(state: Int) {
                    if (state == TelephonyManager.CALL_STATE_RINGING) {
                        handleIncomingCallRinging("")
                    }
                }
            }
            runCatching {
                tm.registerTelephonyCallback(mainExecutor, cb)
                telephonyCallback31 = cb
            }
        }
    }

    @Suppress("MissingPermission", "DEPRECATION")
    fun handleIncomingCallRinging(incomingNumber: String) {
        val app = application as? MayaApplication ?: return
        scope.launch {
            val settings = app.container.preferences.settingsFlow.first()
            val callerName = resolveCallerName(incomingNumber)
            app.container.voiceEngine.speakViaTts("$callerName ki call aa rahi hai.")

            // Experimental Auto-Answer + Receptionist Mode (Direct flavor only + Legal Notice accepted)
            if (FlavorConfig.supportsAutoAnswerCall &&
                settings.receptionistModeEnabled &&
                settings.legalCallNoticeAccepted &&
                ContextCompat.checkSelfPermission(this@MayaVoiceForegroundService, android.Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_GRANTED
            ) {
                delay(settings.autoAnswerDelaySec * 1000L)
                val telecom = getSystemService(Context.TELECOM_SERVICE) as TelecomManager
                runCatching { telecom.acceptRingingCall() }
                val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                am.isSpeakerphoneOn = true
                delay(900)
                val greeting =
                    "Namaste, main ${settings.assistantName} AI assistant bol rahi hoon. Abhi phone owner busy hain, kya message dena hai? Ye baat note ki ja rahi hai."
                app.container.voiceEngine.speakViaTts(greeting)
                app.container.repository.addCallNote(
                    callerName = callerName,
                    number = incomingNumber.ifBlank { "Private/Unknown" },
                    transcript = "AI Receptionist greeted caller over speakerphone.",
                    summary = "$callerName ne call kiya jab Receptionist mode chalu tha."
                )
            }
        }
    }

    private fun resolveCallerName(number: String): String {
        if (number.isBlank()) return "Kisi caller"
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED
        ) return number
        val uri = android.net.Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            android.net.Uri.encode(number)
        )
        contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) return c.getString(0) ?: number
        }
        return number
    }

    override fun onDestroy() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val tm = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            (telephonyCallback31 as? TelephonyCallback)?.let {
                runCatching { tm.unregisterTelephonyCallback(it) }
            }
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "maya_voice_fg_channel"
        const val NOTIF_ID = 4201
        const val ACTION_STOP_SERVICE = "com.myassistant.app.STOP_FG_VOICE"
    }
}
