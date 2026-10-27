package com.example.service

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.service.voice.VoiceInteractionService
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService
import android.telecom.Call
import android.telecom.InCallService
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.MayaApplication
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

class MayaVoiceInteractionService : VoiceInteractionService()

class MayaVoiceInteractionSessionService : VoiceInteractionSessionService() {
    override fun onNewSession(args: Bundle?): VoiceInteractionSession {
        return object : VoiceInteractionSession(this) {
            override fun onShow(args: Bundle?, showFlags: Int) {
                super.onShow(args, showFlags)
                val launch = Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    putExtra("auto_start_voice", true)
                }
                context.startActivity(launch)
                hide()
            }
        }
    }
}

class MayaInCallService : InCallService() {
    override fun onCallAdded(call: Call?) {
        super.onCallAdded(call)
        val handle = call?.details?.handle?.schemeSpecificPart.orEmpty()
        val app = application as? MayaApplication ?: return
        app.container.voiceEngine.speakViaTts("Incoming call: ${handle.ifBlank { "Unknown number" }}")
    }
}

class RoutineAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val app = context.applicationContext as? MayaApplication ?: return
        when (intent?.action) {
            ACTION_EMERGENCY_STOP -> {
                app.container.voiceEngine.stopEverything()
            }
            ACTION_ROUTINE_ALARM -> {
                val title = intent.getStringExtra("title") ?: "Routine kaam"
                val id = intent.getLongExtra("task_id", 1L)
                val hour = intent.getIntExtra("hour", 8)
                val minute = intent.getIntExtra("minute", 0)
                val spoken = "Abhi aapka $title ka time hai!"
                app.container.voiceEngine.speakViaTts(spoken)
                postRoutineNotification(context, id.toInt(), title, spoken)
                RoutineScheduler.scheduleNextAlarm(context, id, title, hour, minute)
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                CoroutineScope(Dispatchers.IO).launch {
                    app.container.repository.getAllRoutineTasks().filter { it.isEnabled }.forEach { t ->
                        RoutineScheduler.scheduleNextAlarm(context, t.id, t.title, t.hour, t.minute)
                    }
                }
            }
        }
    }

    private fun postRoutineNotification(context: Context, id: Int, title: String, message: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(
                    "maya_routine_channel",
                    context.getString(R.string.routine_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
                )
            )
        }
        val pi = PendingIntent.getActivity(
            context,
            id,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notif = NotificationCompat.Builder(context, "maya_routine_channel")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Meri Routine: $title")
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        nm.notify(7000 + id, notif)
    }

    companion object {
        const val ACTION_ROUTINE_ALARM = "com.myassistant.app.ACTION_ROUTINE_ALARM"
        const val ACTION_EMERGENCY_STOP = "com.myassistant.app.ACTION_EMERGENCY_STOP"
    }
}

class RoutineSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as? MayaApplication ?: return Result.success()
        app.container.repository.getAllRoutineTasks().filter { it.isEnabled }.forEach { t ->
            RoutineScheduler.scheduleNextAlarm(applicationContext, t.id, t.title, t.hour, t.minute)
        }
        return Result.success()
    }
}

object RoutineScheduler {
    fun scheduleNextAlarm(context: Context, taskId: Long, title: String, hour: Int, minute: Int) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        val pi = PendingIntent.getBroadcast(
            context,
            (9000 + taskId).toInt(),
            Intent(context, RoutineAlarmReceiver::class.java).apply {
                action = RoutineAlarmReceiver.ACTION_ROUTINE_ALARM
                putExtra("task_id", taskId)
                putExtra("title", title)
                putExtra("hour", hour)
                putExtra("minute", minute)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
            }
        }
    }

    fun enqueueDailyWorkManagerSync(context: Context) {
        val req = PeriodicWorkRequestBuilder<RoutineSyncWorker>(12, TimeUnit.HOURS).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "maya_routine_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            req
        )
    }
}
