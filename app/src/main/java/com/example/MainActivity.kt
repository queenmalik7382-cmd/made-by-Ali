package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.MainViewModel
import com.example.ui.components.DangerousToolConfirmationBanner
import com.example.ui.components.MayaBottomBarWithOrb
import com.example.ui.components.MayaNavigationDrawerContent
import com.example.ui.components.MayaTopHeader
import com.example.ui.screens.AccessibilityAndWirelessDebugScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.CreatorAndCodingHelperScreen
import com.example.ui.screens.DocumentsAndPhotosScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MarketsScreen
import com.example.ui.screens.NotificationsCenterScreen
import com.example.ui.screens.OnboardingWizardScreen
import com.example.ui.screens.PrivacyAboutUpgradeScreen
import com.example.ui.screens.RoutineScreen
import com.example.ui.screens.RulesAndPcSyncScreens
import com.example.ui.screens.ScanScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.YaadeinScreen
import com.example.ui.screens.YouTubeChannelAnalystScreen
import com.example.ui.theme.MayaTokens
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        val app = application as MayaApplication
        MainViewModel.Factory(app, app.container)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIncomingIntent(intent)

        setContent {
            MyApplicationTheme {
                MayaRootApp(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.action == Intent.ACTION_ASSIST ||
            intent.action == "android.speech.action.VOICE_SEARCH_HANDS_FREE" ||
            intent.getBooleanExtra("auto_start_voice", false)
        ) {
            viewModel.startVoiceFromAssistIntent()
        }
        intent.data?.let { uri ->
            if (uri.scheme == "com.myassistant.app" && uri.host == "oauth2redirect") {
                viewModel.handleOAuthRedirectUri(uri)
            }
        }
    }
}

@Composable
fun MayaRootApp(viewModel: MainViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val hasApiKey by viewModel.hasApiKey.collectAsStateWithLifecycle()
    var showSplash by remember { mutableStateOf(true) }

    if (showSplash) {
        SplashScreen(
            assistantName = settings.assistantName,
            onSplashFinished = { showSplash = false }
        )
        return
    }

    if (!settings.onboardingCompleted) {
        OnboardingWizardScreen(
            assistantName = settings.assistantName,
            initialKeyPresent = hasApiKey,
            onSaveApiKey = viewModel::saveApiKey,
            onFinishOnboarding = viewModel::completeOnboarding
        )
        return
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: "home"
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val orbState by viewModel.orbState.collectAsStateWithLifecycle()
    val liveTranscript by viewModel.liveTranscript.collectAsStateWithLifecycle()
    val isMicActive by viewModel.micActiveIndicator.collectAsStateWithLifecycle()
    val isCameraActive by viewModel.isCameraActive.collectAsStateWithLifecycle()
    val isAccessibilityActive by viewModel.isAccessibilityConnected.collectAsStateWithLifecycle()
    val pendingConfirm by viewModel.pendingConfirmation.collectAsStateWithLifecycle()

    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val routineTasks by viewModel.routineTasks.collectAsStateWithLifecycle()
    val learnedTasks by viewModel.learnedTasks.collectAsStateWithLifecycle()
    val memories by viewModel.memories.collectAsStateWithLifecycle()
    val rules by viewModel.rules.collectAsStateWithLifecycle()
    val announcements by viewModel.announcements.collectAsStateWithLifecycle()
    val capturedNotifications by viewModel.capturedNotifications.collectAsStateWithLifecycle()
    val callNotes by viewModel.callNotes.collectAsStateWithLifecycle()

    val weatherLabel by viewModel.weatherLabel.collectAsStateWithLifecycle()
    val visionResult by viewModel.visionResult.collectAsStateWithLifecycle()
    val isAnalyzingVision by viewModel.isAnalyzingVision.collectAsStateWithLifecycle()
    val isRecordingMacro by viewModel.isRecordingMacro.collectAsStateWithLifecycle()
    val recordedSteps by viewModel.recordedMacroSteps.collectAsStateWithLifecycle()
    val backupStatus by viewModel.backupStatus.collectAsStateWithLifecycle()

    val marketQuotes by viewModel.marketQuotes.collectAsStateWithLifecycle()
    val marketLoading by viewModel.marketLoading.collectAsStateWithLifecycle()
    val marketError by viewModel.marketError.collectAsStateWithLifecycle()
    val hasYtToken by viewModel.hasYouTubeToken.collectAsStateWithLifecycle()
    val ytMetrics by viewModel.youtubeMetrics.collectAsStateWithLifecycle()
    val ytAdvice by viewModel.youtubeAiAdvice.collectAsStateWithLifecycle()
    val ytError by viewModel.youtubeError.collectAsStateWithLifecycle()
    val ytLoading by viewModel.isYouTubeLoading.collectAsStateWithLifecycle()
    val notifSmartSummary by viewModel.notificationSmartSummary.collectAsStateWithLifecycle()
    val docOrPhotoResult by viewModel.docOrPhotoAnalysis.collectAsStateWithLifecycle()
    val studioOutput by viewModel.helperStudioOutput.collectAsStateWithLifecycle()
    val runningLanUrl by viewModel.runningLanUrl.collectAsStateWithLifecycle()
    val sharedLanClipboard by viewModel.sharedLanClipboard.collectAsStateWithLifecycle()

    val primaryBottomRoutes = setOf("home", "scan", "yaadein", "chat")
    val isSubScreen = currentRoute !in primaryBottomRoutes

    fun navigateSingleTop(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    if (drawerState.isOpen) {
        BackHandler { scope.launch { drawerState.close() } }
    }

    val headerTitle = when (currentRoute) {
        "home" -> settings.assistantName
        "scan" -> "Scanner"
        "yaadein" -> "Yaadein"
        "chat" -> "Chat"
        "routine" -> "Meri Routine"
        "notifications" -> "Notifications"
        "youtube_channel" -> "Channel Analyst"
        "markets" -> "Markets"
        "marketing" -> "Marketing"
        "documents" -> "Documents"
        "photos" -> "Meri Photos"
        "coding" -> "Website / Coding"
        "accessibility_hub" -> "Accessibility"
        "settings" -> "Settings"
        "rules" -> "${settings.assistantName} ke rules"
        "pc_sync" -> "PC ⇄ Phone"
        "privacy" -> "Privacy Policy"
        "about" -> "About"
        "upgrade" -> "Upgrade"
        else -> settings.assistantName
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            MayaNavigationDrawerContent(
                assistantName = settings.assistantName,
                currentRoute = currentRoute,
                hasUnreadNotifications = announcements.isNotEmpty() || capturedNotifications.isNotEmpty(),
                onSelectRoute = { route ->
                    scope.launch { drawerState.close() }
                    navigateSingleTop(route)
                }
            )
        }
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            containerColor = Color.Transparent,
            topBar = {
                MayaTopHeader(
                    title = headerTitle,
                    showBackInsteadOfMenu = isSubScreen,
                    hasUnreadNotifications = announcements.isNotEmpty() || capturedNotifications.isNotEmpty(),
                    isMicActive = isMicActive,
                    isCameraActive = isCameraActive,
                    isAccessibilityActive = isAccessibilityActive,
                    onLeftClick = {
                        if (isSubScreen) {
                            if (!navController.popBackStack()) navigateSingleTop("home")
                        } else {
                            scope.launch { drawerState.open() }
                        }
                    },
                    onBellClick = { navigateSingleTop("notifications") }
                )
            },
            bottomBar = {
                if (!isSubScreen) {
                    MayaBottomBarWithOrb(
                        currentRoute = currentRoute,
                        orbState = orbState,
                        language = settings.language,
                        onNavigate = { navigateSingleTop(it) },
                        onOrbClick = viewModel::toggleVoiceOrb
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(MayaTokens.BgTop, MayaTokens.BgBottom)))
                    .padding(innerPadding)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    pendingConfirm?.let { p ->
                        DangerousToolConfirmationBanner(
                            pending = p,
                            onApprove = { viewModel.approveOrCancelPendingTool(true) },
                            onCancel = { viewModel.approveOrCancelPendingTool(false) }
                        )
                    }

                    NavHost(
                        navController = navController,
                        startDestination = "home",
                        modifier = Modifier.weight(1f)
                    ) {
                        composable("home") {
                            HomeScreen(
                                settings = settings,
                                orbState = orbState,
                                weatherLabel = weatherLabel,
                                liveTranscript = liveTranscript,
                                onSendPrompt = viewModel::sendChatOrVoicePrompt,
                                onQuickActionPrompt = { q ->
                                    viewModel.sendChatOrVoicePrompt(q)
                                    navigateSingleTop("chat")
                                },
                                onNavigateUpgrade = { navigateSingleTop("upgrade") },
                                onNavigateChat = { navigateSingleTop("chat") }
                            )
                        }
                        composable("scan") {
                            ScanScreen(
                                assistantName = settings.assistantName,
                                visionAnalysisResult = visionResult,
                                isAnalyzingVision = isAnalyzingVision,
                                onCameraActiveChanged = viewModel::setCameraActive,
                                onAnalyzeFrame = viewModel::analyzeCameraFrame
                            )
                        }
                        composable("yaadein") {
                            YaadeinScreen(
                                learnedTasks = learnedTasks,
                                memories = memories,
                                isRecordingMacro = isRecordingMacro,
                                recordedSteps = recordedSteps,
                                backupStatusMsg = backupStatus,
                                onRunTask = viewModel::runLearnedTask,
                                onSaveRecordedTask = viewModel::saveRecordedTask,
                                onUpdateTaskSteps = viewModel::updateTaskSteps,
                                onDeleteTask = viewModel::deleteLearnedTask,
                                onAddMemory = viewModel::addMemory,
                                onDeleteMemory = viewModel::deleteMemory,
                                onExportBackupToUri = viewModel::exportBackup,
                                onImportBackupFromUri = viewModel::importBackup
                            )
                        }
                        composable("chat") {
                            ChatScreen(
                                settings = settings,
                                messages = messages,
                                orbState = orbState,
                                onSendMessage = viewModel::sendChatOrVoicePrompt,
                                onClearChat = viewModel::clearChat
                            )
                        }
                        composable("routine") {
                            RoutineScreen(
                                routineTasks = routineTasks,
                                onAddRoutine = viewModel::addRoutine,
                                onCompleteRoutine = viewModel::completeRoutine,
                                onSkipOrMoveRoutine = viewModel::moveRoutineByMinutes,
                                onDeleteRoutine = viewModel::deleteRoutine,
                                onSpeakTodaySummary = viewModel::speakRoutineSummary
                            )
                        }
                        composable("notifications") {
                            NotificationsCenterScreen(
                                settings = settings,
                                announcements = announcements,
                                capturedNotifications = capturedNotifications,
                                geminiSmartSummary = notifSmartSummary,
                                onClearAll = viewModel::clearNotificationsAndFeed,
                                onSummarizeWithGemini = viewModel::summarizeNotificationsWithGemini,
                                onUpdateNotificationSettings = viewModel::updateNotificationPrefs
                            )
                        }
                        composable("youtube_channel") {
                            YouTubeChannelAnalystScreen(
                                clientId = viewModel.getGoogleClientId(),
                                hasToken = hasYtToken,
                                metrics = ytMetrics,
                                aiCoachAdvice = ytAdvice,
                                errorMessage = ytError,
                                isLoading = ytLoading,
                                onSaveClientIdAndToken = viewModel::saveYouTubeCredentials,
                                onRefreshAndAnalyze = viewModel::analyzeYouTubeChannel
                            )
                        }
                        composable("markets") {
                            MarketsScreen(
                                quotes = marketQuotes,
                                isLoading = marketLoading,
                                error = marketError,
                                onRefresh = viewModel::refreshMarkets,
                                onAskGeminiMarketBrief = viewModel::speakMarketBrief
                            )
                        }
                        composable("documents") {
                            DocumentsAndPhotosScreen(
                                isPhotosMode = false,
                                aiResultText = docOrPhotoResult,
                                onAnalyzeMediaOrDoc = viewModel::analyzeDocOrPhoto
                            )
                        }
                        composable("photos") {
                            DocumentsAndPhotosScreen(
                                isPhotosMode = true,
                                aiResultText = docOrPhotoResult,
                                onAnalyzeMediaOrDoc = viewModel::analyzeDocOrPhoto
                            )
                        }
                        composable("marketing") {
                            CreatorAndCodingHelperScreen(
                                isMarketingMode = true,
                                helperOutput = studioOutput,
                                onGenerateHelper = viewModel::runStudioHelper
                            )
                        }
                        composable("coding") {
                            CreatorAndCodingHelperScreen(
                                isMarketingMode = false,
                                helperOutput = studioOutput,
                                onGenerateHelper = viewModel::runStudioHelper
                            )
                        }
                        composable("accessibility_hub") {
                            AccessibilityAndWirelessDebugScreen(
                                isAccessibilityConnected = isAccessibilityActive,
                                callNotes = callNotes,
                                onTriggerWirelessDebugAutomation = viewModel::triggerWirelessDebugging
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                settings = settings,
                                hasApiKey = hasApiKey,
                                onRenameAssistant = viewModel::renameAssistant,
                                onSaveApiKey = viewModel::saveApiKey,
                                onLanguageSelected = viewModel::setLanguage,
                                onPersonalitySelected = viewModel::setPersonality,
                                onVoiceModeSelected = viewModel::setVoiceMode,
                                onToggleFloatingOrb = viewModel::setFloatingOrb,
                                onToggleReceptionist = viewModel::setReceptionistMode,
                                onUpdateStopWord = viewModel::setEmergencyStopWord
                            )
                        }
                        composable("rules") {
                            RulesAndPcSyncScreens(
                                isRulesScreen = true,
                                assistantName = settings.assistantName,
                                rules = rules,
                                pcSyncEnabled = settings.pcSyncEnabled,
                                runningLanUrl = runningLanUrl,
                                sharedLanClipboard = sharedLanClipboard,
                                onAddRule = viewModel::addRule,
                                onToggleRule = viewModel::toggleRule,
                                onDeleteRule = viewModel::deleteRule,
                                onTogglePcSync = viewModel::togglePcSync,
                                onPushClipboardNote = viewModel::updatePcNote
                            )
                        }
                        composable("pc_sync") {
                            RulesAndPcSyncScreens(
                                isRulesScreen = false,
                                assistantName = settings.assistantName,
                                rules = rules,
                                pcSyncEnabled = settings.pcSyncEnabled,
                                runningLanUrl = runningLanUrl,
                                sharedLanClipboard = sharedLanClipboard,
                                onAddRule = viewModel::addRule,
                                onToggleRule = viewModel::toggleRule,
                                onDeleteRule = viewModel::deleteRule,
                                onTogglePcSync = viewModel::togglePcSync,
                                onPushClipboardNote = viewModel::updatePcNote
                            )
                        }
                        composable("privacy") {
                            PrivacyAboutUpgradeScreen(
                                mode = "privacy",
                                assistantName = settings.assistantName,
                                isActivated = settings.licenseActivated,
                                onActivateFreePro = viewModel::activateUnlimitedFree
                            )
                        }
                        composable("about") {
                            PrivacyAboutUpgradeScreen(
                                mode = "about",
                                assistantName = settings.assistantName,
                                isActivated = settings.licenseActivated,
                                onActivateFreePro = viewModel::activateUnlimitedFree
                            )
                        }
                        composable("upgrade") {
                            PrivacyAboutUpgradeScreen(
                                mode = "upgrade",
                                assistantName = settings.assistantName,
                                isActivated = settings.licenseActivated,
                                onActivateFreePro = viewModel::activateUnlimitedFree
                            )
                        }
                    }
                }
            }
        }
    }
}
