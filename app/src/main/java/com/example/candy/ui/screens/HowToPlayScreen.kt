package com.example.candy.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.candy.ui.GameViewModel
import com.example.candy.util.Localization
import com.example.ui.theme.*

@Composable
fun HowToPlayScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val strings = Localization.getStrings(uiState.language)

    BackHandler { onBack() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF880E4F),
                        Color(0xFF4A148C),
                        Color(0xFF2C1534)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("how_to_play_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color(0x44FFFFFF), CircleShape)
                        .testTag("back_from_help_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = strings.back,
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = strings.howToPlay,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }

            // Guide Content
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    TutorialCard(
                        icon = "🍬",
                        title = strings.tutBasicTitle,
                        badge = strings.tutBasicBadge,
                        badgeColor = CandyPink,
                        description = strings.tutBasicDesc
                    )
                }

                item {
                    TutorialCard(
                        icon = "🍭",
                        title = strings.tutStripedTitle,
                        badge = strings.tutStripedBadge,
                        badgeColor = CandyOrange,
                        description = strings.tutStripedDesc
                    )
                }

                item {
                    TutorialCard(
                        icon = "🍬",
                        title = strings.tutWrappedTitle,
                        badge = strings.tutWrappedBadge,
                        badgeColor = CandyPurple,
                        description = strings.tutWrappedDesc
                    )
                }

                item {
                    TutorialCard(
                        icon = "💣",
                        title = strings.tutBombTitle,
                        badge = strings.tutBombBadge,
                        badgeColor = CandyYellow,
                        description = strings.tutBombDesc
                    )
                }

                item {
                    TutorialCard(
                        icon = "✨",
                        title = strings.tutCombosTitle,
                        badge = strings.tutCombosBadge,
                        badgeColor = Color(0xFF00E676),
                        description = strings.tutCombosDesc
                    )
                }

                item {
                    TutorialCard(
                        icon = "🔨",
                        title = strings.tutBoostersTitle,
                        badge = strings.tutBoostersBadge,
                        badgeColor = CandyBlue,
                        description = strings.tutBoostersDesc
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
        }
    }
}

@Composable
private fun TutorialCard(
    icon: String,
    title: String,
    badge: String,
    badgeColor: Color,
    description: String
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                color = Color(0xFFFCE4EC),
                shape = CircleShape,
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = icon, fontSize = 26.sp)
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = CandyPurpleDark
                    )

                    Surface(
                        color = badgeColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = badge,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = Color(0xFF555555),
                    lineHeight = 18.sp
                )
            }
        }
    }
}
