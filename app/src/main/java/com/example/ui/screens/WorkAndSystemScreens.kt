package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.core.AppLanguage
import com.example.core.AssistantPersonality
import com.example.core.FlavorConfig
import com.example.core.UserSettings
import com.example.data.AssistantRuleEntity
import com.example.data.CallNoteEntity
import com.example.network.MarketQuote
import com.example.ui.components.MayaPrimaryGradientButton
import com.example.ui.components.NeumorphicCard
import com.example.ui.theme.MayaTokens

@Composable
fun MarketsScreen(
    quotes: List<MarketQuote>,
    isLoading: Boolean,
    error: String,
    onRefresh: () -> Unit,
    onAskGeminiMarketBrief: () -> Unit
) {
    LaunchedEffect(Unit) {
        if (quotes.isEmpty()) onRefresh()
    }

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
                Text("Live Global Markets (USD)", style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = onRefresh, modifier = Modifier.testTag("refresh_markets_btn")) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh markets")
                }
            }
        }

        if (error.isNotBlank()) {
            item {
                NeumorphicCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Market feed: $error", color = MayaTokens.DangerRed)
                }
            }
        }

        items(quotes, key = { it.symbol }) { q ->
            val up = q.change24hPct >= 0.0
            NeumorphicCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(q.symbol, style = MaterialTheme.typography.titleLarge)
                        Text(q.name, style = MaterialTheme.typography.bodyMedium)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "$${"%,.2f".format(q.priceUsd)}",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (up) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                contentDescription = null,
                                tint = if (up) MayaTokens.MintCheck else MayaTokens.DangerRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "${if (up) "+" else ""}${"%.2f".format(q.change24hPct)}%",
                                color = if (up) MayaTokens.MintCheck else MayaTokens.DangerRed,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        item {
            MayaPrimaryGradientButton(
                text = if (isLoading) "Loading..." else "Maya se Market Summary suno",
                onClick = onAskGeminiMarketBrief,
                testTag = "market_ai_brief_btn"
            )
        }
    }
}

@Composable
fun DocumentsAndPhotosScreen(
    isPhotosMode: Boolean,
    aiResultText: String,
    onAnalyzeMediaOrDoc: (String, ByteArray?, String?) -> Unit
) {
    val context = LocalContext.current
    var queryPrompt by remember { mutableStateOf("") }
    val selectedUris = remember { mutableStateListOf<Pair<Uri, String>>() }

    // Zero-permission Android Photo Picker per Google Play policy
    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 6)
    ) { uris ->
        selectedUris.clear()
        uris.forEach { u -> selectedUris.add(u to "Photo") }
    }

    val docPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            var name = "Document"
            context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && c.moveToFirst()) name = c.getString(idx) ?: name
            }
            selectedUris.clear()
            selectedUris.add(uri to name)
        }
    }

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
                    Text(
                        text = if (isPhotosMode) "Meri Photos — Voice & AI Search" else "Documents — PDF / Markdown / Text Reader",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = if (isPhotosMode) {
                            "Zero-permission Android Photo Picker se photos chuno aur bolo/likho: 'receipt dhoondo', 'isme kya likha hai?'"
                        } else {
                            "PDF, .md ya .txt document kholo aur Maya se uska nichod (summary) ya sawaal poochho."
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    MayaPrimaryGradientButton(
                        text = if (isPhotosMode) "Photos chuno" else "Document chuno (.pdf / .md / .txt)",
                        icon = if (isPhotosMode) Icons.Default.PhotoLibrary else Icons.Default.Description,
                        onClick = {
                            if (isPhotosMode) {
                                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            } else {
                                docPicker.launch(arrayOf("application/pdf", "text/*"))
                            }
                        },
                        testTag = if (isPhotosMode) "pick_photos_btn" else "pick_doc_btn"
                    )
                }
            }
        }

        if (selectedUris.isNotEmpty()) {
            item {
                NeumorphicCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Chuni gayi files (${selectedUris.size})", style = MaterialTheme.typography.titleMedium)
                        selectedUris.forEach { (uri, name) ->
                            if (isPhotosMode) {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(170.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                )
                            } else {
                                Text("📄 $name", style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                        OutlinedTextField(
                            value = queryPrompt,
                            onValueChange = { queryPrompt = it },
                            placeholder = { Text(if (isPhotosMode) "Is photo me kya dhoondhna hai?" else "Is document se kya poochhna hai?") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                val firstUri = selectedUris.first().first
                                val mime = context.contentResolver.getType(firstUri) ?: if (isPhotosMode) "image/jpeg" else "application/pdf"
                                val bytes = context.contentResolver.openInputStream(firstUri)?.use { it.readBytes() }
                                onAnalyzeMediaOrDoc(
                                    queryPrompt.ifBlank { "Iska saaf Hinglish summary aur mukhya baatein batao." },
                                    bytes,
                                    mime
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Gemini se Analyze Karo")
                        }
                    }
                }
            }
        }

        if (aiResultText.isNotBlank()) {
            item {
                NeumorphicCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Maya ka Analysis", style = MaterialTheme.typography.titleLarge)
                        Text(aiResultText, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

@Composable
fun CreatorAndCodingHelperScreen(
    isMarketingMode: Boolean,
    helperOutput: String,
    onGenerateHelper: (String) -> Unit
) {
    var topic by remember { mutableStateOf("") }

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
                    Text(
                        text = if (isMarketingMode) "Marketing & Viral Copy Studio" else "Website / Coding Studio",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = if (isMarketingMode) {
                            "Reels hook, YouTube title/thumbnail ideas, WhatsApp broadcast ya ad copy taiyaar karwao."
                        } else {
                            "Landing page HTML/CSS, Kotlin/Python bug fix, API integration ya regex turant likhwao."
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = topic,
                        onValueChange = { topic = it },
                        placeholder = {
                            Text(if (isMarketingMode) "Product ya video topic likho..." else "Website feature ya code problem likho...")
                        },
                        modifier = Modifier.fillMaxWidth().testTag("helper_prompt_input")
                    )
                    MayaPrimaryGradientButton(
                        text = if (isMarketingMode) "Viral Copy & Hooks Banao" else "Code / Website Taiyaar Karo",
                        onClick = {
                            if (topic.isNotBlank()) {
                                val full = if (isMarketingMode) {
                                    "Create high-converting Hinglish + English marketing hooks, 5 YouTube/Reel titles, and CTA copy for: $topic"
                                } else {
                                    "Write clean, production-ready code with concise Hinglish explanation for: $topic"
                                }
                                onGenerateHelper(full)
                            }
                        }
                    )
                }
            }
        }

        if (helperOutput.isNotBlank()) {
            item {
                NeumorphicCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = helperOutput,
                        style = MaterialTheme.typography.bodyLarge,
                        fontFamily = if (isMarketingMode) null else FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
fun AccessibilityAndWirelessDebugScreen(
    isAccessibilityConnected: Boolean,
    callNotes: List<CallNoteEntity>,
    onTriggerWirelessDebugAutomation: () -> Unit
) {
    val context = LocalContext.current
    val adbCommand = "adb shell pm grant com.myassistant.app android.permission.WRITE_SECURE_SETTINGS"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = MayaTokens.ScreenHorizontalPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        if (!FlavorConfig.isDirectFlavor) {
            item {
                NeumorphicCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Google Play Safe Edition", style = MaterialTheme.typography.titleLarge)
                        Text(
                            FlavorConfig.playLimitationMessage(AppLanguage.HINGLISH),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }

        item {
            NeumorphicCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Accessibility Screen Control", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = if (isAccessibilityConnected) "✅ Maya Accessibility Service ACTIVE hai."
                        else "⚠️ Maya Accessibility abhi band hai.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isAccessibilityConnected) MayaTokens.MintCheck else MayaTokens.WarningAmber
                    )
                    if (FlavorConfig.supportsAccessibilityAutomation) {
                        Button(onClick = {
                            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        }) {
                            Text("Accessibility Settings kholo")
                        }
                    }
                }
            }
        }

        // Part 06: Developer Options & Wireless Debugging + One-time ADB Grant Guide
        item {
            NeumorphicCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Developer Options & Wireless Debugging", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Voice command: \"Developer options on karo, wireless debugging dhoondo.\"\n\n" +
                            "Tarika 1 (Zero PC): Maya Accessibility About Phone khol kar Build Number 7 baar tap karegi aur Wireless Debugging dhoond kar on karegi.\n\n" +
                            "Tarika 2 (Instant Direct Toggle): Ek baar PC se neeche wali ADB command chala do, uske baad Maya Settings.Global se bina screen khole seedha toggle karegi:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = adbCommand,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("adb_grant", adbCommand))
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("ADB Command Copy")
                        }
                        Button(
                            onClick = onTriggerWirelessDebugAutomation,
                            modifier = Modifier.testTag("run_wireless_debug_btn")
                        ) {
                            Text("Abhi Chalu Karo")
                        }
                    }
                }
            }
        }

        if (callNotes.isNotEmpty()) {
            item {
                Text("AI Receptionist Call Summaries", style = MaterialTheme.typography.titleLarge)
            }
            items(callNotes, key = { it.id }) { note ->
                NeumorphicCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text("${note.callerName} (${note.phoneNumber})", style = MaterialTheme.typography.titleMedium)
                        Text(note.summary, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    settings: UserSettings,
    hasApiKey: Boolean,
    onRenameAssistant: (String) -> Unit,
    onSaveApiKey: (String) -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit,
    onPersonalitySelected: (AssistantPersonality) -> Unit,
    onVoiceModeSelected: (String) -> Unit,
    onToggleFloatingOrb: (Boolean) -> Unit,
    onToggleReceptionist: (Boolean, Boolean) -> Unit,
    onUpdateStopWord: (String) -> Unit
) {
    var nameField by remember(settings.assistantName) { mutableStateOf(settings.assistantName) }
    var apiKeyField by remember { mutableStateOf("") }
    var stopWordField by remember(settings.emergencyStopWord) { mutableStateOf(settings.emergencyStopWord) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = MayaTokens.ScreenHorizontalPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Rename Assistant ("Maya" default, configurable)
        item {
            NeumorphicCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Assistant ka Naam & Bhasha", style = MaterialTheme.typography.titleLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = nameField,
                            onValueChange = { nameField = it },
                            label = { Text("Assistant Name") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("settings_name_input")
                        )
                        Button(onClick = { onRenameAssistant(nameField) }, modifier = Modifier.testTag("settings_save_name_btn")) {
                            Text("Save")
                        }
                    }

                    Text("UI & Voice Language", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppLanguage.entries.forEach { lang ->
                            FilterChip(
                                selected = settings.language == lang,
                                onClick = { onLanguageSelected(lang) },
                                label = { Text(lang.displayName) },
                                modifier = Modifier.testTag("lang_chip_${lang.code}")
                            )
                        }
                    }
                }
            }
        }

        // Personality & Voice Engine
        item {
            NeumorphicCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Voice Engine & Personality", style = MaterialTheme.typography.titleLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = settings.voiceEngineMode == "gemini_live",
                            onClick = { onVoiceModeSelected("gemini_live") },
                            label = { Text("Gemini Live WebSocket (16k/24k)") }
                        )
                        FilterChip(
                            selected = settings.voiceEngineMode == "native_tts",
                            onClick = { onVoiceModeSelected("native_tts") },
                            label = { Text("Offline STT + TTS") }
                        )
                    }

                    AssistantPersonality.entries.forEach { p ->
                        FilterChip(
                            selected = settings.personality == p,
                            onClick = { onPersonalitySelected(p) },
                            label = { Text(p.labelHinglish) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Floating Orb Overlay (dusre apps ke upar)", modifier = Modifier.weight(1f))
                        Switch(
                            checked = settings.floatingOrbEnabled,
                            onCheckedChange = onToggleFloatingOrb
                        )
                    }
                }
            }
        }

        // Encrypted Gemini API Key
        item {
            NeumorphicCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Encrypted Gemini API Key", style = MaterialTheme.typography.titleLarge)
                    Text(
                        if (hasApiKey) "✅ Key EncryptedSharedPreferences me save hai."
                        else "⚠️ Abhi koi API key save nahi hai.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = apiKeyField,
                        onValueChange = { apiKeyField = it },
                        placeholder = { Text("AIza... nayi Gemini key paste karo") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("settings_api_key_input")
                    )
                    Button(
                        onClick = {
                            if (apiKeyField.isNotBlank()) {
                                onSaveApiKey(apiKeyField)
                                apiKeyField = ""
                            }
                        },
                        enabled = apiKeyField.isNotBlank()
                    ) {
                        Text("Encrypted Save Karo")
                    }
                }
            }
        }

        // Part 07: Experimental Call Auto-Answer & AI Receptionist Legal Notice
        item {
            NeumorphicCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Experimental Call Receptionist Mode", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Android Hardware Limitation: Zyadatar Android phones par apps dusri taraf ki call line audio seedha capture nahi kar sakti. Isliye Receptionist Mode speakerphone ON karke caller ko announce karta hai ('Main AI assistant bol rahi hoon') aur mic se message sun kar summary save karta hai.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Legal Notice Accept (AI-on-call disclosure)", modifier = Modifier.weight(1f))
                        Switch(
                            checked = settings.legalCallNoticeAccepted,
                            onCheckedChange = { accepted ->
                                onToggleReceptionist(settings.receptionistModeEnabled && accepted, accepted)
                            }
                        )
                    }
                    if (FlavorConfig.supportsAutoAnswerCall) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Auto-Answer & Speaker Receptionist", modifier = Modifier.weight(1f))
                            Switch(
                                checked = settings.receptionistModeEnabled,
                                enabled = settings.legalCallNoticeAccepted,
                                onCheckedChange = { en -> onToggleReceptionist(en, settings.legalCallNoticeAccepted) }
                            )
                        }
                    } else {
                        Text(
                            "Play Edition me Auto-Answer band hai; incoming caller TTS announcement kaam karega.",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }

        // Emergency Stop Word
        item {
            NeumorphicCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Emergency Stop Voice Word", style = MaterialTheme.typography.titleLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = stopWordField,
                            onValueChange = { stopWordField = it },
                            label = { Text("Stop word (default: ruko)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(onClick = { onUpdateStopWord(stopWordField) }) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RulesAndPcSyncScreens(
    isRulesScreen: Boolean,
    assistantName: String,
    rules: List<AssistantRuleEntity>,
    pcSyncEnabled: Boolean,
    runningLanUrl: String?,
    sharedLanClipboard: String,
    onAddRule: (String) -> Unit,
    onToggleRule: (AssistantRuleEntity) -> Unit,
    onDeleteRule: (Long) -> Unit,
    onTogglePcSync: (Boolean) -> Unit,
    onPushClipboardNote: (String) -> Unit
) {
    var newRuleText by remember { mutableStateOf("") }
    var noteForPc by remember { mutableStateOf("") }

    if (isRulesScreen) {
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
                        Text("$assistantName ke strict rules", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Yahan likhe niyam har Gemini Live aur Chat system prompt me sabse upar bheje jaate hain — cheezein jo $assistantName ko kabhi nahi karni chahiye.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        OutlinedTextField(
                            value = newRuleText,
                            onValueChange = { newRuleText = it },
                            placeholder = { Text("e.g. Bina poochhe kisi group me message mat bhejna") },
                            modifier = Modifier.fillMaxWidth().testTag("new_rule_input")
                        )
                        MayaPrimaryGradientButton(
                            text = "Naya Rule Jodo",
                            icon = Icons.Default.Add,
                            onClick = {
                                if (newRuleText.isNotBlank()) {
                                    onAddRule(newRuleText)
                                    newRuleText = ""
                                }
                            },
                            testTag = "add_rule_btn"
                        )
                    }
                }
            }

            items(rules, key = { it.id }) { rule ->
                NeumorphicCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(rule.ruleText, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Switch(checked = rule.isEnabled, onCheckedChange = { onToggleRule(rule) })
                        IconButton(onClick = { onDeleteRule(rule.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete rule", tint = MayaTokens.DangerRed)
                        }
                    }
                }
            }
        }
    } else {
        // PC ⇄ Phone Local Wi-Fi Sync Screen
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
                        Text("PC ⇄ Phone Local Network Bridge", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Opt-in local Wi-Fi sync. Koi cloud server nahi — sirf aapke ghar/office Wi-Fi par PC browser se Maya ko command, text ya backup bhejo.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Local Wi-Fi Server Chalu Karo", modifier = Modifier.weight(1f))
                            Switch(
                                checked = pcSyncEnabled,
                                onCheckedChange = onTogglePcSync,
                                modifier = Modifier.testTag("pc_sync_toggle")
                            )
                        }
                        if (runningLanUrl != null) {
                            Text(
                                text = "PC Browser me kholo: $runningLanUrl",
                                style = MaterialTheme.typography.titleMedium,
                                color = MayaTokens.PrimaryBlueDeep
                            )
                        }
                    }
                }
            }

            item {
                NeumorphicCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Shared Note / Clipboard", style = MaterialTheme.typography.titleMedium)
                        if (sharedLanClipboard.isNotBlank()) {
                            Text("Latest from PC/Phone: $sharedLanClipboard", style = MaterialTheme.typography.bodyLarge)
                        }
                        OutlinedTextField(
                            value = noteForPc,
                            onValueChange = { noteForPc = it },
                            placeholder = { Text("PC ke liye link ya text likho...") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(onClick = {
                            onPushClipboardNote(noteForPc)
                            noteForPc = ""
                        }) {
                            Text("PC Bridge par Update Karo")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PrivacyAboutUpgradeScreen(
    mode: String, // "privacy" | "about" | "upgrade"
    assistantName: String,
    isActivated: Boolean,
    onActivateFreePro: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = MayaTokens.ScreenHorizontalPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        when (mode) {
            "privacy" -> {
                item {
                    NeumorphicCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Privacy Policy & Data Suraksha", style = MaterialTheme.typography.headlineMedium)
                            Text(
                                "1. On-Device Storage: Aapki chat history, Yaadein (learned tasks), routine, aur captured notifications sirf aapke phone ke local Room SQLite database me save rehte hain.\n\n" +
                                    "2. Encrypted Keys: Aapki Gemini API key aur OAuth token Android Keystore AES-256 EncryptedSharedPreferences me lock rehte hain; koi third-party analytics ya telemetry server nahi hai.\n\n" +
                                    "3. Gemini API Calls: Jab aap voice, chat ya camera scan use karte hain, tabhi audio/image/text seedha Google ke official `generativelanguage.googleapis.com` endpoint par aapki apni API key se jaata hai.\n\n" +
                                    "4. Visible Indicators & Emergency Stop: Jab bhi Mic, Camera ya Accessibility active hota hai, upar status badge dikhta hai. Kabhi bhi \"ruko\" bol kar ya notification me \"Ruko (Stop Maya)\" daba kar sab turant band kiya ja sakta hai.\n\n" +
                                    "5. No SEND_SMS: Ye app kabhi bina bataye background SMS nahi bhejti; hamesha safe Android SMS compose intent kholti hai.",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }

            "about" -> {
                item {
                    NeumorphicCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("$assistantName v5.2.42", style = MaterialTheme.typography.headlineMedium)
                            Text(
                                "Package: com.myassistant.app\n" +
                                    "Active Build Flavor: ${FlavorConfig.flavorName.uppercase()}\n" +
                                    "Architecture: Kotlin + Jetpack Compose Material 3 + Coroutines/Flow + Room + DataStore + OkHttp WebSocket (Gemini Live 16kHz/24kHz) + CameraX.",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                "Manual Test Checklist Summary:\n" +
                                    "• Part 01-02: Splash -> 3-step Onboarding -> Encrypted Gemini key & permissions.\n" +
                                    "• Part 03-04: Tap Center Mic Orb -> Speak Hinglish -> Barge-in & 'Pakka bhej doon?' voice confirmation.\n" +
                                    "• Part 05-06: 'YouTube kholo' & 'Developer options on karo, wireless debugging dhoondo'.\n" +
                                    "• Part 07-09: Caller TTS, Screen-off WhatsApp readout, 'aaj ki routine batao'.\n" +
                                    "• Part 10-13: YouTube OAuth analytics, CameraX Scan, Yaadein backup/restore, Multimodal Chat.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            else -> {
                item {
                    NeumorphicCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("$assistantName Unlimited Unlock", style = MaterialTheme.typography.headlineMedium)
                            Text(
                                "Apni khud ki free Google AI Studio Gemini key ke saath saare tools, unlimited voice minutes, YouTube Channel Analyst aur PC ⇄ Phone bridge bilkul muft chalu karo.",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            MayaPrimaryGradientButton(
                                text = if (isActivated) "Activated ✅ (999 Energy)" else "Abhi Free Activate Karo",
                                onClick = onActivateFreePro,
                                enabled = !isActivated,
                                testTag = "upgrade_activate_btn"
                            )
                        }
                    }
                }
            }
        }
    }
}
