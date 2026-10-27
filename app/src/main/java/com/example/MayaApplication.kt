package com.example

import android.app.Application
import com.example.core.AppPreferences
import com.example.core.SecureKeyStore
import com.example.data.MayaDatabase
import com.example.data.MayaRepository
import com.example.network.GeminiRestClient
import com.example.network.PublicApisClient
import com.example.service.PcPhoneLanServer
import com.example.service.RoutineScheduler
import com.example.service.VoiceEngineController
import com.example.tools.SystemToolsFactory
import com.example.tools.ToolRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(context: Application) {
    val preferences = AppPreferences(context)
    val keyStore = SecureKeyStore(context)
    val database = MayaDatabase.getInstance(context)
    val repository = MayaRepository(database.mayaDao())
    val geminiRestClient = GeminiRestClient()
    val publicApisClient = PublicApisClient(geminiRestClient.httpClient)

    val toolRegistry = ToolRegistry()
    val toolsFactory = SystemToolsFactory(
        context = context,
        repository = repository,
        publicApisClient = publicApisClient,
        getYouTubeToken = { keyStore.getYouTubeAccessToken() }
    ).also { it.registerAll(toolRegistry) }

    val voiceEngine = VoiceEngineController(
        context = context,
        preferences = preferences,
        keyStore = keyStore,
        repository = repository,
        geminiRestClient = geminiRestClient,
        toolRegistry = toolRegistry
    )

    val pcLanServer = PcPhoneLanServer(
        onRemoteCommand = { cmd -> voiceEngine.processSpokenOrTextUtterance(cmd, fromVoice = true) },
        getBackupJsonProvider = { repository.exportBackupJson() }
    )
}

class MayaApplication : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        appScope.launch {
            container.repository.seedDefaultsIfEmpty()
            runCatching { RoutineScheduler.enqueueDailyWorkManagerSync(this@MayaApplication) }
        }
    }
}
