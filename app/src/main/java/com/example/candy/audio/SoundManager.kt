package com.example.candy.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.example.R
import java.util.Collections

class SoundManager(private val context: Context) {
    var isSoundEnabled: Boolean = true
    var isHapticsEnabled: Boolean = true

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    // Google Android SoundPool instance for zero-latency game audio clips
    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(10)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val loadedSoundIds = Collections.synchronizedSet(mutableSetOf<Int>())

    // Sound effect clip resource IDs
    private val soundMatchId: Int
    private val soundExplodeId: Int
    private val soundSwapId: Int
    private val soundWinId: Int
    private val soundLoseId: Int
    private val soundComboId: Int
    private val soundClickId: Int

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                loadedSoundIds.add(sampleId)
            } else {
                Log.w("SoundManager", "Failed to load sample ID: $sampleId")
            }
        }

        soundMatchId = soundPool.load(context, R.raw.snd_match, 1)
        soundExplodeId = soundPool.load(context, R.raw.snd_explode, 1)
        soundSwapId = soundPool.load(context, R.raw.snd_swap, 1)
        soundWinId = soundPool.load(context, R.raw.snd_level_win, 1)
        soundLoseId = soundPool.load(context, R.raw.snd_level_lose, 1)
        soundComboId = soundPool.load(context, R.raw.snd_combo, 1)
        soundClickId = soundPool.load(context, R.raw.snd_click, 1)
    }

    private fun playSound(soundId: Int, volume: Float = 1.0f, rate: Float = 1.0f, priority: Int = 1) {
        if (!isSoundEnabled) return
        try {
            soundPool.play(soundId, volume, volume, priority, 0, rate.coerceIn(0.5f, 2.0f))
        } catch (e: Exception) {
            Log.e("SoundManager", "Error playing sound: ${e.message}")
        }
    }

    fun playClick() {
        playSound(soundClickId, volume = 0.5f)
    }

    fun playSwap() {
        playSound(soundSwapId, volume = 0.8f)
        vibrate(30)
    }

    /**
     * Plays match audio clip with pitch scaling on multi-combos
     */
    fun playMatch(comboCount: Int = 1) {
        val rate = (1.0f + (comboCount - 1) * 0.12f).coerceIn(0.7f, 1.85f)
        playSound(soundMatchId, volume = 1.0f, rate = rate)
        vibrate(40)
    }

    /**
     * Plays explosive blast clip when special candies / wrapped candies detonate
     */
    fun playSpecialExplosion() {
        playSound(soundExplodeId, volume = 1.0f, rate = 1.0f, priority = 2)
        vibrate(100)
    }

    fun playSpecialCreated() {
        playSound(soundComboId, volume = 0.9f, rate = 1.3f)
        vibrate(70)
    }

    fun playColorBomb() {
        playSound(soundExplodeId, volume = 1.0f, rate = 0.9f, priority = 2)
        playSound(soundComboId, volume = 0.9f, rate = 1.4f)
        vibrate(120)
    }

    fun playComboPraise() {
        playSound(soundComboId, volume = 1.0f, rate = 1.15f)
        vibrate(80)
    }

    /**
     * Plays triumphant fanfare audio clip when a level is completed
     */
    fun playWin() {
        playSound(soundWinId, volume = 1.0f, rate = 1.0f, priority = 2)
        vibrate(150)
    }

    /**
     * Plays sad chime audio clip when out of moves
     */
    fun playLose() {
        playSound(soundLoseId, volume = 0.9f, rate = 1.0f)
    }

    fun playTick() {
        playSound(soundClickId, volume = 0.9f, rate = 1.6f)
        vibrate(25)
    }

    fun playLaser() {
        playSound(soundComboId, volume = 1.0f, rate = 1.5f)
        vibrate(60)
    }

    fun playShockwave() {
        playSound(soundExplodeId, volume = 0.9f, rate = 1.2f)
        vibrate(80)
    }

    private fun vibrate(durationMs: Long) {
        if (!isHapticsEnabled) return
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
            // Ignore haptic failures
        }
    }

    fun release() {
        try {
            soundPool.release()
        } catch (e: Exception) {
            Log.e("SoundManager", "Error releasing SoundPool: ${e.message}")
        }
    }
}
