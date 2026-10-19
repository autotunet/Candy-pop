package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val CandyLightColorScheme = lightColorScheme(
    primary = PrimaryColor,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD8E4),
    onPrimaryContainer = Color(0xFF3B071E),
    secondary = SecondaryColor,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF3E5F5),
    onSecondaryContainer = Color(0xFF2E004E),
    tertiary = TertiaryColor,
    onTertiary = Color.White,
    background = BackgroundColor,
    onBackground = Color(0xFF2C1B24),
    surface = SurfaceColor,
    onSurface = Color(0xFF2C1B24)
)

private val CandyDarkColorScheme = darkColorScheme(
    primary = CandyPink,
    onPrimary = Color.White,
    primaryContainer = CandyPurpleDark,
    onPrimaryContainer = Color.White,
    secondary = CandyPurple,
    onSecondary = Color.White,
    tertiary = CandyYellow,
    onTertiary = Color.Black,
    background = Color(0xFF1F0D24),
    onBackground = Color(0xFFFFF0F5),
    surface = Color(0xFF2C1534),
    onSurface = Color(0xFFFFF0F5)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep vibrant custom candy branding
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) CandyDarkColorScheme else CandyLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
