package com.example.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.AppStrings
import com.example.core.UserSettings
import com.example.data.ChatMessageEntity
import com.example.service.VoiceOrbState
import com.example.ui.components.MayaComposerPill
import com.example.ui.components.NeumorphicCard
import com.example.ui.theme.MayaTokens
import java.time.LocalDateTime
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

@Composable
fun HomeScreen(
    settings: UserSettings,
    orbState: VoiceOrbState,
    weatherLabel: String,
    liveTranscript: String,
    onSendPrompt: (String, ByteArray?, String?, String?, String?) -> Unit,
    onQuickActionPrompt: (String) -> Unit,
    onNavigateUpgrade: () -> Unit,
    onNavigateChat: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    val context = LocalContext.current
    val now = remember { LocalDateTime.now() }
    val dayNum = now.dayOfMonth.toString()
    val dayMonthLabel = "${now.dayOfWeek.getDisplayName(JavaTextStyle.SHORT, Locale.ENGLISH)}, ${now.month.getDisplayName(JavaTextStyle.SHORT, Locale.ENGLISH)}"

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
            onSendPrompt("Is photo ko dekh kar batao isme kya hai.", bytes, mime, uri.toString(), "photo.jpg")
            onNavigateChat()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MayaTokens.ScreenHorizontalPadding),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Free mode / Upgrade banner (matches screenshot)
        if (!settings.licenseActivated) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFFCFE0FB))
                    .border(1.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(22.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MayaTokens.PrimaryBlueDeep,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Free mode • Gemini API direct",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = "Saare tools aur Pro creator packs ke liye activate karo",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 12.sp
                    )
                }
                TextButton(onClick = onNavigateUpgrade, modifier = Modifier.testTag("home_activate_btn")) {
                    Text("Activate karo", color = MayaTokens.PrimaryBlueDeep, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Greeting + Energy Pill Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text(
                    text = AppStrings.greeting(settings.language, now.hour),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MayaTokens.InkSecondary
                )
                Text(
                    text = AppStrings.friendWord(settings.language),
                    style = MaterialTheme.typography.displayLarge
                )
            }

            NeumorphicCard(
                cornerRadius = 22.dp,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "Energy",
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Column {
                        Text(
                            text = settings.energyPoints.toString(),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text("Energy", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // Center HUD M.A.Y.A Dial (matches screenshot)
        MayaHudCenterpiece(
            assistantName = settings.assistantName,
            orbState = orbState,
            liveTranscript = liveTranscript
        )

        // Quick Action Pills: Gaane, Padhai, Journal
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickChipPill(
                label = "Gaane",
                icon = Icons.Default.MusicNote,
                modifier = Modifier.weight(1f),
                testTag = "quick_gaane",
                onClick = { onQuickActionPrompt("Koi achha relaxing Hindi lofi gaana chalao") }
            )
            QuickChipPill(
                label = "Padhai",
                icon = Icons.AutoMirrored.Filled.MenuBook,
                modifier = Modifier.weight(1f),
                testTag = "quick_padhai",
                onClick = { onQuickActionPrompt("Meri padhai aur focus ke liye 25 minute ka Pomodoro plan banao") }
            )
            QuickChipPill(
                label = "Journal",
                icon = Icons.Default.Edit,
                modifier = Modifier.weight(1f),
                testTag = "quick_journal",
                onClick = { onQuickActionPrompt("Aaj ka mera journal aur mood yaad rakh lo: Sab badhiya chal raha hai") }
            )
        }

        // 3 Status Cards: Mausam, Aaj, Mood
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HomeStatTile(
                icon = Icons.Default.Cloud,
                label = "Mausam",
                headline = weatherLabel.ifBlank { "—" },
                subtext = if (weatherLabel.isBlank()) "Tap refresh" else "Live Open-Meteo",
                modifier = Modifier.weight(1f),
                onClick = { onQuickActionPrompt("Aaj ka mausam kaisa hai?") }
            )
            HomeStatTile(
                icon = Icons.Default.CalendarToday,
                label = "Aaj",
                headline = dayNum,
                subtext = dayMonthLabel,
                modifier = Modifier.weight(1f),
                onClick = { onQuickActionPrompt("Aaj ki routine batao") }
            )
            HomeStatTile(
                icon = Icons.Default.Favorite,
                label = "Mood",
                headline = settings.moodStatus,
                subtext = "Sab badhiya",
                modifier = Modifier.weight(1f),
                accentHeart = true,
                onClick = { onQuickActionPrompt("Mera mood khush aur energetic set karo") }
            )
        }

        // Composer Pill
        MayaComposerPill(
            value = inputText,
            placeholder = AppStrings.askAnythingPlaceholder(settings.assistantName, settings.language),
            onValueChange = { inputText = it },
            onAttachClick = {
                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onSendClick = {
                if (inputText.isNotBlank()) {
                    onSendPrompt(inputText, null, null, null, null)
                    inputText = ""
                    onNavigateChat()
                }
            }
        )
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun MayaHudCenterpiece(
    assistantName: String,
    orbState: VoiceOrbState,
    liveTranscript: String
) {
    val infinite = rememberInfiniteTransition(label = "hud_rot")
    val sweepAngle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(10000, easing = LinearEasing), RepeatMode.Restart),
        label = "hud_sweep"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(235.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(220.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerR = size.minDimension * 0.46f
            val midR = size.minDimension * 0.38f

            drawCircle(
                color = Color(0xFFBBD0F2),
                radius = outerR,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.85f),
                radius = midR,
                center = center,
                style = Stroke(width = 6.dp.toPx())
            )
            drawArc(
                color = Color(0xFF3B82F6),
                startAngle = sweepAngle,
                sweepAngle = 78f,
                useCenter = false,
                style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                color = Color(0xFFEC4899).copy(alpha = 0.7f),
                startAngle = sweepAngle + 180f,
                sweepAngle = 55f,
                useCenter = false,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "AI ASSISTANT",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = MayaTokens.InkMuted
            )
            Spacer(Modifier.height(4.dp))
            val dottedName = assistantName.uppercase().toCharArray().joinToString(".")
            Text(
                text = buildAnnotatedString {
                    dottedName.forEachIndexed { idx, ch ->
                        val col = when {
                            idx == dottedName.lastIndex -> MayaTokens.AccentPink
                            idx % 2 == 0 -> MayaTokens.InkPrimary
                            else -> MayaTokens.PrimaryBlue
                        }
                        withStyle(SpanStyle(color = col, fontWeight = FontWeight.ExtraBold)) {
                            append(ch)
                        }
                    }
                },
                fontSize = 30.sp,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(6.dp))
            val statusSub = when (orbState) {
                VoiceOrbState.IDLE -> "BOLO, KAISE HELP KARUN?"
                VoiceOrbState.LISTENING -> "SUN RAHI HOON..."
                VoiceOrbState.THINKING -> "SOCH RAHI HOON..."
                VoiceOrbState.SPEAKING -> "BOL RAHI HOON (BARGE-IN ON)"
                VoiceOrbState.AWAITING_CONFIRMATION -> "HAAN YA NAHI BOLO?"
            }
            Text(
                text = if (liveTranscript.isNotBlank() && orbState != VoiceOrbState.IDLE) {
                    liveTranscript.take(48)
                } else statusSub,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MayaTokens.InkSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp),
                maxLines = 2
            )
        }
    }
}

@Composable
private fun QuickChipPill(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    testTag: String,
    onClick: () -> Unit
) {
    NeumorphicCard(
        cornerRadius = 24.dp,
        onClick = onClick,
        testTag = testTag,
        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 10.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MayaTokens.PrimaryBlueDeep, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

@Composable
private fun HomeStatTile(
    icon: ImageVector,
    label: String,
    headline: String,
    subtext: String,
    modifier: Modifier = Modifier,
    accentHeart: Boolean = false,
    onClick: () -> Unit
) {
    NeumorphicCard(
        cornerRadius = 24.dp,
        onClick = onClick,
        contentPadding = PaddingValues(14.dp),
        modifier = modifier.height(114.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (accentHeart) MayaTokens.AccentPink else MayaTokens.InkSecondary,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(5.dp))
                Text(label, style = MaterialTheme.typography.labelSmall, color = MayaTokens.InkSecondary)
            }
            Text(
                text = headline,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtext,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ChatScreen(
    settings: UserSettings,
    messages: List<ChatMessageEntity>,
    orbState: VoiceOrbState,
    onSendMessage: (String, ByteArray?, String?, String?, String?) -> Unit,
    onClearChat: () -> Unit
) {
    var input by remember { mutableStateOf("") }
    var attachedBytes by remember { mutableStateOf<ByteArray?>(null) }
    var attachedMime by remember { mutableStateOf<String?>(null) }
    var attachedName by remember { mutableStateOf<String?>(null) }
    var attachedUriStr by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current

    // Supports Image, PDF, and Markdown (.md / text) attachments via SAF OpenDocument
    val fileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
            var displayName = "attachment"
            context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && c.moveToFirst()) displayName = c.getString(idx) ?: displayName
            }
            val rawBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (rawBytes != null) {
                // If .md or plain text, pass as text/plain; PDF as application/pdf; images as image/*
                val resolvedMime = when {
                    displayName.endsWith(".md", ignoreCase = true) -> "text/markdown"
                    mime.startsWith("image/") || mime == "application/pdf" || mime.startsWith("text/") -> mime
                    else -> "text/plain"
                }
                attachedBytes = rawBytes
                attachedMime = resolvedMime
                attachedName = displayName
                attachedUriStr = uri.toString()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = MayaTokens.ScreenHorizontalPadding)
    ) {
        // Live status indicator row ("Maya live hai") matching screenshot
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF16A34A))
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (orbState == VoiceOrbState.IDLE) "${settings.assistantName} live hai" else "${settings.assistantName}: ${orbState.name.lowercase()}",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            if (messages.isNotEmpty()) {
                TextButton(onClick = onClearChat, modifier = Modifier.testTag("clear_chat_button")) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Saaf karo")
                }
            }
        }

        if (messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = null,
                        tint = MayaTokens.InkMuted,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = "${settings.assistantName} se kuchh bhi poochho —\ntext likho, ya image / PDF / .md file bhejo.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MayaTokens.InkSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 10.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    val isUser = msg.role == "user"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                    ) {
                        Column(
                            modifier = Modifier
                                .widthIn(max = 310.dp)
                                .clip(
                                    RoundedCornerShape(
                                        topStart = 22.dp,
                                        topEnd = 22.dp,
                                        bottomStart = if (isUser) 22.dp else 6.dp,
                                        bottomEnd = if (isUser) 6.dp else 22.dp
                                    )
                                )
                                .background(
                                    if (isUser) {
                                        Brush.horizontalGradient(listOf(Color(0xFF4CA0FF), Color(0xFF2575FC)))
                                    } else {
                                        Brush.verticalGradient(listOf(Color.White, MayaTokens.SurfaceCard))
                                    }
                                )
                                .border(
                                    1.dp,
                                    if (isUser) Color.Transparent else Color.White,
                                    RoundedCornerShape(22.dp)
                                )
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            if (!msg.attachmentName.isNullOrBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Description,
                                        contentDescription = null,
                                        tint = if (isUser) Color.White else MayaTokens.PrimaryBlueDeep,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = msg.attachmentName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isUser) Color.White.copy(alpha = 0.9f) else MayaTokens.PrimaryBlueDeep
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                            }
                            Text(
                                text = msg.content,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isUser) Color.White else MayaTokens.InkPrimary
                            )
                            if (msg.source == "voice") {
                                Text(
                                    text = "🎙 Voice",
                                    fontSize = 10.sp,
                                    color = if (isUser) Color.White.copy(alpha = 0.75f) else MayaTokens.InkMuted
                                )
                            }
                        }
                    }
                }
            }
        }

        // Attachment chip preview if selected
        if (attachedName != null) {
            Row(
                modifier = Modifier
                    .padding(bottom = 6.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Description, contentDescription = null, tint = MayaTokens.PrimaryBlue, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(attachedName.orEmpty(), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text(
                    text = "✕",
                    modifier = Modifier
                        .clickable {
                            attachedBytes = null
                            attachedName = null
                            attachedMime = null
                        }
                        .padding(4.dp),
                    color = MayaTokens.DangerRed
                )
            }
        }

        MayaComposerPill(
            value = input,
            placeholder = AppStrings.askAnythingPlaceholder(settings.assistantName, settings.language),
            onValueChange = { input = it },
            onAttachClick = {
                fileLauncher.launch(arrayOf("image/*", "application/pdf", "text/*"))
            },
            onSendClick = {
                if (input.isNotBlank() || attachedBytes != null) {
                    val finalPrompt = if (attachedMime == "text/markdown" || attachedMime == "text/plain") {
                        val decodedMd = attachedBytes?.toString(Charsets.UTF_8).orEmpty().take(12000)
                        "${input.ifBlank { "Is document ko samjhao:" }}\n\n--- File (${attachedName}) ---\n$decodedMd"
                    } else input

                    val sendInline = if (attachedMime == "text/markdown" || attachedMime == "text/plain") null else attachedBytes
                    onSendMessage(finalPrompt, sendInline, attachedMime, attachedUriStr, attachedName)
                    input = ""
                    attachedBytes = null
                    attachedMime = null
                    attachedName = null
                    attachedUriStr = null
                }
            },
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }
}
