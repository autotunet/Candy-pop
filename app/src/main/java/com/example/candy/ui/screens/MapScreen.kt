package com.example.candy.ui.screens

import android.app.Activity
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.candy.ads.AdManager
import com.example.candy.data.LevelsData
import com.example.candy.data.local.LevelProgressEntity
import com.example.candy.data.local.PlayerStatsEntity
import com.example.candy.model.LevelDef
import com.example.candy.ui.GameScreen
import com.example.candy.ui.GameViewModel
import com.example.candy.util.AppLanguage
import com.example.candy.util.Localization
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun MapScreen(
    viewModel: GameViewModel,
    progressList: List<LevelProgressEntity>,
    playerStats: PlayerStatsEntity?,
    onStartLevel: (Int, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDark = uiState.isDarkMode
    val lang = uiState.language
    val strings = Localization.getStrings(lang)
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val activity = context as? Activity

    var selectedLevelForModal by remember { mutableStateOf<LevelDef?>(null) }
    var showWorldSelector by remember { mutableStateOf(false) }

    val totalStars = progressList.sumOf { it.stars }
    val coins = playerStats?.coins ?: 150
    val highestUnlocked = progressList.filter { it.isUnlocked }.maxOfOrNull { it.levelNumber } ?: 1
    val completedCount = progressList.count { it.stars > 0 }

    // Auto-scroll to the current level on first launch
    LaunchedEffect(highestUnlocked) {
        val targetIndex = (highestUnlocked - 1).coerceAtLeast(0)
        listState.scrollToItem(targetIndex)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("map_screen")
    ) {
        // Candy Kingdom Background Image
        Image(
            painter = painterResource(id = R.drawable.bg_candy_kingdom),
            contentDescription = "Candy Kingdom Map Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Tint overlay: reacts to Dark Mode
        val overlayBrush = if (isDark) {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xD90D0218),
                    Color(0xB32C0B40),
                    Color(0xE6120320)
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0x33000000),
                    Color(0x11FFFFFF),
                    Color(0x444A148C)
                )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(overlayBrush)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top App Bar / Header
            MapTopBar(
                totalStars = totalStars,
                coins = coins,
                currentLang = lang,
                isDark = isDark,
                onToggleDarkMode = { viewModel.toggleDarkMode() },
                onToggleLanguage = {
                    val nextLang = if (lang == AppLanguage.ES) AppLanguage.EN else AppLanguage.ES
                    viewModel.setLanguage(nextLang)
                },
                soundEnabled = viewModel.soundManager.isSoundEnabled,
                hapticsEnabled = viewModel.soundManager.isHapticsEnabled,
                onToggleSound = { viewModel.toggleSound() },
                onToggleHaptics = { viewModel.toggleHaptics() },
                onHelpClick = { viewModel.navigateTo(GameScreen.HOW_TO_PLAY) },
                onWatchAd = {
                    if (activity != null) {
                        AdManager.showAutomaticInterstitial(activity) {
                            viewModel.rewardForWatchingAd()
                        }
                    }
                },
                strings = strings
            )

            // Progress Tracker through 1000 levels
            MapProgressBanner(
                highestUnlocked = highestUnlocked,
                completedCount = completedCount,
                totalLevels = LevelsData.TOTAL_LEVELS,
                totalStars = totalStars,
                isDark = isDark,
                lang = lang,
                strings = strings,
                onOpenWorldSelector = { showWorldSelector = true },
                onJumpToCurrent = {
                    coroutineScope.launch {
                        val targetIndex = (highestUnlocked - 1).coerceAtLeast(0)
                        listState.animateScrollToItem(targetIndex)
                    }
                }
            )

            // 1,000 Winding Levels Map
            val levels = LevelsData.levels
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    reverseLayout = true,
                    contentPadding = PaddingValues(top = 28.dp, bottom = 88.dp, start = 20.dp, end = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("level_list")
                ) {
                    itemsIndexed(levels) { index, level ->
                        val progress = progressList.find { it.levelNumber == level.levelNumber }
                        val isUnlocked = progress?.isUnlocked ?: (level.levelNumber == 1)
                        val stars = progress?.stars ?: 0
                        val isCurrent = (level.levelNumber == highestUnlocked)

                        // Show World Divider every 20 levels
                        if (level.levelNumber % 20 == 1 && level.levelNumber > 1) {
                            val worldNum = LevelsData.getWorldNumber(level.levelNumber)
                            val worldTitle = LevelsData.getWorldName(worldNum, isSpanish = lang == AppLanguage.ES)
                            WorldDividerBanner(worldTitle = worldTitle, isDark = isDark)
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // Alternate horizontal positions to create a winding candy path
                        val alignment = when (index % 4) {
                            0 -> Alignment.Center
                            1 -> Alignment.CenterEnd
                            2 -> Alignment.Center
                            else -> Alignment.CenterStart
                        }

                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = alignment
                        ) {
                            LevelNode(
                                level = level,
                                isUnlocked = isUnlocked,
                                stars = stars,
                                isCurrent = isCurrent,
                                isDark = isDark,
                                isTimed = level.timeLimitSeconds != null,
                                strings = strings,
                                onClick = {
                                    if (isUnlocked) {
                                        selectedLevelForModal = level
                                    }
                                }
                            )
                        }

                        // Path Stepping Connector Dots
                        if (index < levels.size - 1) {
                            val nextIndex = index + 1
                            val currentAlign = index % 4
                            val nextAlign = nextIndex % 4
                            WindingPathConnector(
                                fromAlign = currentAlign,
                                toAlign = nextAlign,
                                isPassed = level.levelNumber < highestUnlocked,
                                isDark = isDark
                            )
                        }
                    }
                }

                // Quick Jump to current level FAB with pulsing badge
                FloatingActionButton(
                    onClick = {
                        coroutineScope.launch {
                            val targetIndex = (highestUnlocked - 1).coerceAtLeast(0)
                            listState.animateScrollToItem(targetIndex)
                        }
                    },
                    containerColor = CandyPink,
                    contentColor = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .testTag("jump_to_current_fab")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Jump to Level $highestUnlocked")
                        Text(
                            text = "$highestUnlocked",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // Level Launch Pre-Game Dialog
        selectedLevelForModal?.let { level ->
            val progress = progressList.find { it.levelNumber == level.levelNumber }
            val stars = progress?.stars ?: 0
            val highScore = progress?.highScore ?: 0

            LevelDetailModal(
                level = level,
                stars = stars,
                highScore = highScore,
                lang = lang,
                isDark = isDark,
                strings = strings,
                onDismiss = { selectedLevelForModal = null },
                onPlay = { isTimerRush ->
                    selectedLevelForModal = null
                    onStartLevel(level.levelNumber, isTimerRush)
                }
            )
        }

        // World / Chapter Selector Modal (Browse 1,000 Levels in 50 Worlds)
        if (showWorldSelector) {
            WorldSelectorModal(
                highestUnlocked = highestUnlocked,
                isDark = isDark,
                lang = lang,
                strings = strings,
                onDismiss = { showWorldSelector = false },
                onSelectWorld = { targetLevel ->
                    showWorldSelector = false
                    coroutineScope.launch {
                        val targetIndex = (targetLevel - 1).coerceIn(0, LevelsData.TOTAL_LEVELS - 1)
                        listState.animateScrollToItem(targetIndex)
                    }
                }
            )
        }
    }
}

/**
 * Header banner showing overall progress through 1000 levels with progress gauge and world quick jump.
 */
@Composable
private fun MapProgressBanner(
    highestUnlocked: Int,
    completedCount: Int,
    totalLevels: Int,
    totalStars: Int,
    isDark: Boolean,
    lang: AppLanguage,
    strings: com.example.candy.util.AppStrings,
    onOpenWorldSelector: () -> Unit,
    onJumpToCurrent: () -> Unit
) {
    val progressFraction = (highestUnlocked.toFloat() / totalLevels).coerceIn(0f, 1f)
    val currentWorld = LevelsData.getWorldNumber(highestUnlocked)
    val worldName = LevelsData.getWorldName(currentWorld, isSpanish = lang == AppLanguage.ES)

    Surface(
        color = if (isDark) Color(0xD9220836) else Color(0xD9FFFFFF),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0x22000000)),
        shadowElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .testTag("map_progress_banner")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Progress stats
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "🗺️ ${strings.progress}: $highestUnlocked / $totalLevels",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isDark) Color.White else CandyPurpleDark
                    )
                    Surface(
                        color = GoldStar.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "⭐ $totalStars / 3000",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) GoldStar else Color(0xFFF57F17),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Worlds Jump Button
                Surface(
                    color = CandyPink.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, CandyPink),
                    modifier = Modifier.clickable(onClick = onOpenWorldSelector)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = "🌍", fontSize = 11.sp)
                        Text(
                            text = strings.worlds,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = CandyPink
                        )
                    }
                }
            }

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0x33000000))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progressFraction)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF00E5FF),
                                    CandyYellow,
                                    CandyOrange,
                                    CandyPink
                                )
                            )
                        )
                )
            }
        }
    }
}

/**
 * Stepping dots / candy pebbles connecting the winding level nodes.
 */
@Composable
private fun WindingPathConnector(
    fromAlign: Int,
    toAlign: Int,
    isPassed: Boolean,
    isDark: Boolean
) {
    val dotColor = if (isPassed) {
        CandyYellow.copy(alpha = 0.85f)
    } else {
        if (isDark) Color(0x33FFFFFF) else Color(0x44FFFFFF)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0..2) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(if (isPassed) 8.dp else 6.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
    }
}

/**
 * Animated Character/Marker pin sitting on the player's active current level node.
 */
@Composable
private fun CurrentLevelMarker(
    isDark: Boolean,
    strings: com.example.candy.util.AppStrings
) {
    val infiniteTransition = rememberInfiniteTransition(label = "current_marker")
    val bobOffset by infiniteTransition.animateFloat(
        initialValue = -14f,
        targetValue = -3f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bob_offset"
    )

    val shadowScale by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shadow_scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .offset(y = bobOffset.dp)
            .testTag("current_level_animated_icon")
    ) {
        // Glowing "PLAY!" Speech Bubble
        Surface(
            color = CandyPink,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.5.dp, GoldStar),
            shadowElevation = 6.dp
        ) {
            Text(
                text = strings.currentLevelTag,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 0.8.sp,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }

        // Mascot Icon (Lollipop / Candy Hero Pin)
        Text(
            text = "🍭",
            fontSize = 26.sp,
            modifier = Modifier.padding(top = 1.dp)
        )

        // Drop shadow under mascot
        Box(
            modifier = Modifier
                .width(18.dp * shadowScale)
                .height(4.dp)
                .clip(CircleShape)
                .background(Color(0x55000000))
        )
    }
}

@Composable
private fun WorldDividerBanner(
    worldTitle: String,
    isDark: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = if (isDark) Color(0xDD3A1152) else Color(0xEEFFFFFF),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.5.dp, CandyOrange),
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "🏰", fontSize = 18.sp)
                Text(
                    text = worldTitle,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    color = if (isDark) Color.White else CandyPurpleDark
                )
            }
        }
    }
}

@Composable
private fun MapTopBar(
    totalStars: Int,
    coins: Int,
    currentLang: AppLanguage,
    isDark: Boolean,
    onToggleDarkMode: () -> Unit,
    onToggleLanguage: () -> Unit,
    soundEnabled: Boolean,
    hapticsEnabled: Boolean,
    onToggleSound: () -> Unit,
    onToggleHaptics: () -> Unit,
    onHelpClick: () -> Unit,
    onWatchAd: () -> Unit,
    strings: com.example.candy.util.AppStrings
) {
    Surface(
        color = if (isDark) Color(0xF21F0D24) else Color(0xF2FFFFFF),
        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
        shadowElevation = 8.dp,
        border = if (isDark) BorderStroke(1.dp, Color(0x33FFFFFF)) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Title & Stars Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    color = CandyPink,
                    shape = CircleShape,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = "🍬", fontSize = 18.sp)
                    }
                }

                Column {
                    Text(
                        text = strings.appName,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = if (isDark) Color.White else CandyPurpleDark
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "⭐ ", fontSize = 11.sp)
                        Text(
                            text = "$totalStars",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFFF57F17)
                        )
                    }
                }
            }

            // Controls: Dark mode, Language, Coins, AdMob button, Sound, Help
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // AdMob Rewarded Bonus Button
                Surface(
                    color = Color(0xFFFFB300),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .clickable(onClick = onWatchAd)
                        .testTag("map_watch_ad_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(text = "🎁", fontSize = 12.sp)
                        Text(text = "+100 🪙", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.Black)
                    }
                }

                // Dark Mode Toggle
                IconButton(
                    onClick = onToggleDarkMode,
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("dark_mode_button")
                ) {
                    Text(text = if (isDark) "☀️" else "🌙", fontSize = 16.sp)
                }

                // Language Switcher Button (ES / EN)
                Surface(
                    color = if (isDark) Color(0xFF3B154A) else Color(0xFFEDE7F6),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CandyPurple.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .clickable(onClick = onToggleLanguage)
                        .testTag("language_switch_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(text = currentLang.flag, fontSize = 12.sp)
                        Text(
                            text = currentLang.name,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            color = if (isDark) Color.White else CandyPurpleDark
                        )
                    }
                }

                // Coins badge
                Surface(
                    color = if (isDark) Color(0xFF3E2723) else Color(0xFFFFF9C4),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, GoldStar),
                    modifier = Modifier.padding(horizontal = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(text = "🪙", fontSize = 11.sp)
                        Text(
                            text = "$coins",
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            color = Color(0xFFF57F17)
                        )
                    }
                }

                // Help Button
                IconButton(
                    onClick = onHelpClick,
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("help_button")
                ) {
                    Text(text = "❓", fontSize = 15.sp)
                }
            }
        }
    }
}

/**
 * Level node on the map, marking completed levels with stars and current levels with an animated icon.
 */
@Composable
private fun LevelNode(
    level: LevelDef,
    isUnlocked: Boolean,
    stars: Int,
    isCurrent: Boolean,
    isDark: Boolean,
    isTimed: Boolean,
    strings: com.example.candy.util.AppStrings,
    onClick: () -> Unit
) {
    val isBoss = (level.levelNumber % 10 == 0)
    val isCompleted = (isUnlocked && stars > 0)

    val infiniteTransition = rememberInfiniteTransition(label = "current_level_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isCurrent) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Glowing aura expanding ring for the current level
    val auraScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "aura_ring"
    )
    val auraAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "aura_alpha"
    )

    // Node surface styling
    val nodeBrush = when {
        !isUnlocked -> Brush.verticalGradient(listOf(Color(0xFF78909C), Color(0xFF455A64)))
        isCompleted && stars == 3 -> Brush.verticalGradient(listOf(Color(0xFFFFD54F), Color(0xFFFF8F00)))
        isCompleted -> Brush.verticalGradient(listOf(CandyPink, CandyPinkDark))
        isBoss -> Brush.verticalGradient(listOf(Color(0xFFFF9100), Color(0xFFE65100)))
        isTimed -> Brush.verticalGradient(listOf(Color(0xFF00E5FF), Color(0xFF0288D1)))
        else -> Brush.verticalGradient(listOf(Color(0xFFFF4081), Color(0xFFC2185B)))
    }

    val borderColor = when {
        isCurrent -> GoldStar
        isCompleted && stars == 3 -> Color(0xFFFFF9C4)
        isCompleted -> Color.White
        isUnlocked -> Color.White.copy(alpha = 0.9f)
        else -> Color.LightGray.copy(alpha = 0.5f)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(enabled = isUnlocked, onClick = onClick)
            .testTag("level_node_${level.levelNumber}")
    ) {
        // Current Level Animated Icon sitting above the node
        if (isCurrent) {
            CurrentLevelMarker(isDark = isDark, strings = strings)
        }

        Box(contentAlignment = Alignment.Center) {
            // Expanding radiant ripple ring on current level
            if (isCurrent) {
                Box(
                    modifier = Modifier
                        .size(if (isBoss) 66.dp else 56.dp)
                        .scale(auraScale)
                        .clip(CircleShape)
                        .border(2.dp, GoldStar.copy(alpha = auraAlpha), CircleShape)
                )
            }

            // Main Level Node Button
            Surface(
                shape = CircleShape,
                color = Color.Transparent,
                border = BorderStroke(
                    width = if (isCurrent) 3.5.dp else if (isCompleted && stars == 3) 2.5.dp else 2.dp,
                    color = borderColor
                ),
                shadowElevation = if (isCurrent) 12.dp else if (isUnlocked) 8.dp else 2.dp,
                modifier = Modifier
                    .size(if (isBoss) 66.dp else 56.dp)
                    .scale(if (isCurrent) pulseScale else 1f)
                    .background(nodeBrush, CircleShape)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isUnlocked) {
                        Text(
                            text = "${level.levelNumber}",
                            fontWeight = FontWeight.Black,
                            fontSize = if (isBoss) 18.sp else 16.sp,
                            color = Color.White
                        )
                    } else {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = Color(0xFFCFD8DC),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // 3-Star Master Checkmark Badge at top right
            if (isCompleted && stars == 3) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFFFD54F),
                    border = BorderStroke(1.dp, Color.White),
                    modifier = Modifier
                        .size(18.dp)
                        .align(Alignment.TopEnd)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = "👑", fontSize = 10.sp)
                    }
                }
            }

            // Timed level badge on node
            if (isTimed && isUnlocked && !(isCompleted && stars == 3)) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF00E5FF),
                    border = BorderStroke(1.dp, Color.White),
                    modifier = Modifier
                        .size(18.dp)
                        .align(Alignment.TopEnd)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = "⏱️", fontSize = 9.sp)
                    }
                }
            }

            // Boss crown on top
            if (isBoss && isUnlocked && !isCurrent) {
                Text(
                    text = "👑",
                    fontSize = 18.sp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-14).dp)
                )
            }
        }

        // Marking completed levels with stars
        if (isUnlocked) {
            Surface(
                color = if (isCompleted) Color(0xDD000000) else Color(0x66000000),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(top = 3.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    for (i in 1..3) {
                        Text(
                            text = if (i <= stars) "⭐" else "☆",
                            fontSize = 11.sp,
                            color = if (i <= stars) GoldStar else Color.LightGray.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * World Selector Modal to quickly explore and jump across all 1,000 levels (50 worlds).
 */
@Composable
private fun WorldSelectorModal(
    highestUnlocked: Int,
    isDark: Boolean,
    lang: AppLanguage,
    strings: com.example.candy.util.AppStrings,
    onDismiss: () -> Unit,
    onSelectWorld: (Int) -> Unit
) {
    val totalWorlds = 50
    val currentWorld = LevelsData.getWorldNumber(highestUnlocked)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0xFF260E36) else Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .padding(12.dp)
                .testTag("world_selector_modal")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🌍 ${strings.worlds} (1 - $totalWorlds)",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isDark) Color.White else CandyPurpleDark
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = if (isDark) Color.White else Color.Black)
                    }
                }

                Text(
                    text = "1000 Niveles • 50 Mundos Temáticos",
                    fontSize = 12.sp,
                    color = if (isDark) Color.LightGray else Color.Gray,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(totalWorlds) { idx ->
                        val worldNum = idx + 1
                        val startLevel = (worldNum - 1) * 20 + 1
                        val endLevel = worldNum * 20
                        val isWorldUnlocked = (highestUnlocked >= startLevel)
                        val isCurrentWorld = (worldNum == currentWorld)
                        val worldName = LevelsData.getWorldName(worldNum, isSpanish = lang == AppLanguage.ES)

                        Surface(
                            color = if (isCurrentWorld) CandyPink.copy(alpha = 0.2f) else if (isWorldUnlocked) (if (isDark) Color(0xFF38154D) else Color(0xFFF3E5F5)) else Color(0x33888888),
                            shape = RoundedCornerShape(14.dp),
                            border = if (isCurrentWorld) BorderStroke(1.5.dp, CandyPink) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectWorld(startLevel) }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = if (isWorldUnlocked) "🏰" else "🔒",
                                        fontSize = 20.sp
                                    )
                                    Column {
                                        Text(
                                            text = worldName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (isWorldUnlocked) (if (isDark) Color.White else Color.Black) else Color.Gray
                                        )
                                        Text(
                                            text = "Niveles $startLevel - $endLevel",
                                            fontSize = 11.sp,
                                            color = if (isCurrentWorld) CandyPink else Color.Gray
                                        )
                                    }
                                }

                                if (isCurrentWorld) {
                                    Surface(
                                        color = CandyPink,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = strings.currentLevelTag,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LevelDetailModal(
    level: LevelDef,
    stars: Int,
    highScore: Int,
    lang: AppLanguage,
    isDark: Boolean,
    strings: com.example.candy.util.AppStrings,
    onDismiss: () -> Unit,
    onPlay: (Boolean) -> Unit
) {
    val levelName = if (level.levelNumber % 10 == 0) {
        if (lang == AppLanguage.ES) "👑 Ciudadela del Rey ${level.levelNumber}" else "👑 Boss Citadel ${level.levelNumber}"
    } else {
        if (lang == AppLanguage.ES) "Nivel ${level.levelNumber}" else "Level ${level.levelNumber}"
    }

    val levelQuote = when {
        level.levelNumber % 10 == 0 -> if (lang == AppLanguage.ES) "¡Una gran fortaleza de caramelo y gelatina!" else "A grand fortress of candy and thick jelly!"
        level.targetJellies > 0 -> if (lang == AppLanguage.ES) "¡Elimina todas las gelatinas brillantes!" else "Clear all shimmering jelly tiles!"
        level.targetIngredients.isNotEmpty() -> if (lang == AppLanguage.ES) "¡Recolecta los caramelos requeridos!" else "Harvest the required colorful candies!"
        level.targetSpecials.isNotEmpty() -> if (lang == AppLanguage.ES) "¡Crea caramelos especiales para ganar!" else "Craft explosive special treats!"
        else -> if (lang == AppLanguage.ES) "¡Combina dulces y haz cascadas épicas!" else "Match sweet candies and cascade your way to victory!"
    }

    val textColor = if (isDark) Color.White else CandyPurpleDark
    var isTimerRush by remember { mutableStateOf(level.timeLimitSeconds != null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0xFF271030) else Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("level_detail_modal")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Badge
                Surface(
                    color = if (level.levelNumber % 10 == 0) Color(0xFFFF6D00) else CandyPink,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Text(
                        text = if (lang == AppLanguage.ES) "NIVEL ${level.levelNumber}" else "LEVEL ${level.levelNumber}",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }

                Text(
                    text = levelName,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = textColor,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = levelQuote,
                    fontSize = 13.sp,
                    color = if (isDark) Color(0xFFB0BEC5) else Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                // Timed Level Info Banner
                if (level.timeLimitSeconds != null) {
                    Surface(
                        color = Color(0xFF00B0FF),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(bottom = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(text = "⏱️", fontSize = 13.sp)
                            Text(
                                text = "${strings.timedLevel} (${level.timeLimitSeconds}s)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }
                }

                // Stars display
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 14.dp)
                ) {
                    for (i in 1..3) {
                        Text(
                            text = if (i <= stars) "⭐" else "☆",
                            fontSize = 32.sp,
                            color = if (i <= stars) GoldStar else Color.LightGray
                        )
                    }
                }

                // Objectives Box
                Surface(
                    color = if (isDark) Color(0xFF3E1A49) else Color(0xFFFCE4EC),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = strings.targetObjectives,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = CandyPink
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🎯 ", fontSize = 14.sp)
                            Text(
                                text = "${strings.targetScore}: ${level.targetScore}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color.White else Color.Black
                            )
                        }
                        if (level.targetJellies > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "🍮 ", fontSize = 14.sp)
                                Text(
                                    text = "${strings.clearJellies}: ${level.targetJellies}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) Color.White else Color.Black
                                )
                            }
                        }
                        for ((color, target) in level.targetIngredients) {
                            val colorName = Localization.getCandyName(color, lang)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "🍬 ", fontSize = 14.sp)
                                Text(
                                    text = "${strings.collectCandies} $target $colorName",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) Color.White else Color.Black
                                )
                            }
                        }
                        for ((special, target) in level.targetSpecials) {
                            val specialName = Localization.getSpecialName(special, lang)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "✨ ", fontSize = 14.sp)
                                Text(
                                    text = "${strings.createSpecials} $target $specialName",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) Color.White else Color.Black
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🎮 ", fontSize = 14.sp)
                            Text(
                                text = "${level.maxMoves} ${strings.movesAllowed}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) CandyYellow else CandyPurpleDark
                            )
                        }
                    }
                }

                // Timer Mode Toggle Switch
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isTimerRush) "⏱️ Modo Temporizador (60s)" else "🎯 Modo Normal",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isTimerRush) Color(0xFF00E5FF) else (if (isDark) Color.White else Color.Black)
                    )
                    Switch(
                        checked = isTimerRush,
                        onCheckedChange = { isTimerRush = it }
                    )
                }

                if (highScore > 0) {
                    Text(
                        text = "${strings.highScore} $highScore",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF57F17),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("modal_cancel_button")
                    ) {
                        Text(text = strings.back, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { onPlay(isTimerRush) },
                        colors = ButtonDefaults.buttonColors(containerColor = CandyPink),
                        modifier = Modifier
                            .weight(1.4f)
                            .height(52.dp)
                            .testTag("modal_play_button")
                    ) {
                        Text(
                            text = strings.play,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    }
}
