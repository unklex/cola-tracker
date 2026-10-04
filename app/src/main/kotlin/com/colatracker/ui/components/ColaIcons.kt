package com.colatracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Кастомные иконки для Cola Tracker
 */
object ColaIcons {

    /**
     * Иконка стакана с колой
     */
    @Composable
    fun GlassIcon(
        modifier: Modifier = Modifier,
        size: Dp = 24.dp,
        liquidColor: Color = MaterialTheme.colorScheme.primary,
        glassColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    ) {
        Canvas(modifier = modifier.size(size)) {
            drawGlass(liquidColor, glassColor)
        }
    }

    /**
     * Иконка бутылки колы
     */
    @Composable
    fun BottleIcon(
        modifier: Modifier = Modifier,
        size: Dp = 24.dp,
        liquidColor: Color = MaterialTheme.colorScheme.primary,
        bottleColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    ) {
        Canvas(modifier = modifier.size(size)) {
            drawBottle(liquidColor, bottleColor)
        }
    }

    /**
     * Иконка банки колы
     */
    @Composable
    fun CanIcon(
        modifier: Modifier = Modifier,
        size: Dp = 24.dp,
        primaryColor: Color = MaterialTheme.colorScheme.primary,
        secondaryColor: Color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Canvas(modifier = modifier.size(size)) {
            drawCan(primaryColor, secondaryColor)
        }
    }

    /**
     * Иконка капли
     */
    @Composable
    fun DropIcon(
        modifier: Modifier = Modifier,
        size: Dp = 24.dp,
        color: Color = MaterialTheme.colorScheme.primary
    ) {
        Canvas(modifier = modifier.size(size)) {
            drawDrop(color)
        }
    }

    /**
     * Логотип приложения - стилизованная бутылка с колой
     */
    @Composable
    fun AppLogo(
        modifier: Modifier = Modifier,
        size: Dp = 32.dp,
        primaryColor: Color = MaterialTheme.colorScheme.primary,
        accentColor: Color = Color.White
    ) {
        Canvas(modifier = modifier.size(size)) {
            drawAppLogo(primaryColor, accentColor)
        }
    }

    private fun DrawScope.drawGlass(liquidColor: Color, glassColor: Color) {
        val width = size.width
        val height = size.height

        // Стакан (трапеция)
        val glassPath = Path().apply {
            moveTo(width * 0.2f, height * 0.15f)
            lineTo(width * 0.8f, height * 0.15f)
            lineTo(width * 0.75f, height * 0.95f)
            lineTo(width * 0.25f, height * 0.95f)
            close()
        }

        // Жидкость (заполнение 70%)
        val liquidPath = Path().apply {
            moveTo(width * 0.235f, height * 0.35f)
            lineTo(width * 0.765f, height * 0.35f)
            lineTo(width * 0.75f, height * 0.95f)
            lineTo(width * 0.25f, height * 0.95f)
            close()
        }

        // Рисуем жидкость
        drawPath(
            path = liquidPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    liquidColor.copy(alpha = 0.9f),
                    liquidColor
                )
            )
        )

        // Рисуем контур стакана
        drawPath(
            path = glassPath,
            color = glassColor,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = width * 0.06f)
        )

        // Пузырьки
        drawCircle(
            color = Color.White.copy(alpha = 0.6f),
            radius = width * 0.04f,
            center = Offset(width * 0.4f, height * 0.6f)
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.5f),
            radius = width * 0.03f,
            center = Offset(width * 0.55f, height * 0.75f)
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.4f),
            radius = width * 0.025f,
            center = Offset(width * 0.45f, height * 0.85f)
        )
    }

    private fun DrawScope.drawBottle(liquidColor: Color, bottleColor: Color) {
        val width = size.width
        val height = size.height

        // Горлышко
        drawRoundRect(
            color = bottleColor,
            topLeft = Offset(width * 0.35f, height * 0.02f),
            size = Size(width * 0.3f, height * 0.15f),
            cornerRadius = CornerRadius(width * 0.05f)
        )

        // Основа бутылки
        val bottlePath = Path().apply {
            moveTo(width * 0.3f, height * 0.17f)
            lineTo(width * 0.7f, height * 0.17f)
            lineTo(width * 0.75f, height * 0.3f)
            lineTo(width * 0.75f, height * 0.95f)
            quadraticBezierTo(width * 0.75f, height, width * 0.7f, height)
            lineTo(width * 0.3f, height)
            quadraticBezierTo(width * 0.25f, height, width * 0.25f, height * 0.95f)
            lineTo(width * 0.25f, height * 0.3f)
            close()
        }

        // Жидкость внутри (80% заполнение)
        val liquidPath = Path().apply {
            moveTo(width * 0.28f, height * 0.35f)
            lineTo(width * 0.72f, height * 0.35f)
            lineTo(width * 0.72f, height * 0.92f)
            quadraticBezierTo(width * 0.72f, height * 0.97f, width * 0.67f, height * 0.97f)
            lineTo(width * 0.33f, height * 0.97f)
            quadraticBezierTo(width * 0.28f, height * 0.97f, width * 0.28f, height * 0.92f)
            close()
        }

        drawPath(
            path = liquidPath,
            brush = Brush.verticalGradient(
                colors = listOf(liquidColor.copy(alpha = 0.8f), liquidColor)
            )
        )

        drawPath(
            path = bottlePath,
            color = bottleColor,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = width * 0.05f)
        )

        // Этикетка
        drawRoundRect(
            color = Color.White.copy(alpha = 0.3f),
            topLeft = Offset(width * 0.3f, height * 0.5f),
            size = Size(width * 0.4f, height * 0.2f),
            cornerRadius = CornerRadius(width * 0.02f)
        )
    }

    private fun DrawScope.drawCan(primaryColor: Color, secondaryColor: Color) {
        val width = size.width
        val height = size.height

        // Основа банки
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(primaryColor, primaryColor.copy(alpha = 0.8f))
            ),
            topLeft = Offset(width * 0.15f, height * 0.1f),
            size = Size(width * 0.7f, height * 0.85f),
            cornerRadius = CornerRadius(width * 0.1f)
        )

        // Верх банки
        drawOval(
            color = secondaryColor,
            topLeft = Offset(width * 0.15f, height * 0.05f),
            size = Size(width * 0.7f, height * 0.15f)
        )

        // Низ банки
        drawOval(
            color = primaryColor.copy(alpha = 0.6f),
            topLeft = Offset(width * 0.15f, height * 0.85f),
            size = Size(width * 0.7f, height * 0.12f)
        )

        // Блик
        drawRoundRect(
            color = Color.White.copy(alpha = 0.3f),
            topLeft = Offset(width * 0.25f, height * 0.2f),
            size = Size(width * 0.15f, height * 0.5f),
            cornerRadius = CornerRadius(width * 0.05f)
        )

        // Язычок открытия
        drawOval(
            color = Color.Gray,
            topLeft = Offset(width * 0.35f, height * 0.08f),
            size = Size(width * 0.3f, height * 0.08f)
        )
    }

    private fun DrawScope.drawDrop(color: Color) {
        val width = size.width
        val height = size.height

        val dropPath = Path().apply {
            moveTo(width * 0.5f, height * 0.05f)
            quadraticBezierTo(width * 0.9f, height * 0.5f, width * 0.5f, height * 0.95f)
            quadraticBezierTo(width * 0.1f, height * 0.5f, width * 0.5f, height * 0.05f)
            close()
        }

        drawPath(
            path = dropPath,
            brush = Brush.verticalGradient(
                colors = listOf(color.copy(alpha = 0.7f), color)
            )
        )

        // Блик
        drawCircle(
            color = Color.White.copy(alpha = 0.5f),
            radius = width * 0.1f,
            center = Offset(width * 0.35f, height * 0.4f)
        )
    }

    private fun DrawScope.drawAppLogo(primaryColor: Color, accentColor: Color) {
        val width = size.width
        val height = size.height

        // Круглый фон
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primaryColor, primaryColor.copy(alpha = 0.8f))
            ),
            radius = width * 0.48f,
            center = Offset(width * 0.5f, height * 0.5f)
        )

        // Стилизованная буква C (Cola)
        val cPath = Path().apply {
            moveTo(width * 0.65f, height * 0.25f)
            quadraticBezierTo(width * 0.2f, height * 0.2f, width * 0.2f, height * 0.5f)
            quadraticBezierTo(width * 0.2f, height * 0.8f, width * 0.65f, height * 0.75f)
        }

        drawPath(
            path = cPath,
            color = accentColor,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = width * 0.12f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        )

        // Пузырьки
        drawCircle(
            color = accentColor.copy(alpha = 0.6f),
            radius = width * 0.05f,
            center = Offset(width * 0.7f, height * 0.35f)
        )
        drawCircle(
            color = accentColor.copy(alpha = 0.4f),
            radius = width * 0.035f,
            center = Offset(width * 0.75f, height * 0.5f)
        )
    }
}
