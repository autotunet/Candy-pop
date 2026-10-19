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
    val totalStars = progressList.sumOf { it.stars }
    val coins = playerStats?.coins ?: 150
    val highestUnlocked = progressList.filter { it.isUnlocked }.maxOfOrNull { it.levelNumber } ?: 1

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

            // 1,000 Winding Levels Map
            val levels = LevelsData.levels
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    reverseLayout = true,
                    contentPadding = PaddingValues(top = 24.dp, bottom = 80.dp, start = 20.dp, end = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("level_list")
                ) {
                    itemsIndexed(levels) { index, level ->
                        val progress = progressList.find { it.levelNumber == level.levelNumber }
                        val isUnlocked = progress?.isUnlocked ?: (level.levelNumber == 1)
                        val stars = progress?.stars ?: 0

                        // Show World Divider every 20 levels
                        if (level.levelNumber % 20 == 1 && level.levelNumber > 1) {
                            val worldNum = LevelsData.getWorldNumber(level.levelNumber)
                            val worldTitle = LevelsData.getWorldName(worldNum, isSpanish = lang == AppLanguage.ES)
                            WorldDividerBanner(worldTitle = worldTitle, isDark = isDark)
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // Alternate positions horizontally to create a winding candy path
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
                                isCurrent = level.levelNumber == highestUnlocked,
                                isDark = isDark,
                                isTimed = level.timeLimitSeconds != null,
                                onClick = {
                                    if (isUnlocked) {
                                        selectedLevelForModal = level
                                    }
                                }
                            )
                        }
                    }
                }

                // Quick Jump to current level FAB
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
                    Icon(Icons.Default.PlayArrow, contentDescription = "Jump to Level $highestUnlocked")
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

@Composable
private fun LevelNode(
    level: LevelDef,
    isUnlocked: Boolean,
    stars: Int,
    isCurrent: Boolean,
    isDark: Boolean,
    isTimed: Boolean,
    onClick: () -> Unit
) {
    val isBoss = (level.levelNumber % 10 == 0)
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

    val nodeColor = when {
        !isUnlocked -> Color.Gray
        isBoss -> Color(0xFFFF9100)
        isTimed -> Color(0xFF00B0FF)
        else -> CandyPink
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .scale(if (isCurrent) pulseScale else 1f)
            .clickable(enabled = isUnlocked, onClick = onClick)
            .testTag("level_node_${level.levelNumber}")
    ) {
        Box(contentAlignment = Alignment.Center) {
            Surface(
                shape = CircleShape,
                color = nodeColor,
                border = BorderStroke(
                    width = if (isCurrent) 3.5.dp else 2.dp,
                    color = if (isCurrent) Color(0xFFFFEB3B) else Color.White
                ),
                shadowElevation = if (isUnlocked) 8.dp else 2.dp,
                modifier = Modifier.size(if (isBoss) 64.dp else 54.dp)
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
                            tint = Color(0xFFB0BEC5),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Timed level badge on node
            if (isTimed && isUnlocked) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF00E5FF),
                    border = BorderStroke(1.dp, Color.White),
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.TopEnd)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = "⏱️", fontSize = 10.sp)
                    }
                }
            }

            // Boss crown
            if (isBoss && isUnlocked) {
                Text(
                    text = "👑",
                    fontSize = 18.sp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-14).dp)
                )
            }
        }

        // Stars earned under node
        if (isUnlocked) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(1.dp),
                modifier = Modifier.padding(top = 2.dp)
            ) {
                for (i in 1..3) {
                    Text(
                        text = if (i <= stars) "⭐" else "☆",
                        fontSize = 10.sp,
                        color = if (i <= stars) GoldStar else Color.LightGray
                    )
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
                            fontSize = 32.sp
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
