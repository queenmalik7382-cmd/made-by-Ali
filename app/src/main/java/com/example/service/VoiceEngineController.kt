package com.example.service

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.graphics.drawable.GradientDrawable
import com.example.core.AppPreferences
import com.example.core.SecureKeyStore
import com.example.data.MayaRepository
import com.example.network.GeminiLiveWebSocketClient
import com.example.network.GeminiRestClient
import com.example.tools.PendingToolConfirmation
import com.example.tools.ToolRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Locale
import kotlin.math.sqrt

enum class VoiceOrbState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING,
    AWAITING_CONFIRMATION
}

/**
 * Coordinates Gemini Live API WebSocket (16 kHz PCM in / 24 kHz PCM out) + Native Android SpeechRecognizer/TTS fallback,
 * barge-in interruption, dangerous-action voice confirmation ("Pakka bhej doon?"), and the floating SYSTEM_ALERT_WINDOW orb.
 */
class VoiceEngineController(
    private val context: Context,
    private val preferences: AppPreferences,
    private val keyStore: SecureKeyStore,
    private val repository: MayaRepository,
    private val geminiRestClient: GeminiRestClient,
    private val toolRegistry: ToolRegistry
) : GeminiLiveWebSocketClient.ListenerCallbacks {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _orbState = MutableStateFlow(VoiceOrbState.IDLE)
    val orbState: StateFlow<VoiceOrbState> = _orbState.asStateFlow()

    private val _liveTranscript = MutableStateFlow("")
    val liveTranscript: StateFlow<String> = _liveTranscript.asStateFlow()

    private val _micActiveIndicator = MutableStateFlow(false)
    val micActiveIndicator: StateFlow<Boolean> = _micActiveIndicator.asStateFlow()

    private val _pendingConfirmation = MutableStateFlow<PendingToolConfirmation?>(null)
    val pendingConfirmation: StateFlow<PendingToolConfirmation?> = _pendingConfirmation.asStateFlow()

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var speechRecognizer: SpeechRecognizer? = null

    private val liveWsClient = GeminiLiveWebSocketClient(geminiRestClient.httpClient, this)
    private var micRecordJob: Job? = null
    private var audioRecord: AudioRecord? = null
    private var audioTrack24k: AudioTrack? = null

    private var overlayView: View? = null
    private var windowManager: WindowManager? = null

    init {
        initTextToSpeech()
    }

    private fun initTextToSpeech() {
        runCatching {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    ttsReady = true
                    tts?.language = Locale.forLanguageTag("hi-IN")
                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            _orbState.value = VoiceOrbState.SPEAKING
                            updateOverlayVisual()
                        }

                        override fun onDone(utteranceId: String?) {
                            if (_orbState.value == VoiceOrbState.SPEAKING) {
                                _orbState.value = if (_pendingConfirmation.value != null) {
                                    VoiceOrbState.AWAITING_CONFIRMATION
                                } else {
                                    VoiceOrbState.IDLE
                                }
                                updateOverlayVisual()
                            }
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            _orbState.value = VoiceOrbState.IDLE
                            updateOverlayVisual()
                        }
                    })
                }
            }
        }
    }

    fun toggleSession() {
        if (_orbState.value != VoiceOrbState.IDLE) {
            stopEverything()
        } else {
            startVoiceSession()
        }
    }

    fun startVoiceSession() {
        scope.launch {
            val settings = preferences.settingsFlow.first()
            val apiKey = keyStore.getGeminiApiKey()

            // Start Foreground Service so voice engine survives screen off
            runCatching {
                val fgIntent = Intent(context, MayaVoiceForegroundService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(fgIntent)
                } else {
                    context.startService(fgIntent)
                }
            }

            if (settings.floatingOrbEnabled) {
                showFloatingOrbOverlay()
            }

            // Barge-in: immediately stop any ongoing TTS or audio track playback
            interruptSpeaking()

            if (settings.voiceEngineMode == "gemini_live" && apiKey.isNotBlank()) {
                startGeminiLiveSession(apiKey, settings.assistantName, settings.personality.promptInstruction, settings.language.displayName)
            } else {
                startNativeSpeechFallback(settings.language.bcp47)
            }
        }
    }

    private suspend fun startGeminiLiveSession(
        apiKey: String,
        assistantName: String,
        personalityInstruction: String,
        langName: String
    ) {
        _orbState.value = VoiceOrbState.LISTENING
        _micActiveIndicator.value = true
        updateOverlayVisual()

        val memContext = repository.buildSystemPromptMemoryContext()
        val sysPrompt = buildString {
            appendLine("You are $assistantName, a real-time voice-first AI phone assistant on Android.")
            appendLine("Primary language: $langName (default Roman Urdu/Hindi Hinglish unless user switches).")
            appendLine(personalityInstruction)
            appendLine("Keep spoken replies crisp (1-3 short sentences). Use registered function tools whenever the user asks to open apps, control the phone, check routine, read notifications, or call contacts.")
            if (memContext.isNotBlank()) appendLine(memContext)
        }

        ensureAudioTrack24k()
        liveWsClient.connect(
            apiKey = apiKey,
            systemInstruction = sysPrompt,
            toolDeclarations = toolRegistry.geminiFunctionDeclarations()
        )
        startMicCapture16k()
    }

    @SuppressLint("MissingPermission")
    private fun startMicCapture16k() {
        micRecordJob?.cancel()
        micRecordJob = scope.launch(Dispatchers.IO) {
            val sampleRate = 16000
            val minBuf = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(3200)

            val rec = runCatching {
                AudioRecord(
                    MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBuf * 2
                )
            }.getOrNull()

            if (rec == null || rec.state != AudioRecord.STATE_INITIALIZED) {
                scope.launch(Dispatchers.Main) {
                    val settings = preferences.settingsFlow.first()
                    startNativeSpeechFallback(settings.language.bcp47)
                }
                return@launch
            }

            audioRecord = rec
            rec.startRecording()
            val buf = ByteArray(3200) // 100ms of 16kHz 16-bit mono PCM

            while (isActive && _micActiveIndicator.value) {
                val read = rec.read(buf, 0, buf.size)
                if (read > 0) {
                    // Barge-in energy check while assistant is speaking
                    if (_orbState.value == VoiceOrbState.SPEAKING && computeRms(buf, read) > 2100.0) {
                        interruptSpeaking()
                        _orbState.value = VoiceOrbState.LISTENING
                        mainHandler.post { updateOverlayVisual() }
                    }
                    if (liveWsClient.isConnected) {
                        liveWsClient.sendAudioChunk16k(buf, read)
                    }
                }
            }
        }
    }

    private fun computeRms(pcm16: ByteArray, length: Int): Double {
        var sum = 0.0
        var samples = 0
        var i = 0
        while (i + 1 < length) {
            val sample = ((pcm16[i + 1].toInt() shl 8) or (pcm16[i].toInt() and 0xFF)).toShort()
            sum += sample * sample
            samples++
            i += 2
        }
        return if (samples > 0) sqrt(sum / samples) else 0.0
    }

    private fun ensureAudioTrack24k() {
        if (audioTrack24k != null) return
        val sampleRate = 24000
        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(4800)

        audioTrack24k = runCatching {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBuf * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
                .apply { play() }
        }.getOrNull()
    }

    private fun startNativeSpeechFallback(bcp47Language: String) {
        mainHandler.post {
            _orbState.value = VoiceOrbState.LISTENING
            _micActiveIndicator.value = true
            updateOverlayVisual()

            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                _liveTranscript.value = "Speech recognition is device par uplabdh nahi hai; Chat box me likho."
                _orbState.value = VoiceOrbState.IDLE
                _micActiveIndicator.value = false
                return@post
            }

            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _orbState.value = VoiceOrbState.LISTENING
                    }
                    override fun onBeginningOfSpeech() {
                        interruptSpeaking()
                    }
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        _micActiveIndicator.value = false
                        _orbState.value = VoiceOrbState.THINKING
                        updateOverlayVisual()
                    }
                    override fun onError(error: Int) {
                        _micActiveIndicator.value = false
                        _orbState.value = VoiceOrbState.IDLE
                        updateOverlayVisual()
                    }
                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val spoken = matches?.firstOrNull().orEmpty()
                        if (spoken.isNotBlank()) {
                            processSpokenOrTextUtterance(spoken, fromVoice = true)
                        } else {
                            _orbState.value = VoiceOrbState.IDLE
                        }
                    }
                    override fun onPartialResults(partialResults: Bundle?) {
                        val part = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                        if (!part.isNullOrBlank()) _liveTranscript.value = part
                    }
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val recIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, bcp47Language)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
            speechRecognizer?.startListening(recIntent)
        }
    }

    /**
     * Unified handler for voice utterances or text chat turns, enforcing emergency stop word,
     * pending confirmation ("haan"/"pakka" vs "nahi"/"ruko"), Gemini tool calling, and offline local fallback.
     */
    fun processSpokenOrTextUtterance(
        text: String,
        fromVoice: Boolean,
        inlineBytes: ByteArray? = null,
        inlineMimeType: String? = null,
        attachmentUri: String? = null,
        attachmentName: String? = null
    ) {
        scope.launch {
            val clean = text.trim()
            if (clean.isBlank() && inlineBytes == null) return@launch

            val settings = preferences.settingsFlow.first()
            val lower = clean.lowercase()

            // 1. Emergency stop word check ("ruko" / "stop")
            if (lower == settings.emergencyStopWord || lower == "ruko" || lower == "maya ruko" || lower == "emergency stop") {
                stopEverything()
                val msg = "Sab rok diya gaya hai, dost."
                repository.addMessage("assistant", msg, if (fromVoice) "voice" else "chat")
                if (fromVoice) speakViaTts(msg)
                return@launch
            }

            _liveTranscript.value = clean
            repository.addMessage(
                role = "user",
                content = clean.ifBlank { "[Attachment: ${attachmentName ?: "file"}]" },
                source = if (fromVoice) "voice" else "chat",
                attachmentUri = attachmentUri,
                attachmentType = inlineMimeType,
                attachmentName = attachmentName
            )

            // 2. Check if awaiting confirmation ("Pakka bhej doon?")
            val pending = _pendingConfirmation.value
            if (pending != null) {
                val yesWords = listOf("haan", "han", "yes", "pakka", "kar do", "bhej do", "ok", "sure", "lagao")
                val isConfirmed = yesWords.any { lower.contains(it) }
                _pendingConfirmation.value = null
                if (isConfirmed) {
                    val res = toolRegistry.invokeTool(pending.toolName, pending.args, confirmed = true)
                    repository.addMessage("assistant", res.spokenFeedback, if (fromVoice) "voice" else "chat")
                    speakViaTts(res.spokenFeedback)
                } else {
                    val cancelMsg = "Theek hai, cancel kar diya."
                    repository.addMessage("assistant", cancelMsg, if (fromVoice) "voice" else "chat")
                    speakViaTts(cancelMsg)
                }
                return@launch
            }

            _orbState.value = VoiceOrbState.THINKING
            updateOverlayVisual()

            val apiKey = keyStore.getGeminiApiKey()
            if (apiKey.isBlank()) {
                // Offline / No-Key intelligent local tool parser
                val matched = toolRegistry.matchLocalHinglishIntent(clean)
                if (matched != null) {
                    handleToolCallInternal("local_call", matched.first, matched.second, fromVoice)
                } else {
                    val fallbackReply =
                        "Abhi Gemini API key set nahi hai, par main offline commands chala sakti hoon (jaise 'YouTube kholo', 'aaj ki routine batao', 'torch on karo'). Full AI baatcheet ke liye Settings me free Gemini key daalo."
                    repository.addMessage("assistant", fallbackReply, if (fromVoice) "voice" else "chat")
                    if (fromVoice) speakViaTts(fallbackReply) else _orbState.value = VoiceOrbState.IDLE
                }
                return@launch
            }

            val memContext = repository.buildSystemPromptMemoryContext()
            val sysInstruction = buildString {
                appendLine("You are ${settings.assistantName}, a smart Android phone assistant.")
                appendLine("Default language: ${settings.language.displayName}. ${settings.personality.promptInstruction}")
                appendLine("When the user wants a phone action, invoke the matching tool.")
                if (memContext.isNotBlank()) appendLine(memContext)
            }

            val recentDb = repository.messages.first().takeLast(10).map { it.role to it.content }
            val result = geminiRestClient.generateWithTools(
                apiKey = apiKey,
                systemInstruction = sysInstruction,
                userPrompt = clean.ifBlank { "Is file/photo ko dhyan se samjhao." },
                recentHistory = recentDb,
                toolDeclarations = if (inlineBytes == null) toolRegistry.geminiFunctionDeclarations() else null,
                inlineBytes = inlineBytes,
                inlineMimeType = inlineMimeType
            )

            if (result.functionCallName != null) {
                handleToolCallInternal(
                    callId = "rest_tool",
                    name = result.functionCallName,
                    args = result.functionCallArgs ?: JSONObject(),
                    speakReply = fromVoice
                )
            } else {
                val reply = result.replyText.ifBlank {
                    result.error ?: "Maaf karna, abhi jawab nahi ban paaya."
                }
                repository.addMessage("assistant", reply, if (fromVoice) "voice" else "chat")
                _liveTranscript.value = reply
                if (fromVoice) speakViaTts(reply) else _orbState.value = VoiceOrbState.IDLE
            }
            updateOverlayVisual()
        }
    }

    private suspend fun handleToolCallInternal(
        callId: String,
        name: String,
        args: JSONObject,
        speakReply: Boolean
    ) {
        val toolResult = toolRegistry.invokeTool(name, args, confirmed = false)
        if (toolResult.requiresConfirmation) {
            _pendingConfirmation.value = PendingToolConfirmation(
                callId = callId,
                toolName = name,
                args = args,
                promptSpoken = toolResult.confirmationPrompt
            )
            _orbState.value = VoiceOrbState.AWAITING_CONFIRMATION
            repository.addMessage("assistant", toolResult.spokenFeedback, "voice")
            speakViaTts(toolResult.spokenFeedback)
            return
        }

        repository.addMessage("assistant", toolResult.spokenFeedback, if (speakReply) "voice" else "chat")
        _liveTranscript.value = toolResult.spokenFeedback
        if (liveWsClient.isConnected) {
            liveWsClient.sendToolResponse(callId, name, toolResult.toJson())
        } else if (speakReply) {
            speakViaTts(toolResult.spokenFeedback)
        } else {
            _orbState.value = VoiceOrbState.IDLE
        }
    }

    fun confirmPendingTool(approved: Boolean) {
        scope.launch {
            val pending = _pendingConfirmation.value ?: return@launch
            _pendingConfirmation.value = null
            if (approved) {
                val res = toolRegistry.invokeTool(pending.toolName, pending.args, confirmed = true)
                repository.addMessage("assistant", res.spokenFeedback, "voice")
                speakViaTts(res.spokenFeedback)
            } else {
                val msg = "Action rok diya gaya."
                repository.addMessage("assistant", msg, "voice")
                _orbState.value = VoiceOrbState.IDLE
            }
        }
    }

    fun speakViaTts(text: String) {
        if (text.isBlank()) return
        if (ttsReady) {
            _orbState.value = VoiceOrbState.SPEAKING
            updateOverlayVisual()
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "maya_utt_${System.currentTimeMillis()}")
        } else {
            _orbState.value = VoiceOrbState.IDLE
        }
    }

    fun interruptSpeaking() {
        runCatching { tts?.stop() }
        runCatching {
            audioTrack24k?.pause()
            audioTrack24k?.flush()
            audioTrack24k?.play()
        }
    }

    fun stopEverything() {
        MayaAccessibilityService.triggerEmergencyStop()
        _micActiveIndicator.value = false
        _pendingConfirmation.value = null
        micRecordJob?.cancel()
        runCatching {
            audioRecord?.stop()
            audioRecord?.release()
        }
        audioRecord = null
        liveWsClient.disconnect()
        mainHandler.post {
            runCatching { speechRecognizer?.cancel(); speechRecognizer?.destroy() }
            speechRecognizer = null
        }
        interruptSpeaking()
        _orbState.value = VoiceOrbState.IDLE
        updateOverlayVisual()
    }

    // --- Gemini Live WebSocket Callbacks ---
    override fun onConnected() {
        _orbState.value = VoiceOrbState.LISTENING
        mainHandler.post { updateOverlayVisual() }
    }

    override fun onAudioPcm24kReceived(pcmBytes: ByteArray) {
        _orbState.value = VoiceOrbState.SPEAKING
        ensureAudioTrack24k()
        audioTrack24k?.write(pcmBytes, 0, pcmBytes.size)
        mainHandler.post { updateOverlayVisual() }
    }

    override fun onTranscriptText(text: String, isUser: Boolean) {
        _liveTranscript.value = text
        scope.launch {
            repository.addMessage(if (isUser) "user" else "assistant", text, "voice")
        }
    }

    override fun onToolCall(callId: String, name: String, args: JSONObject) {
        scope.launch {
            handleToolCallInternal(callId, name, args, speakReply = true)
        }
    }

    override fun onInterrupted() {
        interruptSpeaking()
        _orbState.value = VoiceOrbState.LISTENING
        mainHandler.post { updateOverlayVisual() }
    }

    override fun onDisconnected(reason: String) {
        if (_micActiveIndicator.value) {
            _micActiveIndicator.value = false
            _orbState.value = VoiceOrbState.IDLE
            mainHandler.post { updateOverlayVisual() }
        }
    }

    // --- Floating SYSTEM_ALERT_WINDOW Orb Overlay ---
    fun showFloatingOrbOverlay() {
        mainHandler.post {
            if (!Settings.canDrawOverlays(context) || overlayView != null) return@post
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager = wm
            val sizePx = (58 * context.resources.displayMetrics.density).toInt()
            val view = View(context).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(0xFF2B88FF.toInt())
                    setStroke(6, 0xCCFFFFFF.toInt())
                }
                setOnClickListener { toggleSession() }
            }
            val params = WindowManager.LayoutParams(
                sizePx,
                sizePx,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.END
                x = 24
                y = 260
            }
            runCatching {
                wm.addView(view, params)
                overlayView = view
            }
        }
    }

    fun hideFloatingOrbOverlay() {
        mainHandler.post {
            overlayView?.let { v ->
                runCatching { windowManager?.removeView(v) }
            }
            overlayView = null
        }
    }

    private fun updateOverlayVisual() {
        val v = overlayView ?: return
        val bg = v.background as? GradientDrawable ?: return
        val color = when (_orbState.value) {
            VoiceOrbState.IDLE -> 0xFF2B88FF.toInt()
            VoiceOrbState.LISTENING -> 0xFF10B981.toInt()
            VoiceOrbState.THINKING -> 0xFFF59E0B.toInt()
            VoiceOrbState.SPEAKING -> 0xFF8B5CF6.toInt()
            VoiceOrbState.AWAITING_CONFIRMATION -> 0xFFEF4444.toInt()
        }
        bg.setColor(color)
    }
}
