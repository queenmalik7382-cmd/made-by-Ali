package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.core.FlavorConfig
import com.example.ui.components.MayaPrimaryGradientButton
import com.example.ui.components.NeumorphicCard
import com.example.ui.theme.MayaTokens
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    assistantName: String,
    onSplashFinished: () -> Unit
) {
    var progressTarget by remember { mutableStateOf(0.12f) }
    val animatedProgress by animateFloatAsState(
        targetValue = progressTarget,
        animationSpec = tween(durationMillis = 1250),
        label = "splash_bar"
    )

    LaunchedEffect(Unit) {
        progressTarget = 1f
        delay(1350)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070F26))
            .testTag("splash_screen")
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_maya_splash),
            contentDescription = assistantName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Bottom gradient scrim for crisp text & animated loading bar
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color(0xCC060D24), Color(0xF5050B1D))
                    )
                )
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 32.dp, vertical = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = assistantName.uppercase(),
                fontSize = 44.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 8.sp,
                color = Color.White
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Ek second... load ho raha hai",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFE0ECFF)
            )
            Spacer(Modifier.height(18.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.14f))
                    .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .height(12.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF38BDF8), Color(0xFF60A5FA), Color(0xFFF472B6))
                            )
                        )
                )
            }
        }
    }
}

@Composable
fun OnboardingWizardScreen(
    assistantName: String,
    initialKeyPresent: Boolean,
    onSaveApiKey: (String) -> Unit,
    onFinishOnboarding: () -> Unit
) {
    var step by remember { mutableIntStateOf(1) }
    var apiKeyInput by remember { mutableStateOf("") }
    var refreshTick by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Refresh permission and background states automatically whenever user returns from Android Settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshTick++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(MayaTokens.BgTop, MayaTokens.BgBottom)))
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        // Header with Maya icon + Step 1/3 progress bars
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFF1E6BFF), Color(0xFF060F29)))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null, tint = Color.White)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "$assistantName set up karo",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Step $step / 3",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MayaTokens.InkSecondary
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (i in 1..3) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(5.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (i <= step) MayaTokens.PrimaryBlue else MayaTokens.ShadowDark)
                    )
                }
            }
        }

        when (step) {
            1 -> OnboardingStep1ApiKey(
                assistantName = assistantName,
                keyValue = apiKeyInput,
                hasSavedKey = initialKeyPresent,
                onKeyChange = { apiKeyInput = it },
                onOpenAiStudio = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey"))
                    context.startActivity(intent)
                },
                onPasteClipboard = {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = cm.primaryClip
                    if (clip != null && clip.itemCount > 0) {
                        apiKeyInput = clip.getItemAt(0).coerceToText(context).toString().trim()
                    }
                },
                onSaveAndContinue = {
                    if (apiKeyInput.isNotBlank()) onSaveApiKey(apiKeyInput)
                    step = 2
                },
                onSkip = { step = 2 }
            )

            2 -> OnboardingStep2Permissions(
                assistantName = assistantName,
                refreshTick = refreshTick,
                onRefresh = { refreshTick++ },
                onNext = { step = 3 }
            )

            else -> OnboardingStep3KeepAlive(
                assistantName = assistantName,
                refreshTick = refreshTick,
                onFinish = onFinishOnboarding
            )
        }
    }
}

@Composable
private fun OnboardingStep1ApiKey(
    assistantName: String,
    keyValue: String,
    hasSavedKey: Boolean,
    onKeyChange: (String) -> Unit,
    onOpenAiStudio: () -> Unit,
    onPasteClipboard: () -> Unit,
    onSaveAndContinue: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Apni Gemini key daalo", style = MaterialTheme.typography.headlineLarge)
        Text(
            text = "$assistantName ki awaaz aur dimaag Google ke Gemini par chalte hain. Key ke bina wo offline mode me basic commands chalayegi. Key bilkul free hai aur lagbhag ek minute lagta hai.",
            style = MaterialTheme.typography.bodyLarge
        )

        StepInstructionLine(1, "Neeche \"Free key lo\" dabao. Google AI Studio khulega.")
        StepInstructionLine(2, "Apne Google account se sign in karo.")
        StepInstructionLine(3, "\"Create API key\" dabao. Koi bhi project chuno. Phir \"Copy\" dabao.")
        StepInstructionLine(4, "$assistantName me wapas aao aur \"Paste karo\" dabao.")

        NeumorphicCard(
            onClick = onOpenAiStudio,
            testTag = "onboarding_get_free_key_btn",
            contentPadding = PaddingValues(vertical = 16.dp, horizontal = 20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = MayaTokens.PrimaryBlueDeep)
                Spacer(Modifier.width(10.dp))
                Text("Free key lo", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }
        }

        // Masked Encrypted API Key Input
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(MayaTokens.SurfaceRecessed)
                .border(1.dp, Color.White, RoundedCornerShape(22.dp))
                .padding(horizontal = 8.dp)
        ) {
            TextField(
                value = keyValue,
                onValueChange = onKeyChange,
                placeholder = {
                    Text(if (hasSavedKey) "Key pehle se encrypted save hai (naya daalo ya aage badho)" else "Apni key yahan daalo")
                },
                visualTransformation = PasswordVisualTransformation(),
                trailingIcon = {
                    Icon(Icons.Default.Lock, contentDescription = "Encrypted", tint = MayaTokens.InkSecondary)
                },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("onboarding_api_key_input")
            )
        }

        NeumorphicCard(
            onClick = onPasteClipboard,
            testTag = "onboarding_paste_key_btn",
            contentPadding = PaddingValues(vertical = 16.dp, horizontal = 20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Key, contentDescription = null, tint = MayaTokens.PrimaryBlueDeep)
                Spacer(Modifier.width(10.dp))
                Text("Paste karo", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }
        }

        Text(
            text = "Key free hai. Ye EncryptedSharedPreferences me surakshit rehti hai aur sirf tumhare phone me save hoti hai.",
            style = MaterialTheme.typography.bodyMedium,
            color = MayaTokens.InkSecondary
        )

        MayaPrimaryGradientButton(
            text = "Save karke aage badho",
            icon = Icons.AutoMirrored.Filled.ArrowForward,
            onClick = onSaveAndContinue,
            enabled = keyValue.isNotBlank() || hasSavedKey,
            testTag = "onboarding_save_key_next"
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onSkip, modifier = Modifier.testTag("onboarding_already_have_key")) {
                Text("Maine key pehle hi daal di hai", color = MayaTokens.InkSecondary)
            }
            TextButton(onClick = onSkip, modifier = Modifier.testTag("onboarding_skip_key")) {
                Text("Abhi skip karo", color = MayaTokens.InkSecondary)
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun StepInstructionLine(number: Int, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(Color(0xFFCDE1FF)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number.toString(),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MayaTokens.PrimaryBlueDeep
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(text = text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
    }
}

private data class RuntimePermRow(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val permissions: List<String>
)

@Composable
private fun OnboardingStep2Permissions(
    assistantName: String,
    refreshTick: Int,
    onRefresh: () -> Unit,
    onNext: () -> Unit
) {
    val context = LocalContext.current
    val singlePermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { onRefresh() }

    val rows = remember {
        buildList {
            add(
                RuntimePermRow(
                    "mic",
                    "Microphone",
                    "Taaki tum $assistantName se baat kar sako. Zaroori hai.",
                    Icons.Default.Mic,
                    listOf(Manifest.permission.RECORD_AUDIO)
                )
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(
                    RuntimePermRow(
                        "notifs",
                        "Notifications",
                        "$assistantName ki apni notification, jo use background me chalu rakhti hai.",
                        Icons.Default.Notifications,
                        listOf(Manifest.permission.POST_NOTIFICATIONS)
                    )
                )
            }
            add(
                RuntimePermRow(
                    "camera",
                    "Camera",
                    "Taaki wo photo le sake ya jo tum phone se dikhao wo dekh sake.",
                    Icons.Default.CameraAlt,
                    listOf(Manifest.permission.CAMERA)
                )
            )
            add(
                RuntimePermRow(
                    "calls",
                    "Phone calls",
                    "Taaki wo tumhare liye call laga sake.",
                    Icons.Default.Call,
                    listOf(Manifest.permission.CALL_PHONE, Manifest.permission.READ_PHONE_STATE)
                )
            )
            add(
                RuntimePermRow(
                    "contacts",
                    "Contacts",
                    "Taaki sirf naam kaafi ho — number wo khud dhoondh legi.",
                    Icons.Default.Contacts,
                    listOf(Manifest.permission.READ_CONTACTS)
                )
            )
            add(
                RuntimePermRow(
                    "sms_intent",
                    "SMS (Safe Compose Intent)",
                    "Bina SEND_SMS permission ke tumhare SMS app me message taiyaar kar degi.",
                    Icons.Default.Sms,
                    emptyList() // Always ready via zero-permission Intent.ACTION_SENDTO per strict policy
                )
            )
            add(
                RuntimePermRow(
                    "location",
                    "Location",
                    "Mausam, raasta, aur tum kahan ho.",
                    Icons.Default.LocationOn,
                    listOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
                )
            )
            add(
                RuntimePermRow(
                    "gallery",
                    "Gallery aur files",
                    "Zero-permission Android Photo Picker aur Document Picker se surakshit file chuno.",
                    Icons.Default.PhotoLibrary,
                    emptyList()
                )
            )
            if (FlavorConfig.supportsAutoAnswerCall) {
                add(
                    RuntimePermRow(
                        "answer_calls",
                        "Call uthana aur sambhalna",
                        "Taaki wo bata sake kaun call kar raha hai aur call utha ya kaat sake.",
                        Icons.Default.HeadsetMic,
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            listOf(Manifest.permission.ANSWER_PHONE_CALLS, Manifest.permission.READ_PHONE_STATE)
                        } else listOf(Manifest.permission.READ_PHONE_STATE)
                    )
                )
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(
                    RuntimePermRow(
                        "bt",
                        "Bluetooth",
                        "Taaki uski awaaz tumhare earbuds ya speaker par aaye.",
                        Icons.Default.Bluetooth,
                        listOf(Manifest.permission.BLUETOOTH_CONNECT)
                    )
                )
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("$assistantName ko apna phone use karne do", style = MaterialTheme.typography.headlineLarge)
            Text(
                text = "Kisi bhi row par tap karo — Android ek ek karke permission poochega. Sirf microphone zaroori hai; baaki se wo aur kaam ki banti hai.",
                style = MaterialTheme.typography.bodyLarge
            )

            rows.forEach { row ->
                val granted = remember(refreshTick, row.id) {
                    row.permissions.isEmpty() || row.permissions.all { p ->
                        ContextCompat.checkSelfPermission(context, p) == PackageManager.PERMISSION_GRANTED
                    }
                }
                PermissionChecklistCard(
                    title = row.title,
                    subtitle = row.subtitle,
                    icon = row.icon,
                    isDone = granted,
                    testTag = "perm_row_${row.id}",
                    onClick = {
                        if (row.permissions.isNotEmpty()) {
                            singlePermLauncher.launch(row.permissions.toTypedArray())
                        }
                    }
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            MayaPrimaryGradientButton(
                text = "Aage badho",
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                onClick = onNext,
                testTag = "onboarding_step2_next"
            )
        }
    }
}

@Composable
private fun OnboardingStep3KeepAlive(
    assistantName: String,
    refreshTick: Int,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager

    val batteryDone = remember(refreshTick) { pm.isIgnoringBatteryOptimizations(context.packageName) }
    val overlayDone = remember(refreshTick) { Settings.canDrawOverlays(context) }
    val notifAccessDone = remember(refreshTick) {
        val enabled = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners").orEmpty()
        enabled.contains(context.packageName)
    }
    val accessibilityDone = remember(refreshTick) {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ).orEmpty()
        enabled.contains(context.packageName)
    }
    val assistantRoleDone = remember(refreshTick) {
        val defAssist = Settings.Secure.getString(context.contentResolver, "assistant").orEmpty()
        defAssist.contains(context.packageName)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("$assistantName ko background me zinda rakho", style = MaterialTheme.typography.headlineLarge)
            Text(
                text = "Battery zaroori hai — isi se screen off hone par wo chup nahi hoti. Baaki optional hain par unse wo kahin zyada kaam ki ho jaati hai.",
                style = MaterialTheme.typography.bodyLarge
            )

            if (FlavorConfig.isDirectFlavor) {
                PermissionChecklistCard(
                    title = "Battery — optimisation band",
                    subtitle = "Zaroori hai. Iske bina phone background me $assistantName ko band kar deta hai. Tap karo, phir Allow chuno.",
                    icon = Icons.Default.BatteryChargingFull,
                    isDone = batteryDone,
                    testTag = "keepalive_battery",
                    onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                    Uri.parse("package:${context.packageName}")
                                )
                            )
                        }.onFailure {
                            context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                        }
                    }
                )
            }

            PermissionChecklistCard(
                title = "Dusre apps ke upar dikhna",
                subtitle = "Taaki uska floating orb tum jo bhi kar rahe ho uske upar tair sake.",
                icon = Icons.Default.Layers,
                isDone = overlayDone,
                testTag = "keepalive_overlay",
                onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                    )
                }
            )

            PermissionChecklistCard(
                title = "Notification access",
                subtitle = "WhatsApp messages padhne aur kaun message bhej raha hai ye jaanne ke liye. List me \"$assistantName\" on karo.",
                icon = Icons.Default.Notifications,
                isDone = notifAccessDone,
                testTag = "keepalive_notif_listener",
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }
            )

            if (FlavorConfig.supportsAccessibilityAutomation) {
                PermissionChecklistCard(
                    title = "Accessibility service",
                    subtitle = "Taaki wo apps khol sake, tap kar sake aur tumhare liye screen padh sake. List me \"$assistantName\" on karo.",
                    icon = Icons.Default.AccessibilityNew,
                    isDone = accessibilityDone,
                    testTag = "keepalive_accessibility",
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                )
            }

            PermissionChecklistCard(
                title = "Default assistant",
                subtitle = "Power button long-press karo ya kone se swipe karo aur use bulao, lock screen par bhi. \"$assistantName\" chuno.",
                icon = Icons.Default.HeadsetMic,
                isDone = assistantRoleDone,
                testTag = "keepalive_default_assistant",
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_VOICE_INPUT_SETTINGS))
                }
            )

            Text(
                text = "Kisi bhi pending row par tap karo, uski system screen khulegi — kaam hone par yahan wapas aao, status apne aap refresh ho jayega.",
                style = MaterialTheme.typography.bodyMedium,
                color = MayaTokens.InkSecondary
            )
            Spacer(Modifier.height(10.dp))
        }

        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            MayaPrimaryGradientButton(
                text = "Ho gaya",
                icon = Icons.Default.Check,
                onClick = onFinish,
                testTag = "onboarding_finish_button"
            )
        }
    }
}

@Composable
fun PermissionChecklistCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isDone: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    NeumorphicCard(
        onClick = onClick,
        testTag = testTag,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (isDone) MayaTokens.MintBadgeBg else Color(0xFFDCE8FC)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (isDone) MayaTokens.MintCheck else MayaTokens.PrimaryBlueDeep,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Spacer(Modifier.height(3.dp))
                Text(text = subtitle, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.width(10.dp))
            if (isDone) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Ho gaya",
                    tint = MayaTokens.MintCheck,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text(
                    text = "Baaki hai",
                    style = MaterialTheme.typography.labelSmall,
                    color = MayaTokens.PrimaryBlueDeep
                )
            }
        }
    }
}
