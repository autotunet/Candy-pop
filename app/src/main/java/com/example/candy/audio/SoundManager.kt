package com.example.candy.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.View
import com.example.R
import java.lang.ref.WeakReference
import java.util.Collections

/**
 * Sound and Tactile Haptic Feedback Manager.
 * Uses Android HapticFeedbackConstants to deliver crisp, physical satisfaction
 * for candy matching, swapping, and explosive actions, complemented with low-latency SoundPool audio.
 */
class SoundManager(private val context: Context) {
    var isSoundEnabled: Boolean = true
    var isHapticsEnabled: Boolean = true

    private var viewRef: WeakReference<View>? = null

    /**
     * Attaches an active Android View so HapticFeedbackConstants can be directly executed.
     */
    fun attachView(view: View) {
        viewRef = WeakReference(view)
    }

    /**
     * Detaches the active Android View.
     */
    fun detachView() {
        viewRef = null
    }

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

    /**
     * Executes tactile haptic feedback using Android HapticFeedbackConstants on the active View,
     * with calibrated physical vibration backing to ensure tactile satisfaction across all Android devices.
     */
    fun performTactileHaptic(
        feedbackConstant: Int,
        fallbackDurationMs: Long = 40L,
        amplitude: Int = VibrationEffect.DEFAULT_AMPLITUDE
    ) {
        if (!isHapticsEnabled) return

        val view = viewRef?.get()
        if (view != null) {
            try {
                view.performHapticFeedback(
                    feedbackConstant,
                    HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
                )
            } catch (e: Exception) {
                Log.w("SoundManager", "performHapticFeedback error: ${e.message}")
            }
        }

        // Calibrated motor vibration for physical depth and immersion
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val effect = when (feedbackConstant) {
                    HapticFeedbackConstants.KEYBOARD_TAP ->
                        VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                    HapticFeedbackConstants.LONG_PRESS ->
                        VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
                    HapticFeedbackConstants.CLOCK_TICK ->
                        VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                    else -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                            feedbackConstant == HapticFeedbackConstants.CONFIRM
                        ) {
                            VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                            feedbackConstant == HapticFeedbackConstants.REJECT
                        ) {
                            VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK)
                        } else {
                            VibrationEffect.createOneShot(fallbackDurationMs, amplitude.coerceIn(1, 255))
                        }
                    }
                }
                vibrator?.vibrate(effect)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(
                    VibrationEffect.createOneShot(fallbackDurationMs, amplitude.coerceIn(1, 255))
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(fallbackDurationMs)
            }
        } catch (_: Exception) {
            // Ignore vibration driver exceptions on non-vibrating devices
        }
    }

    /**
     * Tactile feedback for clicking/selecting a candy tile.
     */
    fun playClick() {
        playSound(soundClickId, volume = 0.5f)
        performTactileHaptic(
            feedbackConstant = HapticFeedbackConstants.KEYBOARD_TAP,
            fallbackDurationMs = 20L,
            amplitude = 110
        )
    }

    /**
     * Tactile feedback for candy swap.
     * Uses SEGMENT_TICK / CLOCK_TICK for valid swaps, and REJECT for invalid swaps.
     */
    fun playSwap(isValid: Boolean = true) {
        playSound(soundSwapId, volume = 0.8f)
        if (isValid) {
            val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                HapticFeedbackConstants.SEGMENT_TICK
            } else {
                HapticFeedbackConstants.CLOCK_TICK
            }
            performTactileHaptic(
                feedbackConstant = constant,
                fallbackDurationMs = 30L,
                amplitude = 140
            )
        } else {
            val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                HapticFeedbackConstants.REJECT
            } else {
                HapticFeedbackConstants.KEYBOARD_TAP
            }
            performTactileHaptic(
                feedbackConstant = constant,
                fallbackDurationMs = 60L,
                amplitude = 190
            )
        }
    }

    /**
     * Plays match audio clip with pitch scaling and triggers crisp HapticFeedbackConstants.CONFIRM
     * tactile feedback, scaling in physical intensity for consecutive multi-combos.
     */
    fun playMatch(comboCount: Int = 1) {
        val rate = (1.0f + (comboCount - 1) * 0.12f).coerceIn(0.7f, 1.85f)
        playSound(soundMatchId, volume = 1.0f, rate = rate)

        // HapticFeedbackConstants.CONFIRM delivers the authentic physical match click
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.CONFIRM
        } else {
            HapticFeedbackConstants.KEYBOARD_TAP
        }
        val durationMs = (30L + comboCount * 12L).coerceAtMost(90L)
        val amplitude = (160 + comboCount * 25).coerceIn(160, 255)

        performTactileHaptic(
            feedbackConstant = constant,
            fallbackDurationMs = durationMs,
            amplitude = amplitude
        )
    }

    /**
     * Plays explosive blast clip and delivers heavy HapticFeedbackConstants.LONG_PRESS tactile response
     * when wrapped candies, bombs, or obstacles detonate.
     */
    fun playSpecialExplosion() {
        playSound(soundExplodeId, volume = 1.0f, rate = 1.0f, priority = 2)

        // Powerful explosion punch
        performTactileHaptic(
            feedbackConstant = HapticFeedbackConstants.LONG_PRESS,
            fallbackDurationMs = 120L,
            amplitude = 255
        )
    }

    /**
     * Tactile response for special candy creation (striped candy, wrapped candy, color bomb).
     */
    fun playSpecialCreated() {
        playSound(soundComboId, volume = 0.9f, rate = 1.3f)
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.CONFIRM
        } else {
            HapticFeedbackConstants.KEYBOARD_TAP
        }
        performTactileHaptic(
            feedbackConstant = constant,
            fallbackDurationMs = 65L,
            amplitude = 220
        )
    }

    /**
     * Massive tactile explosion when a Color Bomb detonates across the board.
     */
    fun playColorBomb() {
        playSound(soundExplodeId, volume = 1.0f, rate = 0.9f, priority = 2)
        playSound(soundComboId, volume = 0.9f, rate = 1.4f)

        performTactileHaptic(
            feedbackConstant = HapticFeedbackConstants.LONG_PRESS,
            fallbackDurationMs = 180L,
            amplitude = 255
        )
    }

    /**
     * Tactile praise when reaching a sweet combo streak.
     */
    fun playComboPraise() {
        playSound(soundComboId, volume = 1.0f, rate = 1.15f)
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.CONFIRM
        } else {
            HapticFeedbackConstants.KEYBOARD_TAP
        }
        performTactileHaptic(
            feedbackConstant = constant,
            fallbackDurationMs = 80L,
            amplitude = 230
        )
    }

    /**
     * Fanfare and celebratory tactile haptics when completing a level.
     */
    fun playWin() {
        playSound(soundWinId, volume = 1.0f, rate = 1.0f, priority = 2)
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.CONFIRM
        } else {
            HapticFeedbackConstants.LONG_PRESS
        }
        performTactileHaptic(
            feedbackConstant = constant,
            fallbackDurationMs = 160L,
            amplitude = 255
        )
    }

    /**
     * Plays sad chime audio clip when out of moves/time.
     */
    fun playLose() {
        playSound(soundLoseId, volume = 0.9f, rate = 1.0f)
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.REJECT
        } else {
            HapticFeedbackConstants.KEYBOARD_TAP
        }
        performTactileHaptic(
            feedbackConstant = constant,
            fallbackDurationMs = 100L,
            amplitude = 180
        )
    }

    /**
     * Crisp tactile tick for the countdown timer.
     */
    fun playTick() {
        playSound(soundClickId, volume = 0.9f, rate = 1.6f)
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            HapticFeedbackConstants.SEGMENT_FREQUENT_TICK
        } else {
            HapticFeedbackConstants.CLOCK_TICK
        }
        performTactileHaptic(
            feedbackConstant = constant,
            fallbackDurationMs = 25L,
            amplitude = 120
        )
    }

    /**
     * Sharp tactile laser sweep for striped candy blasts.
     */
    fun playLaser() {
        playSound(soundComboId, volume = 1.0f, rate = 1.5f)
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            HapticFeedbackConstants.CONTEXT_CLICK
        } else {
            HapticFeedbackConstants.KEYBOARD_TAP
        }
        performTactileHaptic(
            feedbackConstant = constant,
            fallbackDurationMs = 55L,
            amplitude = 210
        )
    }

    /**
     * Heavy shockwave tactile pulse.
     */
    fun playShockwave() {
        playSound(soundExplodeId, volume = 0.9f, rate = 1.2f)
        performTactileHaptic(
            feedbackConstant = HapticFeedbackConstants.LONG_PRESS,
            fallbackDurationMs = 85L,
            amplitude = 230
        )
    }

    fun release() {
        try {
            soundPool.release()
            viewRef = null
        } catch (e: Exception) {
            Log.e("SoundManager", "Error releasing SoundPool: ${e.message}")
        }
    }
}
