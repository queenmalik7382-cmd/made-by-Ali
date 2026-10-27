package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.UserSettings
import com.example.data.AppAnnouncementEntity
import com.example.data.CapturedNotificationEntity
import com.example.data.RoutineTaskEntity
import com.example.network.YouTubeChannelMetrics
import com.example.ui.components.MayaPrimaryGradientButton
import com.example.ui.components.NeumorphicCard
import com.example.ui.theme.MayaTokens
import java.time.LocalDate

@Composable
fun RoutineScreen(
    routineTasks: List<RoutineTaskEntity>,
    onAddRoutine: (String, Int, Int) -> Unit,
    onCompleteRoutine: (RoutineTaskEntity) -> Unit,
    onSkipOrMoveRoutine: (RoutineTaskEntity, Int) -> Unit,
    onDeleteRoutine: (Long) -> Unit,
    onSpeakTodaySummary: () -> Unit
) {
    var titleInput by remember { mutableStateOf("") }
    var hourInput by remember { mutableStateOf("07") }
    var minInput by remember { mutableStateOf("00") }
    var voiceSentenceInput by remember { mutableStateOf("") }

    val totalCompletedStreaks = routineTasks.sumOf { it.streakCount }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = MayaTokens.ScreenHorizontalPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            NeumorphicCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Meri Routine & Streaks", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "${routineTasks.size} daily tasks • Kul streak points: $totalCompletedStreaks",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = Color(0xFFEA580C))
                        Spacer(Modifier.width(4.dp))
                        Text("$totalCompletedStreaks", style = MaterialTheme.typography.headlineMedium)
                    }
                }
            }
        }

        item {
            MayaPrimaryGradientButton(
                text = "Aaj ki routine sunao",
                icon = Icons.Default.RecordVoiceOver,
                onClick = onSpeakTodaySummary,
                testTag = "speak_routine_summary_btn"
            )
        }

        // Quick Hinglish Natural Routine Parser ("subah 7 baje gym, 9 baje kaam, 1 baje lunch")
        item {
            NeumorphicCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Bol kar ya likh kar routine sikhao", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = voiceSentenceInput,
                        onValueChange = { voiceSentenceInput = it },
                        placeholder = { Text("subah 7 baje gym, 9 baje kaam, 1 baje lunch") },
                        modifier = Modifier.fillMaxWidth().testTag("routine_natural_input")
                    )
                    Button(
                        onClick = {
                            parseNaturalHinglishRoutine(voiceSentenceInput).forEach { (t, h, m) ->
                                onAddRoutine(t, h, m)
                            }
                            voiceSentenceInput = ""
                        },
                        enabled = voiceSentenceInput.isNotBlank(),
                        modifier = Modifier.testTag("parse_routine_btn")
                    ) {
                        Text("Auto-Parse & Exact Alarm Lagao")
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = titleInput,
                            onValueChange = { titleInput = it },
                            label = { Text("Kaam") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = hourInput,
                            onValueChange = { hourInput = it },
                            label = { Text("HH") },
                            modifier = Modifier.width(72.dp)
                        )
                        OutlinedTextField(
                            value = minInput,
                            onValueChange = { minInput = it },
                            label = { Text("MM") },
                            modifier = Modifier.width(72.dp)
                        )
                    }
                    TextButton(
                        onClick = {
                            if (titleInput.isNotBlank()) {
                                onAddRoutine(
                                    titleInput,
                                    hourInput.toIntOrNull()?.coerceIn(0, 23) ?: 8,
                                    minInput.toIntOrNull()?.coerceIn(0, 59) ?: 0
                                )
                                titleInput = ""
                            }
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Single task jodo")
                    }
                }
            }
        }

        items(routineTasks, key = { it.id }) { task ->
            val doneToday = task.lastCompletedEpochDay == LocalDate.now().toEpochDay()
            NeumorphicCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "%02d:%02d  •  %s".format(task.hour, task.minute, task.title),
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "Streak: ${task.streakCount} din" + if (task.isSkippedToday) " (Aaj skip kiya)" else "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (doneToday) MayaTokens.MintCheck else MayaTokens.InkSecondary
                            )
                        }
                        IconButton(onClick = { onCompleteRoutine(task) }) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Poora hua",
                                tint = if (doneToday) MayaTokens.MintCheck else MayaTokens.InkMuted
                            )
                        }
                        IconButton(onClick = { onSkipOrMoveRoutine(task, 30) }) {
                            Icon(Icons.Default.SkipNext, contentDescription = "30 min aage badhao", tint = MayaTokens.PrimaryBlueDeep)
                        }
                        IconButton(onClick = { onDeleteRoutine(task.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MayaTokens.DangerRed)
                        }
                    }
                }
            }
        }
    }
}

fun parseNaturalHinglishRoutine(raw: String): List<Triple<String, Int, Int>> {
    val results = mutableListOf<Triple<String, Int, Int>>()
    val segments = raw.split(",", "aur", ";")
    val regex = Regex("""(subah|dopahar|shaam|raat)?\s*(\d{1,2})(?::(\d{2}))?\s*baje\s+(.+)""", RegexOption.IGNORE_CASE)
    for (seg in segments) {
        val m = regex.find(seg.trim()) ?: continue
        val period = m.groupValues[1].lowercase()
        var hour = m.groupValues[2].toIntOrNull() ?: continue
        val min = m.groupValues[3].toIntOrNull() ?: 0
        val title = m.groupValues[4].trim().replaceFirstChar { it.uppercase() }
        if ((period == "shaam" || period == "dopahar" || period == "raat") && hour in 1..11) {
            hour += 12
        } else if (period.isEmpty() && hour in 1..5 && title.lowercase().contains("lunch")) {
            hour += 12
        }
        results.add(Triple(title, hour.coerceIn(0, 23), min.coerceIn(0, 59)))
    }
    return results
}

@Composable
fun NotificationsCenterScreen(
    settings: UserSettings,
    announcements: List<AppAnnouncementEntity>,
    capturedNotifications: List<CapturedNotificationEntity>,
    geminiSmartSummary: String,
    onClearAll: () -> Unit,
    onSummarizeWithGemini: () -> Unit,
    onUpdateNotificationSettings: (Boolean, Boolean, Int, Int, String) -> Unit
) {
    var tab by remember { mutableIntStateOf(0) } // 0 = Maya Updates, 1 = Screen-Off Captured (WhatsApp/SMS/Gmail), 2 = Settings
    var allowPackages by remember(settings.allowedNotificationApps) { mutableStateOf(settings.allowedNotificationApps) }
    var readScreenOff by remember(settings.readNotificationsScreenOff) { mutableStateOf(settings.readNotificationsScreenOff) }
    var readFull by remember(settings.readNotificationFullText) { mutableStateOf(settings.readNotificationFullText) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = MayaTokens.ScreenHorizontalPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${announcements.size + capturedNotifications.size} notifications",
                    style = MaterialTheme.typography.titleMedium,
                    color = MayaTokens.InkSecondary
                )
                TextButton(onClick = onClearAll, modifier = Modifier.testTag("notifications_clear_all")) {
                    Text("Sab hatao", color = MayaTokens.InkPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = tab == 0, onClick = { tab = 0 }, label = { Text("App Feed") })
                FilterChip(selected = tab == 1, onClick = { tab = 1 }, label = { Text("WhatsApp/SMS (${capturedNotifications.size})") })
                FilterChip(selected = tab == 2, onClick = { tab = 2 }, label = { Text("Reader Settings") })
            }
        }

        when (tab) {
            0 -> {
                items(announcements, key = { it.id }) { ann ->
                    NeumorphicCard(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.Top) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MayaTokens.InkPrimary)
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(ann.title, style = MaterialTheme.typography.titleLarge)
                                Spacer(Modifier.height(8.dp))
                                Text(ann.body, style = MaterialTheme.typography.bodyLarge)
                                Spacer(Modifier.height(10.dp))
                                Text(ann.relativeAgeLabel, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            1 -> {
                item {
                    MayaPrimaryGradientButton(
                        text = "Gemini se Smart Summary suno",
                        icon = Icons.Default.AutoAwesome,
                        onClick = onSummarizeWithGemini,
                        testTag = "summarize_notifications_btn"
                    )
                }
                if (geminiSmartSummary.isNotBlank()) {
                    item {
                        NeumorphicCard(modifier = Modifier.fillMaxWidth()) {
                            Column {
                                Text("Smart Notification Summary", style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(6.dp))
                                Text(geminiSmartSummary, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
                if (capturedNotifications.isEmpty()) {
                    item {
                        NeumorphicCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                "Jab Notification Access on hoga, to aane wale WhatsApp, SMS aur Gmail messages yahan dikhenge aur screen lock hone par Maya unhe padh kar sunayegi.",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                } else {
                    items(capturedNotifications, key = { it.id }) { n ->
                        NeumorphicCard(modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = MayaTokens.PrimaryBlueDeep)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("${n.appLabel} • ${n.sender}", style = MaterialTheme.typography.titleMedium)
                                    Text(n.text, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }

            else -> {
                item {
                    NeumorphicCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Screen-Off Voice Reader", style = MaterialTheme.typography.titleLarge)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Screen band hone par bol kar sunao", modifier = Modifier.weight(1f))
                                Switch(
                                    checked = readScreenOff,
                                    onCheckedChange = {
                                        readScreenOff = it
                                        onUpdateNotificationSettings(it, readFull, settings.quietHoursStart, settings.quietHoursEnd, allowPackages)
                                    }
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Poora message padho (off = sirf bhejne wale ka naam)", modifier = Modifier.weight(1f))
                                Switch(
                                    checked = readFull,
                                    onCheckedChange = {
                                        readFull = it
                                        onUpdateNotificationSettings(readScreenOff, it, settings.quietHoursStart, settings.quietHoursEnd, allowPackages)
                                    }
                                )
                            }
                            Text(
                                "Quiet Hours: Raat ${settings.quietHoursStart}:00 se Subah ${settings.quietHoursEnd}:00 tak silent",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            OutlinedTextField(
                                value = allowPackages,
                                onValueChange = { allowPackages = it },
                                label = { Text("Allowed package filters (comma separated)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(onClick = {
                                onUpdateNotificationSettings(readScreenOff, readFull, settings.quietHoursStart, settings.quietHoursEnd, allowPackages)
                            }) {
                                Text("Allow-list Save Karo")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun YouTubeChannelAnalystScreen(
    clientId: String,
    hasToken: Boolean,
    metrics: YouTubeChannelMetrics?,
    aiCoachAdvice: String,
    errorMessage: String,
    isLoading: Boolean,
    onSaveClientIdAndToken: (String, String) -> Unit,
    onRefreshAndAnalyze: () -> Unit
) {
    val context = LocalContext.current
    var clientIdInput by remember(clientId) { mutableStateOf(clientId) }
    var manualAccessToken by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = MayaTokens.ScreenHorizontalPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            NeumorphicCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PlayCircleFilled, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("YouTube Channel Analyst", style = MaterialTheme.typography.titleLarge)
                    }
                    Text(
                        "Scopes: youtube.readonly + yt-analytics.readonly. Google Sign-In OAuth se connect karke views, watch time, CTR, retention aur week-over-week growth ka 30-second voice analysis suno.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    OutlinedTextField(
                        value = clientIdInput,
                        onValueChange = { clientIdInput = it },
                        label = { Text("Google Cloud OAuth Client ID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("yt_client_id_input")
                    )

                    Button(
                        onClick = {
                            if (clientIdInput.isNotBlank()) {
                                onSaveClientIdAndToken(clientIdInput, "")
                                val redirectUri = "com.myassistant.app:/oauth2redirect"
                                val scope = Uri.encode("https://www.googleapis.com/auth/youtube.readonly https://www.googleapis.com/auth/yt-analytics.readonly")
                                val authUrl = "https://accounts.google.com/o/oauth2/v2/auth?" +
                                    "client_id=${Uri.encode(clientIdInput.trim())}&" +
                                    "redirect_uri=${Uri.encode(redirectUri)}&" +
                                    "response_type=token&" +
                                    "scope=$scope"
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(authUrl)))
                            }
                        },
                        enabled = clientIdInput.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().testTag("yt_google_oauth_btn")
                    ) {
                        Text("Google OAuth Sign-In (YouTube + Analytics)")
                    }

                    OutlinedTextField(
                        value = manualAccessToken,
                        onValueChange = { manualAccessToken = it },
                        label = { Text("Ya OAuth Playground Bearer Token paste karo") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (manualAccessToken.isNotBlank()) {
                        TextButton(onClick = {
                            onSaveClientIdAndToken(clientIdInput, manualAccessToken)
                            manualAccessToken = ""
                        }) {
                            Text("Bearer Token Save & Connect Karo")
                        }
                    }

                    MayaPrimaryGradientButton(
                        text = if (isLoading) "Channel data aa raha hai..." else "Mere channel ka analysis karo",
                        icon = Icons.Default.AutoAwesome,
                        onClick = onRefreshAndAnalyze,
                        enabled = (hasToken || manualAccessToken.isNotBlank()) && !isLoading,
                        testTag = "yt_analyze_now_btn"
                    )

                    if (errorMessage.isNotBlank()) {
                        Text(errorMessage, color = MayaTokens.DangerRed, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        if (metrics != null) {
            item {
                NeumorphicCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(metrics.channelTitle, style = MaterialTheme.typography.headlineMedium)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            MetricMiniCol("Subscribers", "${metrics.subscriberCount}")
                            MetricMiniCol("28d Views", "${metrics.viewsLast28Days}")
                            MetricMiniCol("Watch Min", "${metrics.watchTimeMinutes28Days}")
                            MetricMiniCol("WoW", "${"%.1f".format(metrics.weekOverWeekGrowthPct)}%")
                        }
                        Text(
                            "Avg View Duration: ${metrics.averageViewDurationSec}s  •  Best Upload: ${metrics.bestUploadWindow}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Custom Compose Bar Chart for Top Videos & Traffic Sources
            if (metrics.topVideos.isNotEmpty()) {
                item {
                    NeumorphicCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Top Performing Videos (28 Days)", style = MaterialTheme.typography.titleLarge)
                            val maxV = (metrics.topVideos.maxOfOrNull { it.second } ?: 1L).coerceAtLeast(1L)
                            metrics.topVideos.forEach { (label, v) ->
                                Column {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(label, style = MaterialTheme.typography.bodyMedium)
                                        Text("$v views", style = MaterialTheme.typography.labelLarge)
                                    }
                                    Canvas(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(10.dp)
                                    ) {
                                        val ratio = (v.toFloat() / maxV.toFloat()).coerceIn(0.05f, 1f)
                                        drawRoundRect(
                                            color = Color(0xFFD6E4FA),
                                            size = Size(size.width, size.height),
                                            cornerRadius = CornerRadius(12f, 12f)
                                        )
                                        drawRoundRect(
                                            color = Color(0xFF2D8CFF),
                                            size = Size(size.width * ratio, size.height),
                                            cornerRadius = CornerRadius(12f, 12f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (aiCoachAdvice.isNotBlank()) {
            item {
                NeumorphicCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Gemini Growth & Thumbnail/Title Coach", style = MaterialTheme.typography.titleLarge)
                        Text(aiCoachAdvice, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricMiniCol(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
