package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.AppContainer
import com.example.core.AppLanguage
import com.example.core.AssistantPersonality
import com.example.core.UserSettings
import com.example.data.AssistantRuleEntity
import com.example.data.LearnedTaskEntity
import com.example.data.RoutineTaskEntity
import com.example.network.MarketQuote
import com.example.network.YouTubeChannelMetrics
import com.example.service.MayaAccessibilityService
import com.example.service.RoutineScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.LocalDate

class MainViewModel(
    application: Application,
    private val container: AppContainer
) : AndroidViewModel(application) {

    val settings: StateFlow<UserSettings> = container.preferences.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    val messages = container.repository.messages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val routineTasks = container.repository.routineTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val learnedTasks = container.repository.learnedTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val memories = container.repository.memories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rules = container.repository.rules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val announcements = container.repository.announcements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val capturedNotifications = container.repository.capturedNotifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val callNotes = container.repository.callNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val orbState = container.voiceEngine.orbState
    val liveTranscript = container.voiceEngine.liveTranscript
    val micActiveIndicator = container.voiceEngine.micActiveIndicator
    val pendingConfirmation = container.voiceEngine.pendingConfirmation

    val isAccessibilityConnected = MayaAccessibilityService.isConnectedFlow
    val isRecordingMacro = MayaAccessibilityService.isRecordingTask
    val recordedMacroSteps = MayaAccessibilityService.recordedSteps

    val runningLanUrl = container.pcLanServer.runningAddress
    val sharedLanClipboard = container.pcLanServer.sharedClipboard

    private val _hasApiKey = MutableStateFlow(container.keyStore.hasGeminiApiKey())
    val hasApiKey: StateFlow<Boolean> = _hasApiKey.asStateFlow()

    private val _hasYouTubeToken = MutableStateFlow(container.keyStore.getYouTubeAccessToken().isNotBlank())
    val hasYouTubeToken: StateFlow<Boolean> = _hasYouTubeToken.asStateFlow()

    private val _weatherLabel = MutableStateFlow("")
    val weatherLabel: StateFlow<String> = _weatherLabel.asStateFlow()

    private val _visionResult = MutableStateFlow("")
    val visionResult: StateFlow<String> = _visionResult.asStateFlow()

    private val _isAnalyzingVision = MutableStateFlow(false)
    val isAnalyzingVision: StateFlow<Boolean> = _isAnalyzingVision.asStateFlow()

    private val _isCameraActive = MutableStateFlow(false)
    val isCameraActive: StateFlow<Boolean> = _isCameraActive.asStateFlow()

    private val _marketQuotes = MutableStateFlow<List<MarketQuote>>(emptyList())
    val marketQuotes: StateFlow<List<MarketQuote>> = _marketQuotes.asStateFlow()

    private val _marketLoading = MutableStateFlow(false)
    val marketLoading: StateFlow<Boolean> = _marketLoading.asStateFlow()

    private val _marketError = MutableStateFlow("")
    val marketError: StateFlow<String> = _marketError.asStateFlow()

    private val _youtubeMetrics = MutableStateFlow<YouTubeChannelMetrics?>(null)
    val youtubeMetrics: StateFlow<YouTubeChannelMetrics?> = _youtubeMetrics.asStateFlow()

    private val _youtubeAiAdvice = MutableStateFlow("")
    val youtubeAiAdvice: StateFlow<String> = _youtubeAiAdvice.asStateFlow()

    private val _youtubeError = MutableStateFlow("")
    val youtubeError: StateFlow<String> = _youtubeError.asStateFlow()

    private val _youtubeLoading = MutableStateFlow(false)
    val youtubeLoading: StateFlow<String> = MutableStateFlow("")
    val isYouTubeLoading = MutableStateFlow(false)

    private val _notificationSmartSummary = MutableStateFlow("")
    val notificationSmartSummary: StateFlow<String> = _notificationSmartSummary.asStateFlow()

    private val _docOrPhotoAnalysis = MutableStateFlow("")
    val docOrPhotoAnalysis: StateFlow<String> = _docOrPhotoAnalysis.asStateFlow()

    private val _helperStudioOutput = MutableStateFlow("")
    val helperStudioOutput: StateFlow<String> = _helperStudioOutput.asStateFlow()

    private val _backupStatus = MutableStateFlow("")
    val backupStatus: StateFlow<String> = _backupStatus.asStateFlow()

    init {
        fetchDefaultWeather()
    }

    private fun fetchDefaultWeather() {
        viewModelScope.launch {
            // New Delhi / South Asia default coordinates if GPS isn't queried yet
            container.publicApisClient.fetchCurrentWeather(28.6139, 77.2090).onSuccess {
                _weatherLabel.value = it
            }
        }
    }

    fun getGoogleClientId(): String = container.keyStore.getGoogleClientId()

    fun saveApiKey(rawKey: String) {
        container.keyStore.saveGeminiApiKey(rawKey)
        _hasApiKey.value = container.keyStore.hasGeminiApiKey()
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            container.preferences.setOnboardingCompleted(true)
        }
    }

    fun toggleVoiceOrb() {
        container.voiceEngine.toggleSession()
    }

    fun startVoiceFromAssistIntent() {
        container.voiceEngine.startVoiceSession()
    }

    fun approveOrCancelPendingTool(approved: Boolean) {
        container.voiceEngine.confirmPendingTool(approved)
    }

    fun sendChatOrVoicePrompt(
        prompt: String,
        inlineBytes: ByteArray? = null,
        inlineMime: String? = null,
        uriStr: String? = null,
        fileName: String? = null
    ) {
        container.voiceEngine.processSpokenOrTextUtterance(
            text = prompt,
            fromVoice = false,
            inlineBytes = inlineBytes,
            inlineMimeType = inlineMime,
            attachmentUri = uriStr,
            attachmentName = fileName
        )
    }

    fun clearChat() {
        viewModelScope.launch { container.repository.clearChatHistory() }
    }

    fun setCameraActive(active: Boolean) {
        _isCameraActive.value = active
    }

    fun analyzeCameraFrame(bitmap: Bitmap, prompt: String) {
        viewModelScope.launch {
            val apiKey = container.keyStore.getGeminiApiKey()
            if (apiKey.isBlank()) {
                _visionResult.value = "Vision chalane ke liye pehle Settings me apni free Gemini API key save karo."
                return@launch
            }
            _isAnalyzingVision.value = true
            val s = settings.value
            val res = container.geminiRestClient.analyzeCameraBitmap(
                apiKey = apiKey,
                bitmap = bitmap,
                prompt = prompt,
                systemInstruction = "You are ${s.assistantName}. Reply concisely in ${s.language.displayName} so it can be read aloud."
            )
            _isAnalyzingVision.value = false
            val text = res.replyText.ifBlank { res.error ?: "Kuchh saaf nahi dikha." }
            _visionResult.value = text
            container.repository.addMessage("assistant", "[Vision] $text", "scan")
            container.voiceEngine.speakViaTts(text)
        }
    }

    // --- Yaadein & Memory ---
    fun runLearnedTask(task: LearnedTaskEntity, promptInput: String) {
        viewModelScope.launch {
            val res = container.toolRegistry.invokeTool(
                "run_learned_task",
                JSONObject().put("taskName", task.name).put("promptInput", promptInput),
                confirmed = true
            )
            container.voiceEngine.speakViaTts(res.spokenFeedback)
        }
    }

    fun saveRecordedTask(name: String, category: String, steps: List<Pair<String, String>>) {
        viewModelScope.launch {
            container.repository.addLearnedTask(name, category, steps)
        }
    }

    fun updateTaskSteps(task: LearnedTaskEntity, newJson: String) {
        viewModelScope.launch {
            container.repository.updateLearnedTask(task.copy(stepsJson = newJson))
        }
    }

    fun deleteLearnedTask(id: Long) {
        viewModelScope.launch { container.repository.deleteLearnedTask(id) }
    }

    fun addMemory(key: String, value: String) {
        viewModelScope.launch { container.repository.saveMemory(key, value) }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch { container.repository.deleteMemory(id) }
    }

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val json = container.repository.exportBackupJson()
                withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openOutputStream(uri)?.use {
                        it.write(json.toByteArray(Charsets.UTF_8))
                    }
                }
                _backupStatus.value = "Backup file safalta se save ho gayi!"
            }.onFailure {
                _backupStatus.value = "Backup error: ${it.localizedMessage}"
            }
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val raw = withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openInputStream(uri)?.use {
                        it.readBytes().toString(Charsets.UTF_8)
                    }.orEmpty()
                }
                val count = container.repository.importBackupJson(raw)
                _backupStatus.value = "$count items safalta se restore ho gaye!"
            }.onFailure {
                _backupStatus.value = "Restore error: ${it.localizedMessage}"
            }
        }
    }

    // --- Routine ---
    fun addRoutine(title: String, hour: Int, minute: Int) {
        viewModelScope.launch {
            val id = container.repository.addRoutineTask(title, hour, minute)
            RoutineScheduler.scheduleNextAlarm(getApplication(), id, title, hour, minute)
        }
    }

    fun completeRoutine(task: RoutineTaskEntity) {
        viewModelScope.launch {
            val today = LocalDate.now().toEpochDay()
            if (task.lastCompletedEpochDay != today) {
                container.repository.updateRoutineTask(
                    task.copy(streakCount = task.streakCount + 1, lastCompletedEpochDay = today, isSkippedToday = false)
                )
            }
        }
    }

    fun moveRoutineByMinutes(task: RoutineTaskEntity, deltaMin: Int) {
        viewModelScope.launch {
            val total = (task.hour * 60 + task.minute + deltaMin).mod(24 * 60)
            val nh = total / 60
            val nm = total % 60
            val updated = task.copy(hour = nh, minute = nm)
            container.repository.updateRoutineTask(updated)
            RoutineScheduler.scheduleNextAlarm(getApplication(), updated.id, updated.title, nh, nm)
        }
    }

    fun deleteRoutine(id: Long) {
        viewModelScope.launch { container.repository.deleteRoutineTask(id) }
    }

    fun speakRoutineSummary() {
        viewModelScope.launch {
            val res = container.toolRegistry.invokeTool("query_routine", JSONObject().put("mode", "today"), confirmed = true)
            container.voiceEngine.speakViaTts(res.spokenFeedback)
        }
    }

    // --- Notifications ---
    fun clearNotificationsAndFeed() {
        viewModelScope.launch {
            container.repository.clearAllAnnouncements()
            container.repository.clearCapturedNotifications()
        }
    }

    fun summarizeNotificationsWithGemini() {
        viewModelScope.launch {
            val list = container.repository.getRecentNotifications(12)
            if (list.isEmpty()) {
                _notificationSmartSummary.value = "Abhi koi captured notifications nahi hain."
                return@launch
            }
            val joined = list.joinToString("\n") { "- [${it.appLabel}] ${it.sender}: ${it.text}" }
            val apiKey = container.keyStore.getGeminiApiKey()
            if (apiKey.isBlank()) {
                _notificationSmartSummary.value = joined
                container.voiceEngine.speakViaTts("Aapke paas ${list.size} notifications aaye hain.")
                return@launch
            }
            val res = container.geminiRestClient.generateWithTools(
                apiKey = apiKey,
                systemInstruction = "Summarize these phone notifications into a crisp 3-bullet Hinglish briefing.",
                userPrompt = joined
            )
            val txt = res.replyText.ifBlank { joined }
            _notificationSmartSummary.value = txt
            container.voiceEngine.speakViaTts(txt)
        }
    }

    fun updateNotificationPrefs(readOff: Boolean, full: Boolean, qStart: Int, qEnd: Int, csv: String) {
        viewModelScope.launch {
            container.preferences.setNotificationPrefs(readOff, full, qStart, qEnd, csv)
        }
    }

    // --- YouTube Channel Analyst ---
    fun handleOAuthRedirectUri(uri: Uri) {
        val frag = uri.fragment.orEmpty()
        if (frag.contains("access_token=")) {
            val params = frag.split("&").associate {
                val p = it.split("=", limit = 2)
                p.getOrElse(0) { "" } to Uri.decode(p.getOrElse(1) { "" })
            }
            val token = params["access_token"].orEmpty()
            val expiresSec = params["expires_in"]?.toLongOrNull() ?: 3600L
            if (token.isNotBlank()) {
                container.keyStore.saveYouTubeAccessToken(token, System.currentTimeMillis() + expiresSec * 1000L)
                _hasYouTubeToken.value = true
                analyzeYouTubeChannel()
            }
        }
    }

    fun saveYouTubeCredentials(clientId: String, manualToken: String) {
        if (clientId.isNotBlank()) container.keyStore.saveGoogleClientId(clientId)
        if (manualToken.isNotBlank()) {
            container.keyStore.saveYouTubeAccessToken(manualToken, System.currentTimeMillis() + 3600_000L)
            _hasYouTubeToken.value = true
        }
    }

    fun analyzeYouTubeChannel() {
        viewModelScope.launch {
            val token = container.keyStore.getYouTubeAccessToken()
            if (token.isBlank()) {
                _youtubeError.value = "OAuth Access Token nahi mila. Google Sign-In karo ya token paste karo."
                return@launch
            }
            _youtubeError.value = ""
            isYouTubeLoading.value = true
            container.publicApisClient.fetchYouTubeChannelAnalytics(token).fold(
                onSuccess = { m ->
                    _youtubeMetrics.value = m
                    val apiKey = container.keyStore.getGeminiApiKey()
                    if (apiKey.isNotBlank()) {
                        val prompt = """
                            Analyze this YouTube channel and give a 30-second spoken Hinglish summary + 3 viral title/thumbnail ideas + best upload strategy:
                            Channel: ${m.channelTitle}, Subscribers: ${m.subscriberCount}, Total Views: ${m.totalViews},
                            Last 28d Views: ${m.viewsLast28Days}, Watch Time Min: ${m.watchTimeMinutes28Days},
                            Avg Duration: ${m.averageViewDurationSec}s, Week-over-Week Growth: ${"%.1f".format(m.weekOverWeekGrowthPct)}%
                        """.trimIndent()
                        val ai = container.geminiRestClient.generateWithTools(
                            apiKey = apiKey,
                            systemInstruction = "You are an expert YouTube strategist speaking natural Hinglish.",
                            userPrompt = prompt
                        )
                        _youtubeAiAdvice.value = ai.replyText
                        container.voiceEngine.speakViaTts(ai.replyText.take(360))
                    } else {
                        val basic = "${m.channelTitle} par pichle 28 din me ${m.viewsLast28Days} views aur ${m.watchTimeMinutes28Days} watch minutes aaye hain."
                        _youtubeAiAdvice.value = basic
                        container.voiceEngine.speakViaTts(basic)
                    }
                },
                onFailure = {
                    _youtubeError.value = it.localizedMessage ?: "YouTube API error"
                }
            )
            isYouTubeLoading.value = false
        }
    }

    // --- Markets, Docs/Photos, Coding/Marketing, Settings ---
    fun refreshMarkets() {
        viewModelScope.launch {
            _marketLoading.value = true
            _marketError.value = ""
            container.publicApisClient.fetchLiveMarketQuotes().fold(
                onSuccess = { _marketQuotes.value = it },
                onFailure = { _marketError.value = it.localizedMessage ?: "Network error" }
            )
            _marketLoading.value = false
        }
    }

    fun speakMarketBrief() {
        viewModelScope.launch {
            val q = _marketQuotes.value
            if (q.isEmpty()) return@launch
            val brief = q.joinToString(", ") { "${it.name} $${"%.0f".format(it.priceUsd)} (${"%.1f".format(it.change24hPct)}%)" }
            container.voiceEngine.speakViaTts("Abhi live market me: $brief")
        }
    }

    fun analyzeDocOrPhoto(prompt: String, bytes: ByteArray?, mime: String?) {
        viewModelScope.launch {
            val apiKey = container.keyStore.getGeminiApiKey()
            if (apiKey.isBlank()) {
                _docOrPhotoAnalysis.value = "Document ya Photo padhne ke liye Settings me free Gemini API key daalo."
                return@launch
            }
            val finalPrompt = if (mime?.startsWith("text/") == true && bytes != null) {
                "$prompt\n\n${bytes.toString(Charsets.UTF_8).take(12000)}"
            } else prompt
            val inline = if (mime?.startsWith("text/") == true) null else bytes
            val res = container.geminiRestClient.generateWithTools(
                apiKey = apiKey,
                systemInstruction = "Answer clearly in Hinglish.",
                userPrompt = finalPrompt,
                inlineBytes = inline,
                inlineMimeType = mime
            )
            _docOrPhotoAnalysis.value = res.replyText.ifBlank { res.error ?: "Analysis nahi ho paya." }
        }
    }

    fun runStudioHelper(prompt: String) {
        viewModelScope.launch {
            val apiKey = container.keyStore.getGeminiApiKey()
            if (apiKey.isBlank()) {
                _helperStudioOutput.value = "Settings me apni free Gemini API key save karo."
                return@launch
            }
            val res = container.geminiRestClient.generateWithTools(
                apiKey = apiKey,
                systemInstruction = "Provide practical, high-craft output.",
                userPrompt = prompt
            )
            _helperStudioOutput.value = res.replyText.ifBlank { res.error ?: "Error" }
        }
    }

    fun triggerWirelessDebugging() {
        viewModelScope.launch {
            val res = container.toolRegistry.invokeTool("enable_wireless_debugging", JSONObject(), confirmed = true)
            container.voiceEngine.speakViaTts(res.spokenFeedback)
        }
    }

    fun renameAssistant(name: String) {
        viewModelScope.launch { container.preferences.setAssistantName(name) }
    }

    fun setLanguage(lang: AppLanguage) {
        viewModelScope.launch { container.preferences.setLanguage(lang) }
    }

    fun setPersonality(p: AssistantPersonality) {
        viewModelScope.launch { container.preferences.setPersonality(p) }
    }

    fun setVoiceMode(mode: String) {
        viewModelScope.launch { container.preferences.setVoiceEngineMode(mode) }
    }

    fun setFloatingOrb(enabled: Boolean) {
        viewModelScope.launch {
            container.preferences.setFloatingOrbEnabled(enabled)
            if (enabled) container.voiceEngine.showFloatingOrbOverlay()
            else container.voiceEngine.hideFloatingOrbOverlay()
        }
    }

    fun setReceptionistMode(enabled: Boolean, legalAccepted: Boolean) {
        viewModelScope.launch { container.preferences.setReceptionistConfig(enabled, legalAccepted) }
    }

    fun setEmergencyStopWord(word: String) {
        viewModelScope.launch { container.preferences.setEmergencyStopWord(word) }
    }

    fun addRule(text: String) {
        viewModelScope.launch { container.repository.addRule(text) }
    }

    fun toggleRule(rule: AssistantRuleEntity) {
        viewModelScope.launch { container.repository.toggleRule(rule) }
    }

    fun deleteRule(id: Long) {
        viewModelScope.launch { container.repository.deleteRule(id) }
    }

    fun togglePcSync(enabled: Boolean) {
        viewModelScope.launch {
            container.preferences.setPcSyncEnabled(enabled)
            if (enabled) container.pcLanServer.start(settings.value.pcSyncPort)
            else container.pcLanServer.stop()
        }
    }

    fun updatePcNote(note: String) {
        container.pcLanServer.updateClipboardNote(note)
    }

    fun activateUnlimitedFree() {
        viewModelScope.launch { container.preferences.setLicenseActivated(true) }
    }

    class Factory(
        private val application: Application,
        private val container: AppContainer
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(application, container) as T
        }
    }
}
