package com.example.core.persistence

import android.content.Context
import android.content.SharedPreferences

/**
 * Robust local persistence manager for game preferences and settings.
 * Handles graceful recovery if data is ever missing or corrupted.
 */
class PreferencesManager(context: Context) {
  private val prefs: SharedPreferences =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  var isMusicEnabled: Boolean
    get() = prefs.getBoolean(KEY_MUSIC_ENABLED, true)
    set(value) = prefs.edit().putBoolean(KEY_MUSIC_ENABLED, value).apply()

  var isSfxEnabled: Boolean
    get() = prefs.getBoolean(KEY_SFX_ENABLED, true)
    set(value) = prefs.edit().putBoolean(KEY_SFX_ENABLED, value).apply()

  var isVibrationEnabled: Boolean
    get() = prefs.getBoolean(KEY_VIBRATION_ENABLED, true)
    set(value) = prefs.edit().putBoolean(KEY_VIBRATION_ENABLED, value).apply()

  var playerName: String
    get() = prefs.getString(KEY_PLAYER_NAME, "Player") ?: "Player"
    set(value) = prefs.edit().putString(KEY_PLAYER_NAME, value.trim().take(16)).apply()

  companion object {
    private const val PREFS_NAME = "offline_uno_settings"
    private const val KEY_MUSIC_ENABLED = "pref_music_enabled"
    private const val KEY_SFX_ENABLED = "pref_sfx_enabled"
    private const val KEY_VIBRATION_ENABLED = "pref_vibration_enabled"
    private const val KEY_PLAYER_NAME = "pref_player_name"
  }
}
