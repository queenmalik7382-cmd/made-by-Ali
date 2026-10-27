package com.example.tools

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.AlarmClock
import android.provider.ContactsContract
import android.provider.Settings
import android.telecom.TelecomManager
import androidx.core.content.ContextCompat
import com.example.core.FlavorConfig
import com.example.data.MayaRepository
import com.example.network.PublicApisClient
import com.example.service.MayaAccessibilityService
import com.example.service.RoutineScheduler
import org.json.JSONObject
import java.time.LocalTime

class SystemToolsFactory(
    private val context: Context,
    private val repository: MayaRepository,
    private val publicApisClient: PublicApisClient,
    private val getYouTubeToken: () -> String
) {
    fun registerAll(registry: ToolRegistry) {
        registry.register(OpenAppTool())
        registry.register(ScreenAutomationTool())
        registry.register(DeviceSettingControlTool())
        registry.register(EnableWirelessDebuggingTool())
        registry.register(MakePhoneCallTool())
        registry.register(ManageActiveCallTool())
        registry.register(ComposeSmsTool())
        registry.register(SetAlarmOrTimerTool())
        registry.register(PlayMusicOrNavigateTool())
        registry.register(QueryRoutineTool())
        registry.register(AddRoutineTaskTool())
        registry.register(ReadRecentNotificationsTool())
        registry.register(SaveMemoryFactTool())
        registry.register(RunLearnedTaskTool())
        registry.register(AnalyzeYouTubeChannelTool())
    }

    fun openInstalledAppByName(appNameQuery: String): Boolean {
        val pm = context.packageManager
        val q = appNameQuery.trim().lowercase()
        val commonPackages = mapOf(
            "youtube" to "com.google.android.youtube",
            "whatsapp" to "com.whatsapp",
            "instagram" to "com.instagram.android",
            "tiktok" to "com.zhiliaoapp.musically",
            "spotify" to "com.spotify.music",
            "youtube music" to "com.google.android.apps.youtube.music",
            "maps" to "com.google.android.apps.maps",
            "chrome" to "com.android.chrome",
            "gmail" to "com.google.android.gm",
            "camera" to "com.android.camera",
            "settings" to "com.android.settings",
            "chatgpt" to "com.openai.chatgpt",
            "google gemini" to "com.google.android.apps.bard"
        )

        commonPackages[q]?.let { pkg ->
            pm.getLaunchIntentForPackage(pkg)?.let { launch ->
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launch)
                return true
            }
        }

        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val apps = pm.queryIntentActivities(mainIntent, 0)
        val matched = apps.firstOrNull { info ->
            val label = info.loadLabel(pm).toString().lowercase()
            label.contains(q) || info.activityInfo.packageName.lowercase().contains(q)
        }
        if (matched != null) {
            val launch = pm.getLaunchIntentForPackage(matched.activityInfo.packageName)
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launch)
                return true
            }
        }

        // Web fallback for popular sites if app is not installed on device
        val fallbackWeb = when {
            q.contains("youtube") -> "https://m.youtube.com"
            q.contains("instagram") -> "https://www.instagram.com"
            q.contains("tiktok") -> "https://www.tiktok.com"
            else -> null
        }
        if (fallbackWeb != null) {
            val browser = Intent(Intent.ACTION_VIEW, Uri.parse(fallbackWeb)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(browser); return true }
        }
        return false
    }

    private fun lookupContactPhone(nameQuery: String): Pair<String, String>? {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED
        ) return null

        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val args = arrayOf("%${nameQuery.trim()}%")
        context.contentResolver.query(uri, projection, selection, args, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val disp = cursor.getString(0) ?: nameQuery
                val num = cursor.getString(1) ?: return null
                return disp to num
            }
        }
        return null
    }

    inner class OpenAppTool : Tool {
        override val name = "open_app"
        override val description = "Opens any installed app by name such as YouTube, WhatsApp, Instagram, TikTok, Maps, Spotify, Settings."
        override val isDangerous = false

        override fun parametersSchema(): JSONObject = ToolRegistry.schemaObject(
            "appName" to ("STRING" to "Name of the app to open"),
            required = listOf("appName")
        )

        override suspend fun execute(args: JSONObject, confirmed: Boolean): ToolResult {
            val appName = args.optString("appName")
            val ok = openInstalledAppByName(appName)
            return if (ok) {
                ToolResult(true, "$appName khol diya hai.")
            } else {
                ToolResult(false, "$appName phone me install nahi mila.")
            }
        }
    }

    inner class ScreenAutomationTool : Tool {
        override val name = "screen_automation"
        override val description =
            "Controls the screen via Accessibility (Direct flavor): read_screen, find_and_click, tap_xy, swipe_up, swipe_down, type_text, back, home, recents, screenshot, lock."
        override val isDangerous = false

        override fun parametersSchema(): JSONObject = ToolRegistry.schemaObject(
            "action" to ("STRING" to "One of: read_screen, find_and_click, long_press, tap_xy, swipe_up, swipe_down, type_text, back, home, recents, screenshot, lock"),
            "label" to ("STRING" to "Button text or input text when applicable"),
            "x" to ("INTEGER" to "X pixel coordinate if tap_xy"),
            "y" to ("INTEGER" to "Y pixel coordinate if tap_xy"),
            required = listOf("action")
        )

        override suspend fun execute(args: JSONObject, confirmed: Boolean): ToolResult {
            if (!FlavorConfig.supportsAccessibilityAutomation) {
                return ToolResult(false, "Play Store flavor me Accessibility screen control band hai. Direct flavor use karo.")
            }
            val svc = MayaAccessibilityService.instance
                ?: return ToolResult(false, "Accessibility service abhi chalu nahi hai. Onboarding ya Settings se Maya Accessibility on karo.")

            val act = args.optString("action")
            val label = args.optString("label")
            return when (act) {
                "read_screen" -> {
                    val summary = svc.readScreenSummary()
                    ToolResult(true, "Screen padh liya hai.", JSONObject().put("screen", summary))
                }
                "find_and_click" -> {
                    val ok = svc.findAndClick(label, longClick = false)
                    ToolResult(ok, if (ok) "$label par tap kar diya." else "$label screen par nahi mila.")
                }
                "long_press" -> {
                    val ok = svc.findAndClick(label, longClick = true)
                    ToolResult(ok, if (ok) "$label ko long press kiya." else "$label nahi mila.")
                }
                "tap_xy" -> {
                    val x = args.optInt("x", 500).toFloat()
                    val y = args.optInt("y", 900).toFloat()
                    val ok = svc.tapCoordinates(x, y)
                    ToolResult(ok, "Screen ($x, $y) par tap kiya.")
                }
                "swipe_up" -> {
                    val ok = svc.scrollDirection(forward = true)
                    ToolResult(ok, "Agla hissa / next video dikhaya.")
                }
                "swipe_down" -> {
                    val ok = svc.scrollDirection(forward = false)
                    ToolResult(ok, "Upar wapas scroll kiya.")
                }
                "type_text" -> {
                    val ok = svc.typeTextIntoFocusedOrFirstInput(label)
                    ToolResult(ok, if (ok) "\"$label\" type kar diya." else "Koi text input box nahi mila.")
                }
                "back", "home", "recents", "screenshot", "lock" -> {
                    val ok = svc.performNavGlobal(act)
                    ToolResult(ok, "$act action chala diya.")
                }
                else -> ToolResult(false, "Unknown screen action: $act")
            }
        }
    }

    inner class DeviceSettingControlTool : Tool {
        override val name = "device_setting_control"
        override val description = "Controls volume (up/down/mute), flashlight (on/off), wifi panel, bluetooth panel, DND mode, or brightness settings."
        override val isDangerous = false

        override fun parametersSchema(): JSONObject = ToolRegistry.schemaObject(
            "setting" to ("STRING" to "volume, flashlight, wifi, bluetooth, dnd, brightness"),
            "value" to ("STRING" to "on, off, up, down, or 0-100"),
            required = listOf("setting", "value")
        )

        override suspend fun execute(args: JSONObject, confirmed: Boolean): ToolResult {
            val setting = args.optString("setting").lowercase()
            val value = args.optString("value").lowercase()
            return when (setting) {
                "volume" -> {
                    val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    when (value) {
                        "up" -> am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                        "down" -> am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                        "mute", "off" -> am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, AudioManager.FLAG_SHOW_UI)
                        else -> {
                            val pct = value.toIntOrNull()?.coerceIn(0, 100) ?: 60
                            val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                            am.setStreamVolume(AudioManager.STREAM_MUSIC, (max * pct) / 100, AudioManager.FLAG_SHOW_UI)
                        }
                    }
                    ToolResult(true, "Volume set kar diya.")
                }
                "flashlight" -> {
                    val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                    val camId = cm.cameraIdList.firstOrNull()
                        ?: return ToolResult(false, "Is device par flashlight nahi mili.")
                    val enable = value == "on" || value == "true" || value == "1"
                    runCatching { cm.setTorchMode(camId, enable) }
                        .fold(
                            onSuccess = { ToolResult(true, if (enable) "Flashlight chalu kar di." else "Flashlight band kar di.") },
                            onFailure = { ToolResult(false, "Flashlight badalne me error: ${it.localizedMessage}") }
                        )
                }
                "wifi" -> {
                    val panelAction = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        Settings.Panel.ACTION_INTERNET_CONNECTIVITY
                    } else Settings.ACTION_WIFI_SETTINGS
                    context.startActivity(Intent(panelAction).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    ToolResult(true, "Wi-Fi aur Internet panel khol diya hai.")
                }
                "bluetooth" -> {
                    context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    ToolResult(true, "Bluetooth settings khol di hain.")
                }
                "dnd" -> {
                    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    if (!nm.isNotificationPolicyAccessGranted) {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        ToolResult(false, "DND badalne ke liye Do Not Disturb access screen khol di hai.")
                    } else {
                        val mode = if (value == "on") NotificationManager.INTERRUPTION_FILTER_PRIORITY else NotificationManager.INTERRUPTION_FILTER_ALL
                        nm.setInterruptionFilter(mode)
                        ToolResult(true, if (value == "on") "Do Not Disturb chalu ho gaya." else "Do Not Disturb hata diya.")
                    }
                }
                "brightness" -> {
                    context.startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    ToolResult(true, "Display brightness screen khol di hai.")
                }
                else -> ToolResult(false, "Ye setting ($setting) samajh nahi aayi.")
            }
        }
    }

    inner class EnableWirelessDebuggingTool : Tool {
        override val name = "enable_wireless_debugging"
        override val description = "Turns on Developer Options and finds/enables Wireless Debugging via WRITE_SECURE_SETTINGS (if granted via ADB) or Accessibility automation."
        override val isDangerous = false

        override fun parametersSchema(): JSONObject = ToolRegistry.schemaObject()

        override suspend fun execute(args: JSONObject, confirmed: Boolean): ToolResult {
            if (!FlavorConfig.supportsSecureSettingsWrite) {
                context.startActivity(Intent(Settings.ACTION_DEVICE_INFO_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return ToolResult(
                    true,
                    "Play flavor me auto-tap band hai; About Phone khol diya hai, Build Number 7 baar tap karke Wireless debugging on kar lo."
                )
            }

            // Fast path: check if one-time `adb shell pm grant com.myassistant.app android.permission.WRITE_SECURE_SETTINGS` was run
            val hasSecureGrant = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.WRITE_SECURE_SETTINGS
            ) == PackageManager.PERMISSION_GRANTED

            if (hasSecureGrant) {
                val wrote = runCatching {
                    Settings.Global.putInt(context.contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 1)
                    Settings.Global.putInt(context.contentResolver, "adb_wifi_enabled", 1)
                    true
                }.getOrDefault(false)
                if (wrote) {
                    return ToolResult(true, "WRITE_SECURE_SETTINGS ke zariye Developer Options aur Wireless Debugging seedha ON kar diya gaya!")
                }
            }

            // Fallback: Accessibility 7x Build Number + scroll to Wireless Debugging
            val svc = MayaAccessibilityService.instance
            return if (svc != null) {
                val msg = svc.automateDeveloperOptionsAndWirelessDebugging()
                ToolResult(true, msg)
            } else {
                context.startActivity(Intent(Settings.ACTION_DEVICE_INFO_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                ToolResult(
                    false,
                    "About Phone khol diya hai. Agar Maya Accessibility ya one-time ADB WRITE_SECURE_SETTINGS on kar doge, to Maya khud Wireless Debugging on kar degi."
                )
            }
        }
    }

    inner class MakePhoneCallTool : Tool {
        override val name = "make_phone_call"
        override val description = "Looks up a contact by name or phone number and places a phone call. Requires voice confirmation."
        override val isDangerous = true

        override fun parametersSchema(): JSONObject = ToolRegistry.schemaObject(
            "contactName" to ("STRING" to "Contact name to search, e.g. Ali"),
            "phoneNumber" to ("STRING" to "Optional explicit phone number")
        )

        override suspend fun execute(args: JSONObject, confirmed: Boolean): ToolResult {
            val contactName = args.optString("contactName")
            var phone = args.optString("phoneNumber")
            var resolvedLabel = contactName.ifBlank { phone }

            if (phone.isBlank() && contactName.isNotBlank()) {
                val match = lookupContactPhone(contactName)
                if (match != null) {
                    resolvedLabel = match.first
                    phone = match.second
                }
            }
            if (phone.isBlank()) {
                return ToolResult(false, "$contactName ka phone number contacts me nahi mila.")
            }

            val hasCallPerm = ContextCompat.checkSelfPermission(context, android.Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
            val action = if (hasCallPerm) Intent.ACTION_CALL else Intent.ACTION_DIAL
            val callIntent = Intent(action, Uri.parse("tel:${Uri.encode(phone)}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(callIntent)
            return ToolResult(true, "$resolvedLabel ko call laga rahi hoon.")
        }
    }

    inner class ManageActiveCallTool : Tool {
        override val name = "manage_active_call"
        override val description = "Answers a ringing phone call ('answer') or hangs up an active/incoming call ('end')."
        override val isDangerous = false

        override fun parametersSchema(): JSONObject = ToolRegistry.schemaObject(
            "action" to ("STRING" to "answer or end"),
            required = listOf("action")
        )

        @Suppress("MissingPermission", "DEPRECATION")
        override suspend fun execute(args: JSONObject, confirmed: Boolean): ToolResult {
            if (!FlavorConfig.supportsAutoAnswerCall) {
                return ToolResult(false, "Play flavor me auto call uthana/kaatna Google Play policy ke mutabiq uplabdh nahi hai.")
            }
            val tm = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            val act = args.optString("action").lowercase()
            return if (act == "answer") {
                if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ANSWER_PHONE_CALLS) != PackageManager.PERMISSION_GRANTED) {
                    return ToolResult(false, "Answer phone calls permission abhi grant nahi hai.")
                }
                runCatching { tm.acceptRingingCall() }
                val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                am.isSpeakerphoneOn = true
                ToolResult(true, "Call utha li hai aur speakerphone chalu kar diya hai.")
            } else {
                val ended = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
                    ContextCompat.checkSelfPermission(context, android.Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_GRANTED
                ) {
                    runCatching { tm.endCall() }.getOrDefault(false)
                } else false
                ToolResult(ended, if (ended) "Call kaat di gayi." else "Call kaatne ki koshish ki.")
            }
        }
    }

    inner class ComposeSmsTool : Tool {
        override val name = "compose_sms"
        override val description = "Opens Android SMS compose screen with recipient and message prefilled (safe Intent, no SEND_SMS permission). Requires confirmation."
        override val isDangerous = true

        override fun parametersSchema(): JSONObject = ToolRegistry.schemaObject(
            "recipient" to ("STRING" to "Contact name or phone number"),
            "body" to ("STRING" to "Message text to prefill"),
            required = listOf("recipient", "body")
        )

        override suspend fun execute(args: JSONObject, confirmed: Boolean): ToolResult {
            val rawRecipient = args.optString("recipient")
            val body = args.optString("body")
            val resolvedNumber = lookupContactPhone(rawRecipient)?.second ?: rawRecipient

            val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:${Uri.encode(resolvedNumber)}")
                putExtra("sms_body", body)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(smsIntent)
            return ToolResult(true, "$rawRecipient ke liye SMS compose khol diya hai — bas Send daba do.")
        }
    }

    inner class SetAlarmOrTimerTool : Tool {
        override val name = "set_alarm_or_timer"
        override val description = "Sets a system alarm (hour, minute) or countdown timer (seconds)."
        override val isDangerous = false

        override fun parametersSchema(): JSONObject = ToolRegistry.schemaObject(
            "mode" to ("STRING" to "alarm or timer"),
            "hour" to ("INTEGER" to "Hour 0-23 if alarm"),
            "minute" to ("INTEGER" to "Minute 0-59 if alarm"),
            "seconds" to ("INTEGER" to "Duration in seconds if timer"),
            "label" to ("STRING" to "Alarm/timer label"),
            required = listOf("mode")
        )

        override suspend fun execute(args: JSONObject, confirmed: Boolean): ToolResult {
            val mode = args.optString("mode", "alarm")
            val label = args.optString("label", "Maya Reminder")
            val intent = if (mode == "timer") {
                Intent(AlarmClock.ACTION_SET_TIMER).apply {
                    putExtra(AlarmClock.EXTRA_LENGTH, args.optInt("seconds", 300))
                    putExtra(AlarmClock.EXTRA_MESSAGE, label)
                    putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } else {
                Intent(AlarmClock.ACTION_SET_ALARM).apply {
                    putExtra(AlarmClock.EXTRA_HOUR, args.optInt("hour", 7))
                    putExtra(AlarmClock.EXTRA_MINUTES, args.optInt("minute", 0))
                    putExtra(AlarmClock.EXTRA_MESSAGE, label)
                    putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            return runCatching {
                context.startActivity(intent)
                ToolResult(true, "$label ka $mode set kar diya hai.")
            }.getOrElse {
                ToolResult(false, "Clock app me $mode set nahi ho paya.")
            }
        }
    }

    inner class PlayMusicOrNavigateTool : Tool {
        override val name = "play_music_or_maps"
        override val description = "Plays a song/artist on Spotify/YouTube Music or starts Google Maps navigation to a destination."
        override val isDangerous = false

        override fun parametersSchema(): JSONObject = ToolRegistry.schemaObject(
            "type" to ("STRING" to "music or maps"),
            "query" to ("STRING" to "Song/artist name or destination address"),
            required = listOf("type", "query")
        )

        override suspend fun execute(args: JSONObject, confirmed: Boolean): ToolResult {
            val type = args.optString("type")
            val query = args.optString("query")
            return if (type == "maps") {
                val gmmIntentUri = Uri.parse("google.navigation:q=${Uri.encode(query)}")
                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                runCatching { context.startActivity(mapIntent) }.onFailure {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(query)}"))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
                ToolResult(true, "$query ka raasta Maps me khol diya hai.")
            } else {
                val musicIntent = Intent(android.provider.MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH).apply {
                    putExtra(android.app.SearchManager.QUERY, query)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                runCatching { context.startActivity(musicIntent) }.onFailure {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://music.youtube.com/search?q=${Uri.encode(query)}"))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
                ToolResult(true, "$query chala rahi hoon.")
            }
        }
    }

    inner class QueryRoutineTool : Tool {
        override val name = "query_routine"
        override val description = "Answers 'abhi mera kya kaam hai?' or 'aaj ki routine batao' from saved daily routine tasks."
        override val isDangerous = false

        override fun parametersSchema(): JSONObject = ToolRegistry.schemaObject(
            "mode" to ("STRING" to "now or today")
        )

        override suspend fun execute(args: JSONObject, confirmed: Boolean): ToolResult {
            val tasks = repository.getAllRoutineTasks().filter { it.isEnabled && !it.isSkippedToday }
            if (tasks.isEmpty()) {
                return ToolResult(true, "Aapki routine me abhi koi task save nahi hai. Bolo 'subah 7 baje gym' add karne ke liye.")
            }
            val now = LocalTime.now()
            val nowMinutes = now.hour * 60 + now.minute
            if (args.optString("mode") == "now") {
                val currentOrNext = tasks.minByOrNull { t ->
                    val diff = (t.hour * 60 + t.minute) - nowMinutes
                    if (diff >= -45) diff + 45 else diff + 24 * 60
                } ?: tasks.first()
                val timeFmt = "%02d:%02d".format(currentOrNext.hour, currentOrNext.minute)
                return ToolResult(
                    true,
                    "Abhi aapka agla ya chalu kaam hai: ${currentOrNext.title} ($timeFmt baje, streak ${currentOrNext.streakCount} din)."
                )
            } else {
                val summary = tasks.joinToString(", ") { "%02d:%02d par %s".format(it.hour, it.minute, it.title) }
                return ToolResult(true, "Aaj ki routine me ${tasks.size} kaam hain: $summary.")
            }
        }
    }

    inner class AddRoutineTaskTool : Tool {
        override val name = "add_routine_task"
        override val description = "Saves a daily routine item taught by voice (e.g., 7:00 Gym, 9:00 Work, 13:00 Lunch) and schedules an exact alarm."
        override val isDangerous = false

        override fun parametersSchema(): JSONObject = ToolRegistry.schemaObject(
            "title" to ("STRING" to "Task name, e.g. Gym"),
            "hour" to ("INTEGER" to "Hour 0-23"),
            "minute" to ("INTEGER" to "Minute 0-59"),
            required = listOf("title", "hour")
        )

        override suspend fun execute(args: JSONObject, confirmed: Boolean): ToolResult {
            val title = args.optString("title")
            val hour = args.optInt("hour", 8)
            val minute = args.optInt("minute", 0)
            val id = repository.addRoutineTask(title, hour, minute)
            RoutineScheduler.scheduleNextAlarm(context, id, title, hour, minute)
            return ToolResult(true, "Routine me save ho gaya: roz %02d:%02d baje $title.".format(hour, minute))
        }
    }

    inner class ReadRecentNotificationsTool : Tool {
        override val name = "read_recent_notifications"
        override val description = "Reads aloud recent captured WhatsApp, SMS, Gmail notifications ('pichli notifications sunao')."
        override val isDangerous = false

        override fun parametersSchema(): JSONObject = ToolRegistry.schemaObject(
            "count" to ("INTEGER" to "How many recent notifications to summarize (default 5)")
        )

        override suspend fun execute(args: JSONObject, confirmed: Boolean): ToolResult {
            val count = args.optInt("count", 5).coerceIn(1, 15)
            val recent = repository.getRecentNotifications(count)
            if (recent.isEmpty()) {
                return ToolResult(true, "Abhi koi nayi WhatsApp ya system notification save nahi hui hai.")
            }
            val spoken = recent.joinToString(". ") { "${it.appLabel} par ${it.sender}: ${it.text.take(90)}" }
            return ToolResult(true, "Pichli ${recent.size} notifications: $spoken")
        }
    }

    inner class SaveMemoryFactTool : Tool {
        override val name = "save_user_memory"
        override val description = "Remembers an important fact, preference, or relationship about the user in long-term Room memory."
        override val isDangerous = false

        override fun parametersSchema(): JSONObject = ToolRegistry.schemaObject(
            "factKey" to ("STRING" to "Short topic, e.g. Favourite Drink or Bhai ka naam"),
            "factValue" to ("STRING" to "Fact value to remember"),
            required = listOf("factKey", "factValue")
        )

        override suspend fun execute(args: JSONObject, confirmed: Boolean): ToolResult {
            val k = args.optString("factKey")
            val v = args.optString("factValue")
            repository.saveMemory(k, v)
            return ToolResult(true, "Yaad rakh liya: $k — $v.")
        }
    }

    inner class RunLearnedTaskTool : Tool {
        override val name = "run_learned_task"
        override val description = "Replays a saved Yaadein macro/task by name (e.g., 'gemini se poochho', 'chatgpt se poochho')."
        override val isDangerous = false

        override fun parametersSchema(): JSONObject = ToolRegistry.schemaObject(
            "taskName" to ("STRING" to "Name of the learned task"),
            "promptInput" to ("STRING" to "Optional prompt text to pass into the task"),
            required = listOf("taskName")
        )

        override suspend fun execute(args: JSONObject, confirmed: Boolean): ToolResult {
            val q = args.optString("taskName").lowercase()
            val input = args.optString("promptInput", "")
            val tasks = repository.getAllLearnedTasks()
            val match = tasks.firstOrNull { it.name.lowercase().contains(q) || q.contains(it.name.lowercase()) }
                ?: return ToolResult(false, "Yaadein me '$q' naam ka koi sikha hua task nahi mila.")

            if (!FlavorConfig.supportsAccessibilityAutomation) {
                return ToolResult(false, "Play flavor me Accessibility replay band hai; Direct flavor me '${match.name}' chalega.")
            }
            val svc = MayaAccessibilityService.instance
                ?: return ToolResult(false, "Task chalane ke liye pehle Maya Accessibility Service on karo.")

            val feedback = svc.replayLearnedTaskSteps(match.stepsJson, input) { app ->
                openInstalledAppByName(app)
            }
            return ToolResult(true, feedback)
        }
    }

    inner class AnalyzeYouTubeChannelTool : Tool {
        override val name = "analyze_youtube_channel"
        override val description = "Fetches user's YouTube Data + YouTube Analytics stats and provides a 30-second spoken growth analysis."
        override val isDangerous = false

        override fun parametersSchema(): JSONObject = ToolRegistry.schemaObject()

        override suspend fun execute(args: JSONObject, confirmed: Boolean): ToolResult {
            val token = getYouTubeToken()
            if (token.isBlank()) {
                return ToolResult(
                    false,
                    "Pehle YouTube Channel Analyst screen me Google Sign-In connect karo taaki main aapke views, CTR aur retention padh sakoon."
                )
            }
            return publicApisClient.fetchYouTubeChannelAnalytics(token).fold(
                onSuccess = { m ->
                    val wowSign = if (m.weekOverWeekGrowthPct >= 0) "+" else ""
                    val summary = "${m.channelTitle} par ${m.subscriberCount} subscribers aur pichle 28 din me ${m.viewsLast28Days} views aaye hain " +
                        "(${wowSign}${"%.1f".format(m.weekOverWeekGrowthPct)}% week-over-week). Watch time ${m.watchTimeMinutes28Days} minutes raha. " +
                        "Agli video ${m.bestUploadWindow} ke beech daalo aur shuru ke 15 second me strong hook rakho."
                    ToolResult(true, summary)
                },
                onFailure = {
                    ToolResult(false, "YouTube Analytics laane me dikkat aayi: ${it.localizedMessage}")
                }
            )
        }
    }
}
