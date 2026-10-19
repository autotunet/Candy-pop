package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Candy Pop", appName)
  }

  @Test
  fun `verify level definitions and initial board`() {
    val level1 = com.example.candy.data.LevelsData.getLevel(1)
    val board = com.example.candy.game.GameEngine.createInitialBoard(level1)
    assertEquals(8, board.size)
    assertEquals(8, board[0].size)
  }

  @Test
  fun `verify sound and haptic feedback manager with view and haptic constants`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val soundManager = com.example.candy.audio.SoundManager(context)
    val testView = android.view.View(context)

    soundManager.attachView(testView)

    // Verify haptics can be executed for match and explosions
    soundManager.isHapticsEnabled = true
    soundManager.playMatch(comboCount = 1)
    soundManager.playMatch(comboCount = 3)
    soundManager.playSpecialExplosion()
    soundManager.playColorBomb()
    soundManager.playLaser()
    soundManager.playSwap(isValid = true)
    soundManager.playSwap(isValid = false)

    // Verify disabling haptics executes cleanly
    soundManager.isHapticsEnabled = false
    soundManager.playMatch(comboCount = 1)
    soundManager.playSpecialExplosion()

    soundManager.detachView()
    soundManager.release()
  }

  @Test
  fun `verify combo streak and score multipliers scaling`() {
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = com.example.candy.ui.GameViewModel(application)

    // Verify streak multiplier progression
    assertEquals(1.0f, viewModel.getMultiplierForStreak(1), 0.001f)
    assertEquals(1.5f, viewModel.getMultiplierForStreak(2), 0.001f)
    assertEquals(2.0f, viewModel.getMultiplierForStreak(3), 0.001f)
    assertEquals(2.5f, viewModel.getMultiplierForStreak(4), 0.001f)
    assertEquals(3.0f, viewModel.getMultiplierForStreak(5), 0.001f)
    assertEquals(3.5f, viewModel.getMultiplierForStreak(6), 0.001f)
    assertEquals(4.0f, viewModel.getMultiplierForStreak(7), 0.001f)
    assertEquals(5.0f, viewModel.getMultiplierForStreak(8), 0.001f)
    assertEquals(5.0f, viewModel.getMultiplierForStreak(15), 0.001f)

    // Verify initial uiState combo fields
    viewModel.startLevel(1)
    val state = viewModel.uiState.value
    assertEquals(0, state.comboStreak)
    assertEquals(1.0f, state.comboMultiplier, 0.001f)
    assertEquals(0, state.maxComboStreak)
    assertEquals(false, state.isComboActive)
  }
}
