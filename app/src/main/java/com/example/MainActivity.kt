package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.candy.ads.AdBannerView
import com.example.candy.ads.AdManager
import com.example.candy.ui.GameScreen
import com.example.candy.ui.GameUiState
import com.example.candy.ui.GameViewModel
import com.example.candy.ui.screens.GamePlayScreen
import com.example.candy.ui.screens.HowToPlayScreen
import com.example.candy.ui.screens.MapScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Google Mobile Ads SDK with provided AdMob IDs
        AdManager.initialize(this)

        setContent {
            val viewModel: GameViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsState()
            val view = LocalView.current

            DisposableEffect(view) {
                viewModel.attachView(view)
                onDispose {
                    viewModel.detachView()
                }
            }

            MyApplicationTheme(darkTheme = uiState.isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = if (uiState.isDarkMode) Color(0xFF1F0D24) else Color(0xFFFCE4EC)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Main App Screen Content
                        Box(modifier = Modifier.weight(1f)) {
                            CandyPopApp(viewModel = viewModel, uiState = uiState)
                        }

                        // Google AdMob Banner Debajo (Bottom Banner Ad)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .background(if (uiState.isDarkMode) Color(0xFF140718) else Color(0xFFF5F5F5))
                        ) {
                            AdBannerView()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CandyPopApp(
    viewModel: GameViewModel,
    uiState: GameUiState
) {
    val progressList by viewModel.levelProgressList.collectAsState()
    val playerStats by viewModel.playerStats.collectAsState()

    Crossfade(
        targetState = uiState.currentScreen,
        label = "screen_transition"
    ) { screen ->
        when (screen) {
            GameScreen.MAP -> {
                MapScreen(
                    viewModel = viewModel,
                    progressList = progressList,
                    playerStats = playerStats,
                    onStartLevel = { levelNum, isTimerRush ->
                        viewModel.startLevel(levelNum, isTimerRush)
                    }
                )
            }
            GameScreen.PLAYING -> {
                GamePlayScreen(
                    viewModel = viewModel,
                    uiState = uiState,
                    onBackToMap = {
                        viewModel.navigateTo(GameScreen.MAP)
                    }
                )
            }
            GameScreen.HOW_TO_PLAY -> {
                HowToPlayScreen(
                    viewModel = viewModel,
                    onBack = {
                        viewModel.navigateTo(GameScreen.MAP)
                    }
                )
            }
        }
    }
}
