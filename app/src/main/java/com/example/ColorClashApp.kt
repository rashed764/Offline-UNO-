package com.example

import android.app.Application
import com.example.core.audio.AudioManager
import com.example.core.economy.CoinRepository
import com.example.core.persistence.PreferencesManager

/**
 * Application class providing global dependencies for economy, settings, and audio.
 */
class ColorClashApp : Application() {
  lateinit var preferencesManager: PreferencesManager
    private set
  lateinit var coinRepository: CoinRepository
    private set
  lateinit var audioManager: AudioManager
    private set

  override fun onCreate() {
    super.onCreate()
    preferencesManager = PreferencesManager(this)
    coinRepository = CoinRepository(this)
    audioManager = AudioManager(this, preferencesManager)
  }

  override fun onTerminate() {
    super.onTerminate()
    audioManager.release()
  }
}
