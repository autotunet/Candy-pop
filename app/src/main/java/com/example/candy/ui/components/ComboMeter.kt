package com.example.candy.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.candy.ui.GameUiState
import com.example.candy.util.AppStrings
import com.example.ui.theme.CandyOrange
import com.example.ui.theme.CandyPink
import com.example.ui.theme.CandyYellow
import com.example.ui.theme.GoldStar
import java.util.Locale

/**
 * Visual Combo Meter UI element that tracks and displays consecutive matches,
 * displaying streak progress, tier praises, and active score multipliers.
 */
@Composable
fun ComboMeter(
    uiState: GameUiState,
    strings: AppStrings,
    modifier: Modifier = Modifier
) {
    val streak = uiState.comboStreak
    val multiplier = uiState.comboMultiplier
    val fill = uiState.comboMeterFill
    val isActive = uiState.isComboActive && streak > 0

    // Dynamic glowing border and accent based on combo streak tier
    val targetBorderColor = when {
        !isActive -> Color(0x33FFFFFF)
        streak <= 1 -> Color(0xFF00E5FF) // Electric Cyan
        streak in 2..3 -> CandyYellow    // Sweet Gold
        streak in 4..5 -> CandyOrange    // Fiery Orange
        else -> CandyPink                // Neon Magenta / Ultra
    }

    val animatedBorderColor by animateColorAsState(
        targetValue = targetBorderColor,
        animationSpec = tween(250),
        label = "combo_border_color"
    )

    // Pulse animation for active multipliers
    val infiniteTransition = rememberInfiniteTransition(label = "combo_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive && multiplier > 1.0f) 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // Animated bar fill fraction
    val animatedFill by animateFloatAsState(
        targetValue = if (isActive) fill.coerceIn(0.05f, 1f) else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "combo_meter_fill"
    )

    // Scale bounce on streak increment
    val streakScale by animateFloatAsState(
        targetValue = if (isActive) 1.15f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "streak_scale"
    )

    Surface(
        color = Color(0xDD1B092B),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(2.dp, animatedBorderColor),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .testTag("visual_combo_meter")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Left: Animated Flame / Icon & Streak Info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.scale(streakScale)
            ) {
                val iconText = when {
                    !isActive -> "⚡"
                    streak <= 2 -> "🔥"
                    streak <= 4 -> "💥"
                    else -> "👑"
                }

                Text(
                    text = iconText,
                    fontSize = 20.sp
                )

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (isActive) "${strings.streak} $streak" else strings.streak,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isActive) animatedBorderColor else Color.White.copy(alpha = 0.7f),
                            letterSpacing = 0.5.sp
                        )
                    }

                    // Praise tag or subtle hint
                    val subLabel = when {
                        !isActive -> strings.combo
                        streak == 2 -> strings.sweet
                        streak == 3 -> strings.tasty
                        streak in 4..5 -> strings.delicious
                        else -> strings.sugarCrush
                    }

                    Text(
                        text = subLabel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) Color.White else Color.White.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Center: Glowing Meter Gauge Track
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(Color(0x66000000)),
                contentAlignment = Alignment.CenterStart
            ) {
                if (isActive && animatedFill > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedFill)
                            .clip(RoundedCornerShape(7.dp))
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

                // 5 Milestone markers on the gauge
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 1..4) {
                        Box(
                            modifier = Modifier
                                .size(2.dp, 8.dp)
                                .background(Color.White.copy(alpha = 0.35f))
                        )
                    }
                }
            }

            // Right: Multiplier Badge
            Surface(
                color = if (isActive && multiplier > 1.0f) animatedBorderColor.copy(alpha = 0.25f) else Color(0x33FFFFFF),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    1.5.dp,
                    if (isActive && multiplier > 1.0f) animatedBorderColor else Color(0x44FFFFFF)
                ),
                modifier = Modifier
                    .scale(pulseScale)
                    .testTag("combo_multiplier_badge")
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val multString = String.format(Locale.US, "x%.1f", multiplier)
                    Text(
                        text = multString,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isActive && multiplier > 1.0f) GoldStar else Color.White
                    )
                    Text(
                        text = strings.multiplier,
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.8f),
                        letterSpacing = 0.4.sp
                    )
                }
            }
        }
    }
}
