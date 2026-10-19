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
}
