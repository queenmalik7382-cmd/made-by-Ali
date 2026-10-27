package com.example.core

import com.example.BuildConfig

object FlavorConfig {
    val isDirectFlavor: Boolean = BuildConfig.IS_DIRECT_FLAVOR
    val flavorName: String = BuildConfig.FLAVOR_MODE

    val supportsAccessibilityAutomation: Boolean = isDirectFlavor
    val supportsAutoAnswerCall: Boolean = isDirectFlavor
    val supportsSecureSettingsWrite: Boolean = isDirectFlavor

    fun playLimitationMessage(lang: AppLanguage): String = when (lang) {
        AppLanguage.HINGLISH -> "Ye feature sirf Maya Direct flavor me milta hai. Google Play policy ke mutabiq Play edition me Accessibility automation, Auto-answer calls aur Secure Settings write band rakhe gaye hain. Intent-based actions bilkul kaam karenge."
        AppLanguage.URDU -> "یہ فیچر صرف مایا ڈائریکٹ ایڈیشن میں دستیاب ہے۔ گوگل پلے پالیسی کے مطابق پلے ورژن میں آٹو کال ریسیو اور ایکسیسبیلٹی آٹومیشن بند ہے۔"
        AppLanguage.ENGLISH -> "This feature is available only in the Direct build flavor. Google Play edition omits Accessibility screen automation, call auto-answer, and WRITE_SECURE_SETTINGS, using safe system Intents instead."
    }
}
