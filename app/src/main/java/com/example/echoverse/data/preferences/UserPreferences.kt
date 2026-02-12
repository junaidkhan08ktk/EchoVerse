package com.example.echoverse.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("echo_verse_prefs", Context.MODE_PRIVATE)

    companion object {
        const val KEY_SENSITIVITY = "sensitivity"
        const val KEY_COLOR_PALETTE = "color_palette" // 0: Default, 1: Neon, 2: Pastel
        const val KEY_MIC_ENABLED = "mic_enabled"
        const val KEY_CALM_FACTOR = "calm_factor" // 0.0 (Calm) to 1.0 (Energetic)
        const val KEY_SELECTED_WORLD = "selected_world"
    }

    var selectedWorldId: String
        get() = prefs.getString(KEY_SELECTED_WORLD, "1") ?: "1"
        set(value) = prefs.edit().putString(KEY_SELECTED_WORLD, value).apply()

    var sensitivity: Float
        get() = prefs.getFloat(KEY_SENSITIVITY, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_SENSITIVITY, value).apply()

    var colorPalette: Int
        get() = prefs.getInt(KEY_COLOR_PALETTE, 0)
        set(value) = prefs.edit().putInt(KEY_COLOR_PALETTE, value).apply()

    var isMicEnabled: Boolean
        get() = prefs.getBoolean(KEY_MIC_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_MIC_ENABLED, value).apply()

    var calmFactor: Float
        get() = prefs.getFloat(KEY_CALM_FACTOR, 0.5f)
        set(value) = prefs.edit().putFloat(KEY_CALM_FACTOR, value).apply()

    // Observe preference changes
    fun observePreferences(): Flow<String?> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            trySend(key)
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
}
