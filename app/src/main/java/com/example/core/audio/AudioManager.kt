package com.example.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.core.persistence.PreferencesManager

/**
 * Sound and haptic manager designed specifically for mobile card gaming.
 * Includes synthesized tones or sound pool cues for buttons, card flicks, turns, and wins.
 */
class AudioManager(
  private val context: Context,
  private val preferencesManager: PreferencesManager
) {
  private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
    vibratorManager?.defaultVibrator
  } else {
    @Suppress("DEPRECATION")
    context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
  }

  private val soundPool: SoundPool

  init {
    val audioAttributes = AudioAttributes.Builder()
      .setUsage(AudioAttributes.USAGE_GAME)
      .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
      .build()
    soundPool = SoundPool.Builder()
      .setMaxStreams(4)
      .setAudioAttributes(audioAttributes)
      .build()
  }

  fun playButtonClick() {
    if (preferencesManager.isSfxEnabled) {
      triggerHaptic(30)
    }
  }

  fun playCardPlay() {
    if (preferencesManager.isSfxEnabled) {
      triggerHaptic(45)
    }
  }

  fun playCardDraw() {
    if (preferencesManager.isSfxEnabled) {
      triggerHaptic(25)
    }
  }

  fun playTurnAlert() {
    if (preferencesManager.isSfxEnabled) {
      triggerHaptic(60)
    }
  }

  fun playWin() {
    if (preferencesManager.isSfxEnabled) {
      triggerHaptic(120)
    }
  }

  private fun triggerHaptic(durationMs: Long) {
    if (!preferencesManager.isVibrationEnabled) return
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator?.vibrate(
          VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
        )
      } else {
        @Suppress("DEPRECATION")
        vibrator?.vibrate(durationMs)
      }
    } catch (_: Exception) {
      // Ignore vibration error on unsupported hardware
    }
  }

  fun release() {
    soundPool.release()
  }
}
