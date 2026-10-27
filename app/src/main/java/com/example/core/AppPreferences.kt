package com.example.core

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.mayaDataStore by preferencesDataStore(name = "maya_user_preferences")

data class UserSettings(
    val assistantName: String = "Maya",
    val language: AppLanguage = AppLanguage.HINGLISH,
    val personality: AssistantPersonality = AssistantPersonality.DOST,
    val onboardingCompleted: Boolean = false,
    val voiceEngineMode: String = "gemini_live", // gemini_live | native_tts
    val hotwordEnabled: Boolean = false,
    val floatingOrbEnabled: Boolean = false,
    val readNotificationsScreenOff: Boolean = true,
    val readNotificationFullText: Boolean = true,
    val quietHoursStart: Int = 23,
    val quietHoursEnd: Int = 7,
    val allowedNotificationApps: String = "com.whatsapp,com.google.android.apps.messaging,com.google.android.gm,org.telegram.messenger",
    val receptionistModeEnabled: Boolean = false,
    val legalCallNoticeAccepted: Boolean = false,
    val autoAnswerDelaySec: Int = 4,
    val pcSyncEnabled: Boolean = false,
    val pcSyncPort: Int = 8765,
    val licenseActivated: Boolean = false,
    val energyPoints: Int = 10,
    val moodStatus: String = "Pyaar ...",
    val emergencyStopWord: String = "ruko"
)

class AppPreferences(private val context: Context) {
    private object Keys {
        val ASSISTANT_NAME = stringPreferencesKey("assistant_name")
        val LANGUAGE = stringPreferencesKey("language")
        val PERSONALITY = stringPreferencesKey("personality")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val VOICE_ENGINE = stringPreferencesKey("voice_engine")
        val HOTWORD = booleanPreferencesKey("hotword_enabled")
        val FLOATING_ORB = booleanPreferencesKey("floating_orb_enabled")
        val READ_NOTIFS_SCREEN_OFF = booleanPreferencesKey("read_notifs_screen_off")
        val READ_NOTIFS_FULL = booleanPreferencesKey("read_notifs_full")
        val QUIET_START = intPreferencesKey("quiet_hours_start")
        val QUIET_END = intPreferencesKey("quiet_hours_end")
        val ALLOWED_NOTIF_APPS = stringPreferencesKey("allowed_notif_apps")
        val RECEPTIONIST_MODE = booleanPreferencesKey("receptionist_mode")
        val LEGAL_CALL_NOTICE = booleanPreferencesKey("legal_call_notice")
        val AUTO_ANSWER_DELAY = intPreferencesKey("auto_answer_delay")
        val PC_SYNC_ENABLED = booleanPreferencesKey("pc_sync_enabled")
        val PC_SYNC_PORT = intPreferencesKey("pc_sync_port")
        val LICENSE_ACTIVATED = booleanPreferencesKey("license_activated")
        val ENERGY_POINTS = intPreferencesKey("energy_points")
        val MOOD_STATUS = stringPreferencesKey("mood_status")
        val EMERGENCY_WORD = stringPreferencesKey("emergency_stop_word")
    }

    val settingsFlow: Flow<UserSettings> = context.mayaDataStore.data.map { prefs ->
        UserSettings(
            assistantName = prefs[Keys.ASSISTANT_NAME]?.ifBlank { "Maya" } ?: "Maya",
            language = AppLanguage.fromCode(prefs[Keys.LANGUAGE] ?: "hinglish"),
            personality = AssistantPersonality.fromId(prefs[Keys.PERSONALITY] ?: "dost"),
            onboardingCompleted = prefs[Keys.ONBOARDING_DONE] ?: false,
            voiceEngineMode = prefs[Keys.VOICE_ENGINE] ?: "gemini_live",
            hotwordEnabled = prefs[Keys.HOTWORD] ?: false,
            floatingOrbEnabled = prefs[Keys.FLOATING_ORB] ?: false,
            readNotificationsScreenOff = prefs[Keys.READ_NOTIFS_SCREEN_OFF] ?: true,
            readNotificationFullText = prefs[Keys.READ_NOTIFS_FULL] ?: true,
            quietHoursStart = prefs[Keys.QUIET_START] ?: 23,
            quietHoursEnd = prefs[Keys.QUIET_END] ?: 7,
            allowedNotificationApps = prefs[Keys.ALLOWED_NOTIF_APPS]
                ?: "com.whatsapp,com.google.android.apps.messaging,com.google.android.gm,org.telegram.messenger",
            receptionistModeEnabled = prefs[Keys.RECEPTIONIST_MODE] ?: false,
            legalCallNoticeAccepted = prefs[Keys.LEGAL_CALL_NOTICE] ?: false,
            autoAnswerDelaySec = prefs[Keys.AUTO_ANSWER_DELAY] ?: 4,
            pcSyncEnabled = prefs[Keys.PC_SYNC_ENABLED] ?: false,
            pcSyncPort = prefs[Keys.PC_SYNC_PORT] ?: 8765,
            licenseActivated = prefs[Keys.LICENSE_ACTIVATED] ?: false,
            energyPoints = prefs[Keys.ENERGY_POINTS] ?: 10,
            moodStatus = prefs[Keys.MOOD_STATUS] ?: "Pyaar ...",
            emergencyStopWord = prefs[Keys.EMERGENCY_WORD] ?: "ruko"
        )
    }

    suspend fun setAssistantName(name: String) {
        context.mayaDataStore.edit { it[Keys.ASSISTANT_NAME] = name.trim().ifBlank { "Maya" } }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.mayaDataStore.edit { it[Keys.LANGUAGE] = language.code }
    }

    suspend fun setPersonality(personality: AssistantPersonality) {
        context.mayaDataStore.edit { it[Keys.PERSONALITY] = personality.id }
    }

    suspend fun setOnboardingCompleted(done: Boolean) {
        context.mayaDataStore.edit { it[Keys.ONBOARDING_DONE] = done }
    }

    suspend fun setVoiceEngineMode(mode: String) {
        context.mayaDataStore.edit { it[Keys.VOICE_ENGINE] = mode }
    }

    suspend fun setHotwordEnabled(enabled: Boolean) {
        context.mayaDataStore.edit { it[Keys.HOTWORD] = enabled }
    }

    suspend fun setFloatingOrbEnabled(enabled: Boolean) {
        context.mayaDataStore.edit { it[Keys.FLOATING_ORB] = enabled }
    }

    suspend fun setNotificationPrefs(
        readWhenScreenOff: Boolean,
        readFullText: Boolean,
        quietStart: Int,
        quietEnd: Int,
        allowedPackagesCsv: String
    ) {
        context.mayaDataStore.edit {
            it[Keys.READ_NOTIFS_SCREEN_OFF] = readWhenScreenOff
            it[Keys.READ_NOTIFS_FULL] = readFullText
            it[Keys.QUIET_START] = quietStart
            it[Keys.QUIET_END] = quietEnd
            it[Keys.ALLOWED_NOTIF_APPS] = allowedPackagesCsv
        }
    }

    suspend fun setReceptionistConfig(enabled: Boolean, legalAccepted: Boolean, delaySec: Int = 4) {
        context.mayaDataStore.edit {
            it[Keys.RECEPTIONIST_MODE] = enabled
            it[Keys.LEGAL_CALL_NOTICE] = legalAccepted
            it[Keys.AUTO_ANSWER_DELAY] = delaySec.coerceIn(2, 15)
        }
    }

    suspend fun setPcSyncEnabled(enabled: Boolean) {
        context.mayaDataStore.edit { it[Keys.PC_SYNC_ENABLED] = enabled }
    }

    suspend fun setLicenseActivated(activated: Boolean) {
        context.mayaDataStore.edit {
            it[Keys.LICENSE_ACTIVATED] = activated
            if (activated) it[Keys.ENERGY_POINTS] = 999
        }
    }

    suspend fun setMoodStatus(mood: String) {
        context.mayaDataStore.edit { it[Keys.MOOD_STATUS] = mood }
    }

    suspend fun setEmergencyStopWord(word: String) {
        context.mayaDataStore.edit { it[Keys.EMERGENCY_WORD] = word.trim().lowercase().ifBlank { "ruko" } }
    }
}
