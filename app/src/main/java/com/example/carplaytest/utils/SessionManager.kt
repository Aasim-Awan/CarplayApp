package com.example.carplaytest.utils

import android.content.Context

class SessionManager(context: Context) {
    private val preferences = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
    private val editor = preferences.edit()

    // General SharedPreferences methods
    fun saveString(key: String, value: String) {
        editor.putString(key, value).apply()
    }

    fun getString(key: String, defaultValue: String? = null): String? {
        return preferences.getString(key, defaultValue)
    }

    fun saveBoolean(key: String, value: Boolean) {
        editor.putBoolean(key, value).apply()
    }

    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return preferences.getBoolean(key, defaultValue)
    }

    fun saveInt(key: String, value: Int) {
        editor.putInt(key, value).apply()
    }

    fun getInt(key: String, defaultValue: Int): Int {
        return preferences.getInt(key, defaultValue)
    }

    fun saveFloat(key: String, value: Float) {
        editor.putFloat(key, value).apply()
    }

    fun getFloat(key: String, defaultValue: Float): Float {
        return preferences.getFloat(key, defaultValue)
    }

    fun saveLong(key: String, value: Long) {
        editor.putLong(key, value).apply()
    }

    fun getLong(key: String, defaultValue: Long): Long {
        return preferences.getLong(key, defaultValue)
    }

    fun removeKey(key: String) {
        preferences.edit().remove(key).apply()
    }

    fun clear() {
        editor.clear().apply()
    }

    fun saveEmails(name: String, sender: String, receiver: String) {
        editor.putString("Name", name)
            .putString("SenderEmail", sender)
            .putString("ReceiverEmail", receiver)
            .apply()
    }

    fun getEmails(): Triple<String?, String?, String?> {
        val name = preferences.getString("Name", null)
        val sender = preferences.getString("SenderEmail", null)
        val receiver = preferences.getString("ReceiverEmail", null)
        return Triple(name, sender, receiver)
    }

    fun setLastEmailSentTime(timestamp: Long) {
        editor.putLong("LastEmailDate", timestamp).apply()
    }

    fun getLastEmailSentTime(): Long {
        return preferences.getLong("LastEmailDate", 0L)
    }

    fun clearEmails() {
        editor.remove("Name")
            .remove("SenderEmail")
            .remove("ReceiverEmail")
            .apply()
    }

    object SessionKeys {
        const val IS_LOCATION_TRACKING_ENABLED = "is_location_tracking_enabled"
        const val IS_CRASH_MONITORING_ENABLED = "is_crash_monitoring_enabled"
        const val IS_SPEED_TRACKING_ENABLED = "is_speed_tracking_enabled"

        const val KEY_IS_FIRST_LAUNCH: String = "translator.first_launch"
        const val IS_REMOVE_AD_PURCHASED = "translator.purchase.ads"
        const val KEY_SOURCE_OCR: String = "translate.language.source.ocr"
        const val KEY_SOURCE: String = "translate.language.source"
        const val KEY_TARGET: String = "translate.language.target"
        const val KEY_TRANSLATE_COIN: String = "translate.free.coins"
        const val IS_GDPR_CHECKED: String = "translate.gdpr.check"
        const val SHOW_INAPP: String = "translator.inapp.dialog.show"
        const val SAVE_TRANSLATION: String = "is.save.translation"
    }
}
