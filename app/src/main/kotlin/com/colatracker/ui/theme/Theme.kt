package com.colatracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Цветовая палитра Cola Tracker
private val ColaRed = Color(0xFFE63946)
private val ColaRedDark = Color(0xFFC1121F)
private val ColaBackground = Color(0xFFF8F9FA)
private val ColaBackgroundDark = Color(0xFF212529)
private val ColaCardBackground = Color(0xFFFFFFFF)
private val ColaCardBackgroundDark = Color(0xFF2B2F33)

// Accent Colors
private val ColaYellow = Color(0xFFFFC107)
private val ColaYellowDark = Color(0xFFFFA000)

// Светлая тема
private val LightColorScheme = lightColorScheme(
    primary = ColaRed,
    onPrimary = Color.White,
    primaryContainer = ColaRed.copy(alpha = 0.1f),
    onPrimaryContainer = ColaRedDark,

    secondary = Color(0xFF457B9D),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCDE5F5),
    onSecondaryContainer = Color(0xFF001D35),

    tertiary = Color(0xFFF4A261),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFF0E6),
    onTertiaryContainer = Color(0xFF5C3D2E),

    background = ColaBackground,
    onBackground = Color(0xFF1A1C1E),
    surface = ColaCardBackground,
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFF0F0F0),
    onSurfaceVariant = Color(0xFF49454F),

    error = Color(0xFFBA1A1A),
    onError = Color.White,
)

// Тёмная тема
private val DarkColorScheme = darkColorScheme(
    primary = ColaRed,
    onPrimary = Color.White,
    primaryContainer = ColaRedDark,
    onPrimaryContainer = Color(0xFFFFDAD6),

    secondary = Color(0xFF9BCBEB),
    onSecondary = Color(0xFF003549),
    secondaryContainer = Color(0xFF004D68),
    onSecondaryContainer = Color(0xFFCDE5F5),

    tertiary = Color(0xFFF4A261),
    onTertiary = Color(0xFF3D2E1F),
    tertiaryContainer = Color(0xFF5C3D2E),
    onTertiaryContainer = Color(0xFFFFF0E6),

    background = ColaBackgroundDark,
    onBackground = Color(0xFFE2E2E5),
    surface = ColaCardBackgroundDark,
    onSurface = Color(0xFFE2E2E5),
    surfaceVariant = Color(0xFF3C3C3C),
    onSurfaceVariant = Color(0xFFCAC4D0),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

@Composable
fun ColaTrackerTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
