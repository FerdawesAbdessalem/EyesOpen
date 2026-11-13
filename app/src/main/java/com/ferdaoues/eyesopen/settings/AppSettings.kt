package com.ferdaoues.eyesopen.settings

import android.content.Context

/** User preferences, persisted between launches. */
class AppSettings(context: Context) {

    private val prefs = context.getSharedPreferences("eyesopen_settings", Context.MODE_PRIVATE)

    /** Speak the detected objects automatically. */
    var voiceEnabled: Boolean
        get() = prefs.getBoolean(KEY_VOICE, true)
        set(value) = prefs.edit().putBoolean(KEY_VOICE, value).apply()

    /** Add "on your left / ahead / on your right" to announcements. */
    var announcePosition: Boolean
        get() = prefs.getBoolean(KEY_POSITION, true)
        set(value) = prefs.edit().putBoolean(KEY_POSITION, value).apply()

    /** Vibrate when a new object is announced. */
    var vibrate: Boolean
        get() = prefs.getBoolean(KEY_VIBRATE, true)
        set(value) = prefs.edit().putBoolean(KEY_VIBRATE, value).apply()

    /** Minimum confidence (0.3 – 0.9) to keep a detection. */
    var minConfidence: Float
        get() = prefs.getFloat(KEY_CONFIDENCE, DEFAULT_CONFIDENCE)
        set(value) = prefs.edit().putFloat(KEY_CONFIDENCE, value.coerceIn(0.3f, 0.9f)).apply()

    private companion object {
        const val KEY_VOICE = "voice"
        const val KEY_POSITION = "position"
        const val KEY_VIBRATE = "vibrate"
        const val KEY_CONFIDENCE = "confidence"
        const val DEFAULT_CONFIDENCE = 0.55f
    }
}
