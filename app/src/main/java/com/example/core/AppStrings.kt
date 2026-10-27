package com.example.core

enum class AppLanguage(val code: String, val displayName: String, val bcp47: String) {
    HINGLISH("hinglish", "Hinglish (Roman)", "hi-IN"),
    URDU("urdu", "اردو (Urdu)", "ur-PK"),
    ENGLISH("english", "English", "en-IN");

    companion object {
        fun fromCode(code: String): AppLanguage =
            entries.firstOrNull { it.code == code } ?: HINGLISH
    }
}

enum class AssistantPersonality(val id: String, val labelHinglish: String, val promptInstruction: String) {
    DOST(
        "dost",
        "Pyaari Dost (Warm & Friendly)",
        "Speak warmly like a caring, smart best friend ('dost', 'tum'). Keep answers crisp, helpful, natural Hinglish/Urdu/English."
    ),
    PROFESSIONAL(
        "pro",
        "Executive Secretary (Sharp & Formal)",
        "Speak concisely and respectfully ('aap'). Prioritize efficiency, bullet summaries, and executive clarity."
    ),
    COACH(
        "coach",
        "Strict Routine Coach (High Energy)",
        "Encourage discipline, streak consistency, and focus. Keep high motivating energy."
    ),
    CODER(
        "coder",
        "Tech & Creator Partner",
        "Give sharp technical, YouTube growth, coding, and debugging insights with practical next steps."
    );

    companion object {
        fun fromId(id: String): AssistantPersonality =
            entries.firstOrNull { it.id == id } ?: DOST
    }
}

object AppStrings {
    fun greeting(lang: AppLanguage, hour: Int): String = when (lang) {
        AppLanguage.HINGLISH -> when (hour) {
            in 5..11 -> "Good morning,"
            in 12..16 -> "Good afternoon,"
            in 17..21 -> "Good evening,"
            else -> "Raat bhar jaag rahe ho,"
        }
        AppLanguage.URDU -> when (hour) {
            in 5..11 -> "صبح بخیر،"
            in 12..16 -> "دوپہر بخیر،"
            else -> "شام بخیر،"
        }
        AppLanguage.ENGLISH -> when (hour) {
            in 5..11 -> "Good morning,"
            in 12..16 -> "Good afternoon,"
            else -> "Good evening,"
        }
    }

    fun friendWord(lang: AppLanguage): String = when (lang) {
        AppLanguage.HINGLISH -> "dost"
        AppLanguage.URDU -> "دوست"
        AppLanguage.ENGLISH -> "friend"
    }

    fun askAnythingPlaceholder(name: String, lang: AppLanguage): String = when (lang) {
        AppLanguage.HINGLISH -> "$name se kuchh bhi poochho..."
        AppLanguage.URDU -> "$name سے کچھ بھی پوچھیں..."
        AppLanguage.ENGLISH -> "Ask $name anything..."
    }

    fun navHome(lang: AppLanguage) = when (lang) {
        AppLanguage.URDU -> "ہوم"
        else -> "Home"
    }

    fun navScan(lang: AppLanguage) = when (lang) {
        AppLanguage.URDU -> "اسکین"
        else -> "Scan"
    }

    fun navYaadein(lang: AppLanguage) = when (lang) {
        AppLanguage.HINGLISH -> "Yaadein"
        AppLanguage.URDU -> "یادیں"
        AppLanguage.ENGLISH -> "Memories"
    }

    fun navChat(lang: AppLanguage) = when (lang) {
        AppLanguage.URDU -> "چیٹ"
        else -> "Chat"
    }

    fun confirmPrompt(actionSummary: String, lang: AppLanguage): String = when (lang) {
        AppLanguage.HINGLISH -> "$actionSummary — Pakka bhej doon ya chalu karoon?"
        AppLanguage.URDU -> "$actionSummary — کیا پکا کر دوں؟"
        AppLanguage.ENGLISH -> "$actionSummary — Are you sure you want me to proceed?"
    }
}
