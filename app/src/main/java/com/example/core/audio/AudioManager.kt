package com.example.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.core.persistence.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import kotlin.math.PI
import kotlin.math.sin

/**
 * Full Sound & Music Manager for Offline UNO!.
 * Features:
 * - Generates high quality synthesized 16-bit PCM WAV sound effects on first run
 * - Loads them into SoundPool for zero-latency, reliable audio playback
 * - Synthesizes soft, ambient background music played via MediaPlayer / AudioTrack
 * - Obeys preferencesManager.isSfxEnabled, isMusicEnabled, and isVibrationEnabled
 * - Lifecycle-safe with release()
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
  private val soundIds = mutableMapOf<SoundType, Int>()

  private var bgmTrack: AudioTrack? = null
  private var bgmJob: Job? = null
  private val audioScope = CoroutineScope(Dispatchers.IO)

  enum class SoundType {
    BUTTON_CLICK,
    CARD_PLAY,
    CARD_DRAW,
    SPECIAL_ACTION,
    UNO_SHOUT,
    UNO_PENALTY,
    WIN,
    LOSE
  }

  init {
    val audioAttributes = AudioAttributes.Builder()
      .setUsage(AudioAttributes.USAGE_GAME)
      .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
      .build()
    soundPool = SoundPool.Builder()
      .setMaxStreams(8)
      .setAudioAttributes(audioAttributes)
      .build()

    initializeSynthesizedSounds()
    startBackgroundMusicIfEnabled()
  }

  private fun initializeSynthesizedSounds() {
    try {
      val cacheDir = File(context.cacheDir, "uno_sfx").apply { mkdirs() }

      // Generate pleasant game sound effect WAVs
      loadSynthSound(SoundType.BUTTON_CLICK, createToneWav(cacheDir, "click.wav", 800.0, 50, 0.5))
      loadSynthSound(SoundType.CARD_PLAY, createToneWav(cacheDir, "card_play.wav", 440.0, 70, 0.6))
      loadSynthSound(SoundType.CARD_DRAW, createToneWav(cacheDir, "card_draw.wav", 320.0, 60, 0.5))
      loadSynthSound(SoundType.SPECIAL_ACTION, createArpeggioWav(cacheDir, "special.wav", listOf(523.25, 659.25, 783.99), 60))
      loadSynthSound(SoundType.UNO_SHOUT, createArpeggioWav(cacheDir, "uno.wav", listOf(440.0, 880.0), 90))
      loadSynthSound(SoundType.UNO_PENALTY, createToneWav(cacheDir, "penalty.wav", 180.0, 150, 0.8))
      loadSynthSound(SoundType.WIN, createArpeggioWav(cacheDir, "win.wav", listOf(523.25, 659.25, 783.99, 1046.50), 120))
      loadSynthSound(SoundType.LOSE, createArpeggioWav(cacheDir, "lose.wav", listOf(400.0, 300.0, 200.0), 120))
    } catch (_: Exception) {
      // Audio fallback
    }
  }

  private fun loadSynthSound(type: SoundType, wavFile: File) {
    if (wavFile.exists()) {
      val id = soundPool.load(wavFile.absolutePath, 1)
      soundIds[type] = id
    }
  }

  fun playButtonClick() {
    playSound(SoundType.BUTTON_CLICK, 0.8f)
    triggerHaptic(25)
  }

  fun playCardPlay() {
    playSound(SoundType.CARD_PLAY, 0.9f)
    triggerHaptic(40)
  }

  fun playCardDraw() {
    playSound(SoundType.CARD_DRAW, 0.8f)
    triggerHaptic(30)
  }

  fun playSpecialAction() {
    playSound(SoundType.SPECIAL_ACTION, 0.9f)
    triggerHaptic(60)
  }

  fun playUnoShout() {
    playSound(SoundType.UNO_SHOUT, 1.0f)
    triggerHaptic(80)
  }

  fun playUnoPenalty() {
    playSound(SoundType.UNO_PENALTY, 1.0f)
    triggerHaptic(100)
  }

  fun playTurnAlert() {
    playSound(SoundType.BUTTON_CLICK, 0.7f)
    triggerHaptic(50)
  }

  fun playWin() {
    playSound(SoundType.WIN, 1.0f)
    triggerHaptic(120)
  }

  fun playLose() {
    playSound(SoundType.LOSE, 0.9f)
    triggerHaptic(80)
  }

  private fun playSound(type: SoundType, volume: Float) {
    if (!preferencesManager.isSfxEnabled) return
    val id = soundIds[type] ?: return
    soundPool.play(id, volume, volume, 1, 0, 1.0f)
  }

  fun startBackgroundMusicIfEnabled() {
    if (!preferencesManager.isMusicEnabled) {
      stopBackgroundMusic()
      return
    }
    if (bgmJob != null && bgmJob?.isActive == true) return

    bgmJob = audioScope.launch {
      try {
        val sampleRate = 22050
        val bufferSize = AudioTrack.getMinBufferSize(
          sampleRate,
          AudioFormat.CHANNEL_OUT_MONO,
          AudioFormat.ENCODING_PCM_16BIT
        )
        val track = AudioTrack.Builder()
          .setAudioAttributes(
            AudioAttributes.Builder()
              .setUsage(AudioAttributes.USAGE_GAME)
              .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
              .build()
          )
          .setAudioFormat(
            AudioFormat.Builder()
              .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
              .setSampleRate(sampleRate)
              .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
              .build()
          )
          .setBufferSizeInBytes(bufferSize)
          .setTransferMode(AudioTrack.MODE_STREAM)
          .build()

        bgmTrack = track
        track.play()

        // Calming pentatonic melody sequence for casual card table
        val notes = listOf(261.63, 293.66, 329.63, 392.00, 440.00, 392.00, 329.63, 293.66)
        var noteIdx = 0

        val shortBuf = ShortArray(sampleRate / 4) // 250ms per note

        while (isActive && preferencesManager.isMusicEnabled) {
          val freq = notes[noteIdx % notes.size]
          noteIdx++
          for (i in shortBuf.indices) {
            val angle = 2.0 * PI * i / (sampleRate / freq)
            val envelope = sin(PI * i / shortBuf.size) // Soft windowing
            shortBuf[i] = (sin(angle) * envelope * 2400.0).toInt().toShort() // Gentle background volume
          }
          track.write(shortBuf, 0, shortBuf.size)
          delay(20)
        }
      } catch (_: Exception) {
      } finally {
        try {
          bgmTrack?.stop()
          bgmTrack?.release()
        } catch (_: Exception) {}
        bgmTrack = null
      }
    }
  }

  fun stopBackgroundMusic() {
    bgmJob?.cancel()
    bgmJob = null
    try {
      bgmTrack?.stop()
      bgmTrack?.release()
    } catch (_: Exception) {}
    bgmTrack = null
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
    } catch (_: Exception) {}
  }

  fun release() {
    stopBackgroundMusic()
    soundPool.release()
  }

  // --- WAV File Generation Helpers ---
  private fun createToneWav(dir: File, name: String, freq: Double, durationMs: Int, vol: Double): File {
    val file = File(dir, name)
    if (file.exists() && file.length() > 0) return file

    val sampleRate = 22050
    val numSamples = (sampleRate * (durationMs / 1000.0)).toInt().coerceAtLeast(100)
    val pcm = ShortArray(numSamples)

    for (i in 0 until numSamples) {
      val t = i.toDouble() / sampleRate
      val envelope = (1.0 - (i.toDouble() / numSamples)) // Decay
      val sample = sin(2.0 * PI * freq * t) * envelope * vol * 32767.0
      pcm[i] = sample.toInt().coerceIn(-32768, 32767).toShort()
    }
    writeWav(file, pcm, sampleRate)
    return file
  }

  private fun createArpeggioWav(dir: File, name: String, freqs: List<Double>, noteDurationMs: Int): File {
    val file = File(dir, name)
    if (file.exists() && file.length() > 0) return file

    val sampleRate = 22050
    val noteSamples = (sampleRate * (noteDurationMs / 1000.0)).toInt().coerceAtLeast(100)
    val totalSamples = noteSamples * freqs.size
    val pcm = ShortArray(totalSamples)

    for (n in freqs.indices) {
      val freq = freqs[n]
      val offset = n * noteSamples
      for (i in 0 until noteSamples) {
        val t = i.toDouble() / sampleRate
        val env = 1.0 - (i.toDouble() / noteSamples * 0.5)
        val sample = sin(2.0 * PI * freq * t) * env * 0.7 * 32767.0
        pcm[offset + i] = sample.toInt().coerceIn(-32768, 32767).toShort()
      }
    }
    writeWav(file, pcm, sampleRate)
    return file
  }

  private fun writeWav(file: File, pcm: ShortArray, sampleRate: Int) {
    val byteData = ByteArray(pcm.size * 2)
    for (i in pcm.indices) {
      val s = pcm[i]
      byteData[i * 2] = (s.toInt() and 0x00FF).toByte()
      byteData[i * 2 + 1] = ((s.toInt() and 0xFF00) shr 8).toByte()
    }

    val totalAudioLen = byteData.size.toLong()
    val totalDataLen = totalAudioLen + 36
    val byteRate = sampleRate * 2

    FileOutputStream(file).use { out ->
      val header = ByteArray(44)
      header[0] = 'R'.code.toByte(); header[1] = 'I'.code.toByte(); header[2] = 'F'.code.toByte(); header[3] = 'F'.code.toByte()
      header[4] = (totalDataLen and 0xff).toByte()
      header[5] = ((totalDataLen shr 8) and 0xff).toByte()
      header[6] = ((totalDataLen shr 16) and 0xff).toByte()
      header[7] = ((totalDataLen shr 24) and 0xff).toByte()
      header[8] = 'W'.code.toByte(); header[9] = 'A'.code.toByte(); header[10] = 'V'.code.toByte(); header[11] = 'E'.code.toByte()
      header[12] = 'f'.code.toByte(); header[13] = 'm'.code.toByte(); header[14] = 't'.code.toByte(); header[15] = ' '.code.toByte()
      header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0
      header[20] = 1; header[21] = 0 // PCM
      header[22] = 1; header[23] = 0 // Mono
      header[24] = (sampleRate and 0xff).toByte()
      header[25] = ((sampleRate shr 8) and 0xff).toByte()
      header[26] = ((sampleRate shr 16) and 0xff).toByte()
      header[27] = ((sampleRate shr 24) and 0xff).toByte()
      header[28] = (byteRate and 0xff).toByte()
      header[29] = ((byteRate shr 8) and 0xff).toByte()
      header[30] = ((byteRate shr 16) and 0xff).toByte()
      header[31] = ((byteRate shr 24) and 0xff).toByte()
      header[32] = 2; header[33] = 0 // Block align
      header[34] = 16; header[35] = 0 // Bits per sample
      header[36] = 'd'.code.toByte(); header[37] = 'a'.code.toByte(); header[38] = 't'.code.toByte(); header[39] = 'a'.code.toByte()
      header[40] = (totalAudioLen and 0xff).toByte()
      header[41] = ((totalAudioLen shr 8) and 0xff).toByte()
      header[42] = ((totalAudioLen shr 16) and 0xff).toByte()
      header[43] = ((totalAudioLen shr 24) and 0xff).toByte()

      out.write(header)
      out.write(byteData)
    }
  }
}
