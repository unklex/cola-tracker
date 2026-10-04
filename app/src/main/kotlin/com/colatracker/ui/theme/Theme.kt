package com.colatracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// === Effervescent Archive Palette ===

// Primary Core
val ColaPrimary = Color(0xFFBA0012)
val ColaPrimaryDim = Color(0xFFA4000F)
val ColaPrimaryContainer = Color(0xFFFF766A)
val ColaOnPrimary = Color(0xFFFFEFED)
val ColaOnPrimaryContainer = Color(0xFF4F0003)

// Secondary
val ColaSecondary = Color(0xFF9F364B)
val ColaSecondaryContainer = Color(0xFFFFC2C9)
val ColaOnSecondary = Color(0xFFFFEFEF)
val ColaOnSecondaryContainer = Color(0xFF852138)

// Tertiary (purple accent)
val ColaTertiary = Color(0xFF7842A5)
val ColaTertiaryContainer = Color(0xFFD199FF)
val ColaOnTertiary = Color(0xFFFCEEFF)
val ColaOnTertiaryContainer = Color(0xFF490C77)

// Surface (light)
val ColaSurface = Color(0xFFFAF5F5)
val ColaSurfaceVariant = Color(0xFFE0DCDC)
val ColaOnSurface = Color(0xFF302E2F)
val ColaOnSurfaceVariant = Color(0xFF5D5B5B)

// Outline
val ColaOutline = Color(0xFF797676)
val ColaOutlineVariant = Color(0xFFB0ACAC)

// Error
val ColaError = Color(0xFFB41340)
val ColaErrorContainer = Color(0xFFF74B6D)
val ColaOnError = Color(0xFFFFEFEF)
val ColaOnErrorContainer = Color(0xFF3F0014)

// Inverse
val ColaInverseSurface = Color(0xFF0F0E0E)
val ColaInverseOnSurface = Color(0xFF9F9C9C)
val ColaInversePrimary = Color(0xFFFF544A)

// Legacy aliases (used by the detail screen and shared components)
val ColaRed = ColaPrimary
val ColaRedLight = Color(0xFFE63946)
val ColaRedDark = ColaPrimaryDim

// Progress Colors
val ProgressGreen = Color(0xFF4CAF50)
val ProgressGreenLight = Color(0xFF81C784)
val ProgressYellow = Color(0xFFFFC107)
val ProgressYellowLight = Color(0xFFFFD54F)
val ProgressOrange = Color(0xFFFF9800)
val ProgressRed = Color(0xFFE53935)
val ProgressRedDark = Color(0xFFC62828)

// Dark surfaces
private val ColaSurfaceDarkBase = Color(0xFF171A1D)
private val ColaOnSurfaceDark = Color(0xFFE2E2E5)
private val ColaOnSurfaceVariantDark = Color(0xFFB8B4B5)

/**
 * Уровни surface-контейнеров.
 *
 * В Material3 1.1.x (compose-bom 2024.01.00) в `ColorScheme` ещё нет ролей
 * `surfaceContainer*`, поэтому иерархия контейнеров живёт здесь и корректно
 * переключается вместе с темой.
 */
data class ColaContainers(
    val surfaceContainerLowest: Color,
    val surfaceContainerLow: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,
    val surfaceBright: Color
)

val LightContainers = ColaContainers(
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF5EFF0),
    surfaceContainer = Color(0xFFECE7E7),
    surfaceContainerHigh = Color(0xFFE6E1E1),
    surfaceContainerHighest = Color(0xFFE0DCDC),
    surfaceBright = Color(0xFFFAF5F5)
)

val DarkContainers = ColaContainers(
    surfaceContainerLowest = Color(0xFF101215),
    surfaceContainerLow = Color(0xFF1C1F23),
    surfaceContainer = Color(0xFF222629),
    surfaceContainerHigh = Color(0xFF2C3033),
    surfaceContainerHighest = Color(0xFF373B3F),
    surfaceBright = Color(0xFF2C3033)
)

// Extended colors for gradients
data class ColaGradients(
    val primaryGradient: Brush,
    val cardGradient: Brush,
    val progressGradient: Brush,
    val successGradient: Brush,
    val warningGradient: Brush,
    val dangerGradient: Brush,
    val colaCtaGradient: Brush
)

val LightGradients = ColaGradients(
    primaryGradient = Brush.verticalGradient(
        colors = listOf(ColaPrimary, ColaPrimaryDim)
    ),
    cardGradient = Brush.verticalGradient(
        colors = listOf(Color.White, Color(0xFFFAFAFA))
    ),
    progressGradient = Brush.horizontalGradient(
        colors = listOf(ProgressGreen, ProgressYellow, ProgressOrange, ProgressRed)
    ),
    successGradient = Brush.horizontalGradient(
        colors = listOf(ProgressGreenLight, ProgressGreen)
    ),
    warningGradient = Brush.horizontalGradient(
        colors = listOf(ProgressYellowLight, ProgressYellow)
    ),
    dangerGradient = Brush.horizontalGradient(
        colors = listOf(ProgressRed, ProgressRedDark)
    ),
    colaCtaGradient = Brush.linearGradient(
        colors = listOf(ColaPrimary, ColaPrimaryDim),
        // 145 degrees approximation
        start = androidx.compose.ui.geometry.Offset(0f, 0f),
        end = androidx.compose.ui.geometry.Offset(100f, 200f)
    )
)

val DarkGradients = ColaGradients(
    primaryGradient = Brush.verticalGradient(
        colors = listOf(ColaRedLight, ColaPrimary)
    ),
    cardGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF2D2D2D), Color(0xFF252525))
    ),
    progressGradient = Brush.horizontalGradient(
        colors = listOf(ProgressGreen, ProgressYellow, ProgressOrange, ProgressRed)
    ),
    successGradient = Brush.horizontalGradient(
        colors = listOf(ProgressGreenLight, ProgressGreen)
    ),
    warningGradient = Brush.horizontalGradient(
        colors = listOf(ProgressYellowLight, ProgressYellow)
    ),
    dangerGradient = Brush.horizontalGradient(
        colors = listOf(ProgressRed, ProgressRedDark)
    ),
    colaCtaGradient = Brush.linearGradient(
        colors = listOf(ColaInversePrimary, ColaPrimary),
        start = androidx.compose.ui.geometry.Offset(0f, 0f),
        end = androidx.compose.ui.geometry.Offset(100f, 200f)
    )
)

val LocalColaGradients = staticCompositionLocalOf { LightGradients }
val LocalColaContainers = staticCompositionLocalOf { LightContainers }

// Светлая тема — Effervescent Archive palette
private val LightColorScheme = lightColorScheme(
    primary = ColaPrimary,
    onPrimary = ColaOnPrimary,
    primaryContainer = ColaPrimaryContainer,
    onPrimaryContainer = ColaOnPrimaryContainer,

    secondary = ColaSecondary,
    onSecondary = ColaOnSecondary,
    secondaryContainer = ColaSecondaryContainer,
    onSecondaryContainer = ColaOnSecondaryContainer,

    tertiary = ColaTertiary,
    onTertiary = ColaOnTertiary,
    tertiaryContainer = ColaTertiaryContainer,
    onTertiaryContainer = ColaOnTertiaryContainer,

    background = ColaSurface,
    onBackground = ColaOnSurface,
    surface = ColaSurface,
    onSurface = ColaOnSurface,
    surfaceVariant = ColaSurfaceVariant,
    onSurfaceVariant = ColaOnSurfaceVariant,

    outline = ColaOutline,
    outlineVariant = ColaOutlineVariant,

    error = ColaError,
    onError = ColaOnError,
    errorContainer = ColaErrorContainer,
    onErrorContainer = ColaOnErrorContainer,

    inverseSurface = ColaInverseSurface,
    inverseOnSurface = ColaInverseOnSurface,
    inversePrimary = ColaInversePrimary,

    surfaceTint = ColaPrimary
)

// Тёмная тема — та же палитра, пересчитанная под тёмный фон
private val DarkColorScheme = darkColorScheme(
    primary = ColaInversePrimary,
    onPrimary = Color(0xFF3A0004),
    primaryContainer = Color(0xFF8C000D),
    onPrimaryContainer = Color(0xFFFFDAD6),

    secondary = Color(0xFFFFB1BC),
    onSecondary = Color(0xFF5F1123),
    secondaryContainer = Color(0xFF7E2438),
    onSecondaryContainer = Color(0xFFFFD9DE),

    tertiary = Color(0xFFC48BF4),
    onTertiary = Color(0xFF3D1263),
    tertiaryContainer = Color(0xFF5B2B8A),
    onTertiaryContainer = Color(0xFFEEDCFF),

    background = ColaSurfaceDarkBase,
    onBackground = ColaOnSurfaceDark,
    surface = ColaSurfaceDarkBase,
    onSurface = ColaOnSurfaceDark,
    surfaceVariant = Color(0xFF3C3C3C),
    onSurfaceVariant = ColaOnSurfaceVariantDark,

    outline = Color(0xFF8C8888),
    outlineVariant = Color(0xFF4A4747),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    inverseSurface = ColaOnSurfaceDark,
    inverseOnSurface = Color(0xFF2E3133),
    inversePrimary = ColaPrimary,

    surfaceTint = ColaInversePrimary
)

@Composable
fun ColaTrackerTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val gradients = if (darkTheme) DarkGradients else LightGradients
    val containers = if (darkTheme) DarkContainers else LightContainers

    CompositionLocalProvider(
        LocalColaGradients provides gradients,
        LocalColaContainers provides containers
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

// Extension для удобного доступа к градиентам и контейнерам
object ColaTheme {
    val gradients: ColaGradients
        @Composable
        get() = LocalColaGradients.current

    val containers: ColaContainers
        @Composable
        get() = LocalColaContainers.current
}
