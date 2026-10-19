package com.example.candy.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.candy.ads.AdManager
import com.example.candy.data.LevelsData
import com.example.candy.game.GameEngine
import com.example.candy.model.*
import com.example.candy.ui.GameScreen
import com.example.candy.ui.GameUiState
import com.example.candy.ui.GameViewModel
import com.example.candy.ui.components.*
import com.example.candy.util.AppLanguage
import com.example.candy.util.Localization
import com.example.ui.theme.*
import kotlin.math.abs

@Composable
fun GamePlayScreen(
    viewModel: GameViewModel,
    uiState: GameUiState,
    onBackToMap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = uiState.language
    val strings = Localization.getStrings(lang)
    val context = LocalContext.current
    val activity = context as? Activity
    val view = LocalView.current

    DisposableEffect(view) {
        viewModel.attachView(view)
        onDispose { }
    }

    // Screen Shake dynamic juice
    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(uiState.screenShakeIntensity) {
        if (uiState.screenShakeIntensity > 0f) {
            shakeOffset.animateTo(uiState.screenShakeIntensity, spring(stiffness = Spring.StiffnessHigh))
            shakeOffset.animateTo(-uiState.screenShakeIntensity * 0.7f, spring(stiffness = Spring.StiffnessHigh))
            shakeOffset.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
        }
    }

    // Handle back button: show pause dialog if playing
    BackHandler {
        if (!uiState.isGameOver) {
            viewModel.togglePause()
        } else {
            onBackToMap()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF880E4F),
                        Color(0xFF4A148C),
                        Color(0xFF311B92)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("gameplay_screen")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar: Level info, moves left, timer, pause
            GameTopHeader(
                level = uiState.currentLevel,
                movesLeft = uiState.movesLeft,
                timeLeftSeconds = uiState.timeLeftSeconds,
                elapsedSeconds = uiState.elapsedTimeSeconds,
                strings = strings,
                onPauseClick = { viewModel.togglePause() },
                onRestartClick = { viewModel.startLevel(uiState.currentLevel.levelNumber) }
            )

            // Star Score Progress Bar
            ScoreProgressBar(
                currentScore = uiState.currentScore,
                starScores = uiState.currentLevel.starScores,
                starsEarned = uiState.starsEarned,
                scoreLabel = strings.score
            )

            // Objective Status Bar
            ObjectivesStatusBar(
                level = uiState.currentLevel,
                jelliesRemaining = uiState.jelliesRemaining,
                collectedIngredients = uiState.collectedIngredients,
                createdSpecials = uiState.createdSpecials,
                lang = lang,
                strings = strings
            )

            // Visual Combo Meter UI Element
            ComboMeter(
                uiState = uiState,
                strings = strings
            )

            // Active Booster Prompt (if armed)
            uiState.activeBooster?.let { booster ->
                ActiveBoosterPrompt(
                    booster = booster,
                    strings = strings,
                    onCancel = { viewModel.selectBooster(booster) }
                )
            }

            Spacer(modifier = Modifier.weight(0.5f))

            // 8x8 Board Container with Screen Shake and Modern Visual Effects Overlays
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .aspectRatio(1f)
                    .offset(x = shakeOffset.value.dp, y = (shakeOffset.value * 0.5f).dp),
                contentAlignment = Alignment.Center
            ) {
                // Main Candy Grid
                CandyBoard(
                    uiState = uiState,
                    onTileClick = { pos -> viewModel.onTileClick(pos) },
                    onTileSwipe = { from, dr, dc -> viewModel.onTileSwipe(from, dr, dc) }
                )

                // 1. Expanding Shockwaves Overlay
                ShockwavesOverlay(shockwaves = uiState.shockwaves)

                // 2. Laser Energy Beams Overlay
                LaserBeamsOverlay(lasers = uiState.lasers)

                // 3. Rainbow Tentacles Overlay for Color Bomb
                RainbowTentaclesOverlay(tentacles = uiState.rainbowTentacles)

                // 4. Particle Sparks & Stars Overlay
                ParticlesOverlay(particles = uiState.particles)

                // 5. Floating Scores (+60, +120, +500)
                FloatingScoresOverlay(scores = uiState.floatingScores)

                // 6. Combo Praise Banner (Sweet!, Tasty!, Delicious!, Sugar Crush!)
                ComboAnnouncementBanner(
                    text = uiState.comboText,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            Spacer(modifier = Modifier.weight(0.5f))

            // Bottom Booster Actions Bar
            BoostersActionBar(
                viewModel = viewModel,
                activeBooster = uiState.activeBooster,
                strings = strings,
                onHintClick = { viewModel.requestHint() }
            )
        }

        // Win Modal
        if (uiState.isGameOver && uiState.isGameWon) {
            LevelWinDialog(
                score = uiState.currentScore,
                stars = uiState.starsEarned,
                maxStreak = uiState.maxComboStreak,
                levelNumber = uiState.currentLevel.levelNumber,
                hasNextLevel = uiState.currentLevel.levelNumber < LevelsData.TOTAL_LEVELS,
                strings = strings,
                onNextLevel = {
                    if (activity != null) {
                        AdManager.showAutomaticInterstitial(activity) {
                            viewModel.startLevel(uiState.currentLevel.levelNumber + 1)
                        }
                    } else {
                        viewModel.startLevel(uiState.currentLevel.levelNumber + 1)
                    }
                },
                onReplay = {
                    if (activity != null) {
                        AdManager.showAutomaticInterstitial(activity) {
                            viewModel.startLevel(uiState.currentLevel.levelNumber)
                        }
                    } else {
                        viewModel.startLevel(uiState.currentLevel.levelNumber)
                    }
                },
                onMap = {
                    if (activity != null) {
                        AdManager.showAutomaticInterstitial(activity) {
                            onBackToMap()
                        }
                    } else {
                        onBackToMap()
                    }
                }
            )
        }

        // Game Over Modal (Out of Moves OR Timed Out)
        if (uiState.isGameOver && !uiState.isGameWon) {
            LevelGameOverDialog(
                score = uiState.currentScore,
                movesLeft = uiState.movesLeft,
                isTimedOut = uiState.isTimedOut,
                strings = strings,
                onAddMoves = { viewModel.useExtraMovesBooster() },
                onAddTime = { viewModel.addExtraTime(15) },
                onWatchAdForReward = {
                    if (activity != null) {
                        AdManager.showAutomaticInterstitial(activity) {
                            if (uiState.isTimedOut) {
                                viewModel.addExtraTime(15)
                            } else {
                                viewModel.useExtraMovesBooster()
                            }
                        }
                    }
                },
                onRetry = {
                    if (activity != null) {
                        AdManager.showAutomaticInterstitial(activity) {
                            viewModel.startLevel(uiState.currentLevel.levelNumber)
                        }
                    } else {
                        viewModel.startLevel(uiState.currentLevel.levelNumber)
                    }
                },
                onMap = {
                    if (activity != null) {
                        AdManager.showAutomaticInterstitial(activity) {
                            onBackToMap()
                        }
                    } else {
                        onBackToMap()
                    }
                }
            )
        }

        // Pause Modal
        if (uiState.isPaused) {
            PauseDialog(
                viewModel = viewModel,
                currentLang = lang,
                onToggleLanguage = {
                    val nextLang = if (lang == AppLanguage.ES) AppLanguage.EN else AppLanguage.ES
                    viewModel.setLanguage(nextLang)
                },
                strings = strings,
                onResume = { viewModel.togglePause() },
                onRestart = { viewModel.startLevel(uiState.currentLevel.levelNumber) },
                onMap = onBackToMap,
                onWatchAd = {
                    if (activity != null) {
                        AdManager.showAutomaticInterstitial(activity) {
                            viewModel.rewardForWatchingAd()
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun GameTopHeader(
    level: LevelDef,
    movesLeft: Int,
    timeLeftSeconds: Int?,
    elapsedSeconds: Int,
    strings: com.example.candy.util.AppStrings,
    onPauseClick: () -> Unit,
    onRestartClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(
            onClick = onPauseClick,
            modifier = Modifier
                .size(42.dp)
                .background(Color(0x44FFFFFF), CircleShape)
                .testTag("pause_button")
        ) {
            Icon(Icons.Default.Pause, contentDescription = strings.gamePaused, tint = Color.White)
        }

        // Moves Counter Badge
        val movesColor = if (movesLeft <= 5) CandyRed else CandyYellow
        Surface(
            color = Color(0xDD2A0845),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(2.dp, movesColor),
            modifier = Modifier
                .animateContentSize()
                .testTag("moves_counter")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = strings.moves,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Text(
                    text = "$movesLeft",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = movesColor
                )
            }
        }

        // Temporizador / Timer Badge (Active Countdown or Stopwatch)
        val isCountdown = (timeLeftSeconds != null)
        val secondsToShow = timeLeftSeconds ?: elapsedSeconds
        val minutes = secondsToShow / 60
        val seconds = secondsToShow % 60
        val timeFormatted = String.format("%02d:%02d", minutes, seconds)
        val isUrgent = isCountdown && (secondsToShow <= 10)

        val infiniteTransition = rememberInfiniteTransition(label = "urgent_timer")
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = if (isUrgent) 1.15f else 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(380, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        )

        Surface(
            color = if (isUrgent) Color(0xDDE91E63) else Color(0xDD1A052C),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(
                2.dp,
                if (isUrgent) Color(0xFFFF5252) else if (isCountdown) Color(0xFF00E5FF) else Color(0x88FFFFFF)
            ),
            modifier = Modifier
                .scale(if (isUrgent) pulseScale else 1f)
                .testTag("timer_badge")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(text = "⏱️", fontSize = 13.sp)
                Text(
                    text = timeFormatted,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isUrgent) Color(0xFFFFEB3B) else Color.White
                )
            }
        }

        IconButton(
            onClick = onRestartClick,
            modifier = Modifier
                .size(42.dp)
                .background(Color(0x44FFFFFF), CircleShape)
                .testTag("restart_button")
        ) {
            Icon(Icons.Default.Refresh, contentDescription = strings.restartLevel, tint = Color.White)
        }
    }
}

@Composable
private fun ScoreProgressBar(
    currentScore: Int,
    starScores: Triple<Int, Int, Int>,
    starsEarned: Int,
    scoreLabel: String
) {
    val maxScore = starScores.third.toFloat()
    val progress = (currentScore / maxScore).coerceIn(0f, 1f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$scoreLabel: $currentScore",
                fontWeight = FontWeight.Black,
                fontSize = 15.sp,
                color = Color.White
            )
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                for (i in 1..3) {
                    Text(
                        text = if (i <= starsEarned) "⭐" else "☆",
                        fontSize = 16.sp,
                        color = if (i <= starsEarned) GoldStar else Color.White.copy(alpha = 0.5f)
                    )
                }
            }
        }

        // Progress track with star nodes
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(Color(0x55000000))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .clip(RoundedCornerShape(7.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFFF5252),
                                Color(0xFFFFEB3B),
                                Color(0xFF00E676)
                            )
                        )
                    )
            )

            // Star thresholds on the track
            val s1Frac = (starScores.first / maxScore).coerceIn(0.1f, 0.9f)
            val s2Frac = (starScores.second / maxScore).coerceIn(0.2f, 0.95f)

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(s1Frac)
            ) {
                Box(
                    modifier = Modifier
                        .size(3.dp, 14.dp)
                        .background(Color.White)
                        .align(Alignment.CenterEnd)
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(s2Frac)
            ) {
                Box(
                    modifier = Modifier
                        .size(3.dp, 14.dp)
                        .background(Color.White)
                        .align(Alignment.CenterEnd)
                )
            }
        }
    }
}

@Composable
private fun ObjectivesStatusBar(
    level: LevelDef,
    jelliesRemaining: Int,
    collectedIngredients: Map<CandyColor, Int>,
    createdSpecials: Map<SpecialCandyType, Int>,
    lang: AppLanguage,
    strings: com.example.candy.util.AppStrings
) {
    Surface(
        color = Color(0x66000000),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .testTag("objectives_bar")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Target Score
            ObjectiveChip(
                icon = "🎯",
                current = null,
                target = level.targetScore,
                label = strings.score
            )

            // Jelly Goal
            if (level.targetJellies > 0) {
                ObjectiveChip(
                    icon = "🍮",
                    current = jelliesRemaining,
                    target = level.targetJellies,
                    isCountdown = true
                )
            }

            // Ingredient Goals
            for ((color, target) in level.targetIngredients) {
                val current = collectedIngredients[color] ?: 0
                ObjectiveChip(
                    icon = when (color) {
                        CandyColor.RED -> "🍓"
                        CandyColor.YELLOW -> "🍋"
                        CandyColor.ORANGE -> "🍊"
                        CandyColor.GREEN -> "🍏"
                        CandyColor.BLUE -> "🫐"
                        CandyColor.PURPLE -> "🍇"
                    },
                    current = current,
                    target = target
                )
            }

            // Specials Goals
            for ((spec, target) in level.targetSpecials) {
                val current = createdSpecials[spec] ?: 0
                ObjectiveChip(
                    icon = "✨",
                    current = current,
                    target = target
                )
            }
        }
    }
}

@Composable
private fun ObjectiveChip(
    icon: String,
    current: Int?,
    target: Int,
    label: String = "",
    isCountdown: Boolean = false
) {
    val isDone = if (isCountdown) (current ?: 0) <= 0 else (current ?: 0) >= target

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = icon, fontSize = 14.sp)
        if (isDone && current != null) {
            Text(text = "✓", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFF00E676))
        } else {
            val text = if (current != null) {
                if (isCountdown) "$current" else "$current/$target"
            } else {
                "$target $label"
            }
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun ActiveBoosterPrompt(
    booster: BoosterType,
    strings: com.example.candy.util.AppStrings,
    onCancel: () -> Unit
) {
    val boosterDesc = when (booster) {
        BoosterType.LOLLIPOP_HAMMER -> strings.hammerDesc
        BoosterType.FREE_SWITCH -> strings.switchDesc
        BoosterType.COLOR_BOMB -> strings.bombDesc
        BoosterType.EXTRA_MOVES -> strings.extraMovesDesc
    }

    Surface(
        color = Color(0xDDFF007F),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(2.dp, Color.White),
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .testTag("active_booster_prompt")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "${booster.iconName} $boosterDesc",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onCancel) {
                Text(text = strings.cancel, color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun CandyBoard(
    uiState: GameUiState,
    onTileClick: (Position) -> Unit,
    onTileSwipe: (Position, Int, Int) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x33000000))
            .border(2.dp, Color(0x66FFFFFF), RoundedCornerShape(20.dp))
            .testTag("candy_board")
    ) {
        val boardSize = maxWidth
        val cellSize = boardSize / GameEngine.COLS

        var dragStartPos by remember { mutableStateOf<Position?>(null) }
        var dragOffset by remember { mutableStateOf(Offset.Zero) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(uiState.board) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val c = (offset.x / (size.width / GameEngine.COLS)).toInt().coerceIn(0, 7)
                            val r = (offset.y / (size.height / GameEngine.ROWS)).toInt().coerceIn(0, 7)
                            dragStartPos = Position(r, c)
                            dragOffset = Offset.Zero
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragOffset += dragAmount
                        },
                        onDragEnd = {
                            val start = dragStartPos
                            if (start != null) {
                                val dx = dragOffset.x
                                val dy = dragOffset.y
                                val threshold = 30f
                                if (abs(dx) > threshold || abs(dy) > threshold) {
                                    if (abs(dx) > abs(dy)) {
                                        val dc = if (dx > 0) 1 else -1
                                        onTileSwipe(start, 0, dc)
                                    } else {
                                        val dr = if (dy > 0) 1 else -1
                                        onTileSwipe(start, dr, 0)
                                    }
                                } else {
                                    onTileClick(start)
                                }
                            }
                            dragStartPos = null
                            dragOffset = Offset.Zero
                        },
                        onDragCancel = {
                            dragStartPos = null
                            dragOffset = Offset.Zero
                        }
                    )
                }
        ) {
            // Render 8x8 checkerboard tiles & Animated Candies
            for (r in 0 until GameEngine.ROWS) {
                for (c in 0 until GameEngine.COLS) {
                    val tile = uiState.board.getOrNull(r)?.getOrNull(c)
                    val pos = Position(r, c)
                    val isSelected = uiState.selectedPosition == pos
                    val isHinted = uiState.hintPair?.first == pos || uiState.hintPair?.second == pos

                    val cellX = cellSize * c
                    val cellY = cellSize * r

                    Box(
                        modifier = Modifier
                            .offset(x = cellX, y = cellY)
                            .size(cellSize)
                            .background(
                                if ((r + c) % 2 == 0) Color(0x22FFFFFF) else Color(0x11000000)
                            )
                            .clickable { onTileClick(pos) }
                            .testTag("tile_${r}_${c}")
                    ) {
                        if (tile != null) {
                            CandyTileView(
                                tile = tile,
                                isSelected = isSelected,
                                isHinted = isHinted
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BoostersActionBar(
    viewModel: GameViewModel,
    activeBooster: BoosterType?,
    strings: com.example.candy.util.AppStrings,
    onHintClick: () -> Unit
) {
    val stats by viewModel.playerStats.collectAsState()
    val hammers = stats?.hammers ?: 3
    val switches = stats?.freeSwitches ?: 3
    val bombs = stats?.colorBombs ?: 2
    val extraMoves = stats?.extraMoves ?: 2

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BoosterButton(
            icon = "🔨",
            title = strings.hammerTitle,
            count = hammers,
            isActive = activeBooster == BoosterType.LOLLIPOP_HAMMER,
            onClick = { viewModel.selectBooster(BoosterType.LOLLIPOP_HAMMER) }
        )
        BoosterButton(
            icon = "⇄",
            title = strings.switchTitle,
            count = switches,
            isActive = activeBooster == BoosterType.FREE_SWITCH,
            onClick = { viewModel.selectBooster(BoosterType.FREE_SWITCH) }
        )
        BoosterButton(
            icon = "💣",
            title = strings.bombTitle,
            count = bombs,
            isActive = activeBooster == BoosterType.COLOR_BOMB,
            onClick = { viewModel.selectBooster(BoosterType.COLOR_BOMB) }
        )
        BoosterButton(
            icon = "+5",
            title = strings.extraMovesTitle,
            count = extraMoves,
            isActive = activeBooster == BoosterType.EXTRA_MOVES,
            onClick = { viewModel.useExtraMovesBooster() }
        )
        // Hint Button
        IconButton(
            onClick = onHintClick,
            modifier = Modifier
                .size(48.dp)
                .background(Color(0x33FFFFFF), CircleShape)
                .testTag("hint_button")
        ) {
            Text(text = "💡", fontSize = 20.sp)
        }
    }
}

@Composable
private fun BoosterButton(
    icon: String,
    title: String,
    count: Int,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isActive) Color(0xFFFFEB3B) else Color(0x33FFFFFF)
    val textColor = if (isActive) Color.Black else Color.White

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Text(text = icon, fontSize = 22.sp)
            if (count > 0) {
                Text(
                    text = "$count",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    modifier = Modifier
                        .background(CandyPink, CircleShape)
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }
        Text(
            text = title,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
private fun LevelWinDialog(
    score: Int,
    stars: Int,
    maxStreak: Int = 0,
    levelNumber: Int,
    hasNextLevel: Boolean,
    strings: com.example.candy.util.AppStrings,
    onNextLevel: () -> Unit,
    onReplay: () -> Unit,
    onMap: () -> Unit
) {
    Dialog(onDismissRequest = {}) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("win_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = strings.sweetVictory,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = CandyPinkDark,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "${strings.levelCompleted} ($levelNumber)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = CandyPurpleDark,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                // Animated Stars
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    for (i in 1..3) {
                        Text(
                            text = if (i <= stars) "⭐" else "☆",
                            fontSize = 40.sp,
                            color = if (i <= stars) GoldStar else Color.LightGray
                        )
                    }
                }

                Surface(
                    color = Color(0xFFF3E5F5),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = strings.finalScore, fontSize = 13.sp, color = Color.Gray)
                        Text(
                            text = "$score",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = CandyPurpleDark
                        )
                        Text(
                            text = "+${stars * 50} 🪙 ${strings.coinsEarned}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE65100),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        if (maxStreak > 1) {
                            Text(
                                text = "🔥 ${strings.maxStreak}: ${maxStreak}x",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = CandyOrange,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                if (hasNextLevel) {
                    Button(
                        onClick = onNextLevel,
                        colors = ButtonDefaults.buttonColors(containerColor = CandyPink),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("next_level_button")
                    ) {
                        Text(
                            text = strings.nextLevel,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onReplay,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("replay_button")
                    ) {
                        Text(strings.replay, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onMap,
                        colors = ButtonDefaults.buttonColors(containerColor = CandyPurple),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("map_button")
                    ) {
                        Text(strings.map, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun LevelGameOverDialog(
    score: Int,
    movesLeft: Int,
    isTimedOut: Boolean,
    strings: com.example.candy.util.AppStrings,
    onAddMoves: () -> Unit,
    onAddTime: () -> Unit,
    onWatchAdForReward: () -> Unit,
    onRetry: () -> Unit,
    onMap: () -> Unit
) {
    Dialog(onDismissRequest = {}) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("game_over_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isTimedOut) strings.timeUp else strings.outOfMoves,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = CandyRed,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = if (isTimedOut) "¡Se acabó el tiempo! Agrega segundos o mira un anuncio para continuar." else strings.outOfMovesDesc,
                    fontSize = 13.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                // Extra Moves or Extra Time Offer Button (coins)
                Button(
                    onClick = if (isTimedOut) onAddTime else onAddMoves,
                    colors = ButtonDefaults.buttonColors(containerColor = CandyPink),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("continue_offer_button")
                ) {
                    Text(
                        text = if (isTimedOut) strings.addTimeOffer else strings.addMovesOffer,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // AdMob Watch Ad Option ("Anuncios emergentes automáticos y recompensados")
                Button(
                    onClick = onWatchAdForReward,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8F00)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("watch_ad_continue_button")
                ) {
                    Text(
                        text = if (isTimedOut) "📺 Ver Anuncio (+15s Gratis)" else "📺 Ver Anuncio (+5 Movs Gratis)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onRetry,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("retry_button")
                    ) {
                        Text(strings.retry, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onMap,
                        colors = ButtonDefaults.buttonColors(containerColor = CandyPurple),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("game_over_map_button")
                    ) {
                        Text(strings.map, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun PauseDialog(
    viewModel: GameViewModel,
    currentLang: AppLanguage,
    onToggleLanguage: () -> Unit,
    strings: com.example.candy.util.AppStrings,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onMap: () -> Unit,
    onWatchAd: () -> Unit
) {
    Dialog(onDismissRequest = onResume) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("pause_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = strings.gamePaused,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = CandyPurpleDark,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Audio & Language Controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = viewModel.soundManager.isSoundEnabled,
                        onClick = { viewModel.toggleSound() },
                        label = { Text(strings.sound) },
                        leadingIcon = {
                            Icon(
                                if (viewModel.soundManager.isSoundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                contentDescription = null
                            )
                        }
                    )
                    FilterChip(
                        selected = viewModel.soundManager.isHapticsEnabled,
                        onClick = { viewModel.toggleHaptics() },
                        label = { Text(strings.vibration) },
                        leadingIcon = {
                            Icon(
                                if (viewModel.soundManager.isHapticsEnabled) Icons.Default.Vibration else Icons.Default.Smartphone,
                                contentDescription = null
                            )
                        }
                    )
                    FilterChip(
                        selected = true,
                        onClick = onToggleLanguage,
                        label = { Text("${currentLang.flag} ${currentLang.name}") }
                    )
                }

                // AdMob Bonus Coins Button
                Button(
                    onClick = onWatchAd,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8F00)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("pause_watch_ad_button")
                ) {
                    Text("🎁 Ver Anuncio AdMob (+100 🪙)", fontWeight = FontWeight.Black, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onResume,
                    colors = ButtonDefaults.buttonColors(containerColor = CandyPink),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("resume_button")
                ) {
                    Text(strings.resume, fontSize = 15.sp, fontWeight = FontWeight.Black)
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onRestart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("pause_restart_button")
                ) {
                    Text(strings.restartLevel, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onMap,
                    colors = ButtonDefaults.buttonColors(containerColor = CandyPurple),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("pause_map_button")
                ) {
                    Text(strings.backToMap, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
