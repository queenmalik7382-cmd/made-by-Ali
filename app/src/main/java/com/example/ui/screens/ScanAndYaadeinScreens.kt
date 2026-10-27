package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.core.FlavorConfig
import com.example.data.LearnedTaskEntity
import com.example.data.UserMemoryEntity
import com.example.service.MayaAccessibilityService
import com.example.service.RecordedActionStep
import com.example.ui.components.MayaPrimaryGradientButton
import com.example.ui.components.NeumorphicCard
import com.example.ui.theme.MayaTokens
import kotlinx.coroutines.delay

@Composable
fun ScanScreen(
    assistantName: String,
    visionAnalysisResult: String,
    isAnalyzingVision: Boolean,
    onCameraActiveChanged: (Boolean) -> Unit,
    onAnalyzeFrame: (Bitmap, String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var useFrontCamera by remember { mutableStateOf(false) }
    var selectedVisionMode by remember { mutableStateOf("Ye kya hai?") }
    var isVideoSampling by remember { mutableStateOf(false) }
    val previewView = remember { PreviewView(context) }

    val camPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPerm = granted
    }

    DisposableEffect(hasCameraPerm) {
        onCameraActiveChanged(hasCameraPerm)
        onDispose { onCameraActiveChanged(false) }
    }

    // Bind CameraX Preview whenever camera permission or front/back lens changes
    LaunchedEffect(hasCameraPerm, useFrontCamera) {
        if (!hasCameraPerm) return@LaunchedEffect
        runCatching {
            val providerFuture = ProcessCameraProvider.getInstance(context)
            providerFuture.addListener({
                runCatching {
                    val provider = providerFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                    val selector = if (useFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
                    provider.unbindAll()
                    provider.bindToLifecycle(lifecycleOwner, selector, preview)
                }
            }, ContextCompat.getMainExecutor(context))
        }
    }

    // Continuous video frame sampling mode when REC is toggled on
    LaunchedEffect(isVideoSampling) {
        while (isVideoSampling && hasCameraPerm) {
            previewView.bitmap?.let { bmp ->
                onAnalyzeFrame(bmp, "Live video feed dekh kar chhota real-time update do: $selectedVisionMode")
            }
            delay(5500)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A1222))
    ) {
        if (hasCameraPerm) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(28.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$assistantName Vision ke liye Camera chahiye",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Koi bhi cheez dikhao, text padhwao, ya translate karwao.",
                    color = Color(0xFFBBD0F5)
                )
                Spacer(Modifier.height(20.dp))
                MayaPrimaryGradientButton(
                    text = "Camera Permission Do",
                    onClick = { camPermLauncher.launch(Manifest.permission.CAMERA) },
                    testTag = "grant_camera_button"
                )
            }
        }

        // Sci-Fi HUD Reticle & Corner Brackets matching screenshot
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height * 0.44f
            val r = size.minDimension * 0.30f

            drawCircle(
                color = Color(0xFF38BDF8).copy(alpha = 0.55f),
                radius = r,
                center = Offset(cx, cy),
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(22f, 18f), 0f)
                )
            )
            drawArc(
                color = Color(0xFF60A5FA),
                startAngle = -35f,
                sweepAngle = 70f,
                useCenter = false,
                topLeft = Offset(cx - r * 0.8f, cy - r * 0.8f),
                size = androidx.compose.ui.geometry.Size(r * 1.6f, r * 1.6f),
                style = Stroke(width = 3.dp.toPx())
            )
            drawArc(
                color = Color(0xFF60A5FA),
                startAngle = 145f,
                sweepAngle = 70f,
                useCenter = false,
                topLeft = Offset(cx - r * 0.8f, cy - r * 0.8f),
                size = androidx.compose.ui.geometry.Size(r * 1.6f, r * 1.6f),
                style = Stroke(width = 3.dp.toPx())
            )
        }

        // Top HUD Status Overlay
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "● ${assistantName.uppercase()} VISION",
                    color = Color(0xFF38BDF8),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = if (useFrontCamera) "FRONT CAM" else "BACK CAM",
                    color = Color(0xFF60A5FA),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (isAnalyzingVision) "> ANALYZING FRAME WITH GEMINI MULTIMODAL..."
                else "> STANDBY — shutter dabao, $assistantName ko aankhein do",
                color = Color(0xFF93C5FD),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )

            // Vision Prompt Mode Chips
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Ye kya hai?", "Text padho", "Hindi/Urdu Translate", "Detail batao").forEach { mode ->
                    val active = selectedVisionMode == mode
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (active) Color(0xCC2563EB) else Color(0x770F172A))
                            .border(1.dp, Color(0x8838BDF8), RoundedCornerShape(14.dp))
                            .clickable { selectedVisionMode = mode }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(mode, color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }

        // Multimodal Result Card + Bottom Shutter/Flip/Rec Controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (visionAnalysisResult.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xDD0B152B))
                        .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(20.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = visionAnalysisResult,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 6
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // FLIP Camera
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .testTag("scan_flip_camera")
                            .clip(CircleShape)
                            .background(Color(0xAA0F172A))
                            .border(1.dp, Color(0xFF38BDF8), CircleShape)
                            .clickable { useFrontCamera = !useFrontCamera },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.FlipCameraAndroid, contentDescription = "Camera palto", tint = Color(0xFF93C5FD))
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("FLIP", color = Color(0xFF60A5FA), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }

                // Glowing Capture Orb
                Box(
                    modifier = Modifier
                        .size(78.dp)
                        .testTag("scan_capture_shutter")
                        .shadow(14.dp, CircleShape, spotColor = Color(0xFF38BDF8))
                        .clip(CircleShape)
                        .background(Brush.radialGradient(listOf(Color.White, Color(0xFF60A5FA), Color(0xFF1D4ED8))))
                        .border(3.dp, Color.White, CircleShape)
                        .clickable {
                            val bmp = previewView.bitmap
                                ?: Bitmap.createBitmap(320, 320, Bitmap.Config.ARGB_8888)
                            val prompt = when (selectedVisionMode) {
                                "Text padho" -> "Is image me likha saara text saaf-saaf padh kar sunao."
                                "Hindi/Urdu Translate" -> "Is image ke text ko asaan Hinglish/Hindi me translate karo."
                                "Detail batao" -> "Is scene ka poora vivaran Hinglish me do."
                                else -> "Ye kya hai? Hinglish me chhota aur saaf jawab do."
                            }
                            onAnalyzeFrame(bmp, prompt)
                        }
                )

                // REC (Live Video Sampling)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .testTag("scan_rec_video")
                            .clip(CircleShape)
                            .background(if (isVideoSampling) Color(0xCCDC2626) else Color(0xAA0F172A))
                            .border(1.dp, Color(0xFF38BDF8), CircleShape)
                            .clickable { isVideoSampling = !isVideoSampling },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isVideoSampling) Icons.Default.Stop else Icons.Default.Videocam,
                            contentDescription = "Live Video Vision",
                            tint = Color.White
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (isVideoSampling) "LIVE ON" else "REC",
                        color = if (isVideoSampling) Color(0xFFF87171) else Color(0xFF60A5FA),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
fun YaadeinScreen(
    learnedTasks: List<LearnedTaskEntity>,
    memories: List<UserMemoryEntity>,
    isRecordingMacro: Boolean,
    recordedSteps: List<RecordedActionStep>,
    backupStatusMsg: String,
    onRunTask: (LearnedTaskEntity, String) -> Unit,
    onSaveRecordedTask: (String, String, List<Pair<String, String>>) -> Unit,
    onUpdateTaskSteps: (LearnedTaskEntity, String) -> Unit,
    onDeleteTask: (Long) -> Unit,
    onAddMemory: (String, String) -> Unit,
    onDeleteMemory: (Long) -> Unit,
    onExportBackupToUri: (Uri) -> Unit,
    onImportBackupFromUri: (Uri) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Sikhe hue tasks, 1 = Long-term Memory
    var taskToRun by remember { mutableStateOf<LearnedTaskEntity?>(null) }
    var promptForTask by remember { mutableStateOf("") }
    var editingTask by remember { mutableStateOf<LearnedTaskEntity?>(null) }
    var editedStepsJson by remember { mutableStateOf("") }

    var showSaveRecordingDialog by remember { mutableStateOf(false) }
    var newTaskName by remember { mutableStateOf("") }
    var newTaskCategory by remember { mutableStateOf("custom") }

    var newMemKey by remember { mutableStateOf("") }
    var newMemVal by remember { mutableStateOf("") }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> if (uri != null) onExportBackupToUri(uri) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) onImportBackupFromUri(uri) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = MayaTokens.ScreenHorizontalPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    label = { Text("Sikhe hue tasks (${learnedTasks.size})") },
                    modifier = Modifier.testTag("yaadein_tab_tasks")
                )
                FilterChip(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    label = { Text("Meri Yaadein (${memories.size})") },
                    modifier = Modifier.testTag("yaadein_tab_memories")
                )
            }
        }

        if (selectedTab == 0) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Sikhe hue tasks", style = MaterialTheme.typography.headlineMedium)
                    if (FlavorConfig.supportsAccessibilityAutomation) {
                        Button(
                            onClick = {
                                if (isRecordingMacro) {
                                    MayaAccessibilityService.stopTaskRecording()
                                    showSaveRecordingDialog = true
                                } else {
                                    MayaAccessibilityService.startTaskRecording()
                                }
                            },
                            modifier = Modifier.testTag("record_macro_button")
                        ) {
                            Icon(
                                if (isRecordingMacro) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(if (isRecordingMacro) "Save (${recordedSteps.size})" else "Naya sikhao")
                        }
                    }
                }
            }

            items(learnedTasks, key = { it.id }) { task ->
                NeumorphicCard(
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "learned_task_${task.id}"
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = task.name,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "${task.stepCount} steps  •  ${task.categoryTag}  •  ${if (task.isBuiltIn) "built-in" else "sikha hua"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MayaTokens.PrimaryBlueDeep
                            )
                        }

                        IconButton(onClick = {
                            editingTask = task
                            editedStepsJson = task.stepsJson
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = "Steps edit karo", tint = MayaTokens.InkSecondary)
                        }

                        if (!task.isBuiltIn) {
                            IconButton(onClick = { onDeleteTask(task.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete task", tint = MayaTokens.DangerRed)
                            }
                        }

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    promptForTask = ""
                                    taskToRun = task
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Chalao",
                                tint = MayaTokens.PrimaryBlueDeep,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "Chalao",
                                style = MaterialTheme.typography.labelLarge,
                                color = MayaTokens.PrimaryBlueDeep
                            )
                        }
                    }
                }
            }
        } else {
            // Long-Term User Facts & Preferences (injected into Gemini System Prompt)
            item {
                NeumorphicCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Nayi baat yaad dilao", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Jo yahan likhoge (jaise 'Mera bhai Ali hai', 'Mujhe bina shakkar ki chai pasand hai'), Maya har baatcheet me yaad rakhegi.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        OutlinedTextField(
                            value = newMemKey,
                            onValueChange = { newMemKey = it },
                            label = { Text("Topic (e.g. Pasand / Naam)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("memory_key_input")
                        )
                        OutlinedTextField(
                            value = newMemVal,
                            onValueChange = { newMemVal = it },
                            label = { Text("Baat jo yaad rakhni hai") },
                            modifier = Modifier.fillMaxWidth().testTag("memory_val_input")
                        )
                        MayaPrimaryGradientButton(
                            text = "Yaad rakh lo",
                            icon = Icons.Default.Add,
                            onClick = {
                                if (newMemKey.isNotBlank() && newMemVal.isNotBlank()) {
                                    onAddMemory(newMemKey, newMemVal)
                                    newMemKey = ""
                                    newMemVal = ""
                                }
                            },
                            testTag = "save_memory_button"
                        )
                    }
                }
            }

            items(memories, key = { it.id }) { mem ->
                NeumorphicCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(mem.factKey, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            Text(mem.factValue, style = MaterialTheme.typography.bodyLarge)
                        }
                        IconButton(onClick = { onDeleteMemory(mem.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Hatao", tint = MayaTokens.DangerRed)
                        }
                    }
                }
            }
        }

        // Backup & Restore Card matching screenshot
        item {
            NeumorphicCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { exportLauncher.launch("maya_yaadein_backup.json") },
                testTag = "backup_export_card"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = MayaTokens.InkPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Backup & restore", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            Text("Apni yaadein aur chats dusre phone pe le jao", style = MaterialTheme.typography.bodyMedium)
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TextButton(onClick = { exportLauncher.launch("maya_yaadein_backup.json") }) {
                            Text("Backup file save karo")
                        }
                        TextButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/*")) }) {
                            Text("Backup restore karo")
                        }
                    }
                    if (backupStatusMsg.isNotBlank()) {
                        Text(backupStatusMsg, style = MaterialTheme.typography.labelSmall, color = MayaTokens.MintCheck)
                    }
                }
            }
        }
    }

    // Run Task Prompt Dialog
    taskToRun?.let { t ->
        AlertDialog(
            onDismissRequest = { taskToRun = null },
            title = { Text("\"${t.name}\" chalao") },
            text = {
                OutlinedTextField(
                    value = promptForTask,
                    onValueChange = { promptForTask = it },
                    label = { Text("Sawaal ya prompt (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    onRunTask(t, promptForTask)
                    taskToRun = null
                }) { Text("Chalao") }
            },
            dismissButton = {
                TextButton(onClick = { taskToRun = null }) { Text("Ruko") }
            }
        )
    }

    // Edit Task Steps JSON Dialog
    editingTask?.let { t ->
        AlertDialog(
            onDismissRequest = { editingTask = null },
            title = { Text("Steps Edit Karo: ${t.name}") },
            text = {
                OutlinedTextField(
                    value = editedStepsJson,
                    onValueChange = { editedStepsJson = it },
                    label = { Text("Steps JSON") },
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    onUpdateTaskSteps(t, editedStepsJson)
                    editingTask = null
                }) { Text("Save Steps") }
            },
            dismissButton = {
                TextButton(onClick = { editingTask = null }) { Text("Cancel") }
            }
        )
    }

    // Save Newly Recorded Accessibility Macro Dialog
    if (showSaveRecordingDialog) {
        AlertDialog(
            onDismissRequest = { showSaveRecordingDialog = false },
            title = { Text("Naya Sikha Hua Task Save Karo") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Recorded clicks: ${recordedSteps.size} steps")
                    OutlinedTextField(
                        value = newTaskName,
                        onValueChange = { newTaskName = it },
                        label = { Text("Voice command naam (e.g. reel like karo)") }
                    )
                    OutlinedTextField(
                        value = newTaskCategory,
                        onValueChange = { newTaskCategory = it },
                        label = { Text("Category tag") }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val steps = if (recordedSteps.isNotEmpty()) {
                        recordedSteps.map { it.action to it.target }
                    } else {
                        listOf("readScreen" to "summary")
                    }
                    if (newTaskName.isNotBlank()) {
                        onSaveRecordedTask(newTaskName, newTaskCategory, steps)
                        newTaskName = ""
                    }
                    showSaveRecordingDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showSaveRecordingDialog = false }) { Text("Cancel") }
            }
        )
    }
}
