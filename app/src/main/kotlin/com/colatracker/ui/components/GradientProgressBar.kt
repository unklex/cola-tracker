package com.colatracker.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.colatracker.ui.theme.ProgressGreen
import com.colatracker.ui.theme.ProgressGreenLight
import com.colatracker.ui.theme.ProgressOrange
import com.colatracker.ui.theme.ProgressRed
import com.colatracker.ui.theme.ProgressRedDark
import com.colatracker.ui.theme.ProgressYellow
import com.colatracker.ui.theme.ProgressYellowLight

/**
 * Градиентный прогресс бар с тиками и процентом
 *
 * @param progress Прогресс от 0f до 1f (0% - 100%)
 * @param showPercentage Показывать процент на баре
 * @param showTicks Показывать метки на 25%, 50%, 75%
 * @param height Высота прогресс бара
 * @param animated Анимировать изменение прогресса
 */
@Composable
fun GradientProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    showPercentage: Boolean = true,
    showTicks: Boolean = true,
    height: Dp = 16.dp,
    animated: Boolean = true
) {
    val clampedProgress = progress.coerceIn(0f, 1f)

    val animatedProgress by animateFloatAsState(
        targetValue = clampedProgress,
        animationSpec = tween(
            durationMillis = if (animated) 1000 else 0,
            easing = FastOutSlowInEasing
        ),
        label = "progress_animation"
    )

    val backgroundColor = MaterialTheme.colorScheme.surfaceVariant
    val tickColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)

    // Определяем цвета градиента на основе прогресса
    val gradientColors = remember(animatedProgress) {
        getProgressGradientColors(animatedProgress)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
        ) {
            val cornerRadius = size.height / 2

            // Фон
            drawRoundRect(
                color = backgroundColor,
                cornerRadius = CornerRadius(cornerRadius),
                size = size
            )

            // Заполненная часть с градиентом
            if (animatedProgress > 0f) {
                val progressWidth = size.width * animatedProgress
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = gradientColors,
                        startX = 0f,
                        endX = progressWidth
                    ),
                    cornerRadius = CornerRadius(cornerRadius),
                    size = Size(progressWidth, size.height)
                )
            }

            // Тики на 25%, 50%, 75%
            if (showTicks) {
                drawTicks(tickColor, cornerRadius)
            }
        }

        // Процент на баре
        if (showPercentage && height >= 16.dp) {
            val percentage = (animatedProgress * 100).toInt()
            val textColor = if (animatedProgress > 0.5f) {
                Color.White
            } else {
                MaterialTheme.colorScheme.onSurface
            }

            Text(
                text = "$percentage%",
                color = textColor,
                fontSize = (height.value * 0.55f).sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 4.dp)
            )
        }
    }
}

/**
 * Компактный градиентный прогресс бар без текста
 */
@Composable
fun CompactGradientProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp
) {
    GradientProgressBar(
        progress = progress,
        modifier = modifier,
        showPercentage = false,
        showTicks = false,
        height = height,
        animated = true
    )
}

/**
 * Получение цветов градиента на основе прогресса
 */
private fun getProgressGradientColors(progress: Float): List<Color> {
    return when {
        progress <= 0.5f -> listOf(ProgressGreenLight, ProgressGreen)
        progress <= 0.8f -> listOf(ProgressGreen, ProgressYellow, ProgressOrange)
        else -> listOf(ProgressYellow, ProgressOrange, ProgressRed, ProgressRedDark)
    }
}

/**
 * Рисование тиков на прогресс баре
 */
private fun DrawScope.drawTicks(tickColor: Color, cornerRadius: Float) {
    val tickPositions = listOf(0.25f, 0.5f, 0.75f)
    val tickWidth = 2.dp.toPx()
    val tickHeight = size.height * 0.4f
    val tickY = (size.height - tickHeight) / 2

    tickPositions.forEach { position ->
        val tickX = size.width * position - tickWidth / 2

        // Не рисуем тики близко к краям
        if (tickX > cornerRadius && tickX < size.width - cornerRadius) {
            drawRoundRect(
                color = tickColor,
                topLeft = Offset(tickX, tickY),
                size = Size(tickWidth, tickHeight),
                cornerRadius = CornerRadius(tickWidth / 2)
            )
        }
    }
}
