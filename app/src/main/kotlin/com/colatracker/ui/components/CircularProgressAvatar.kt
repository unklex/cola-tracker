package com.colatracker.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest

/**
 * Аватар с круговым индикатором расхода за текущий месяц.
 *
 * @param progress доля израсходованного месячного начисления (0f..1f)
 * @param photoVersion версия фото для сброса кэша Coil (см. ChildDetailViewModel)
 */
@Composable
fun CircularProgressAvatar(
    photoUrl: String?,
    name: String,
    progress: Float,
    size: Dp = 100.dp,
    strokeWidth: Dp = 6.dp,
    isUploading: Boolean = false,
    photoVersion: Long = 0L,
    onAvatarClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 1000),
        label = "progress"
    )

    val progressColor = monthUsageColor(progress)
    val backgroundColor = MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Круговой прогресс
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(
                width = strokeWidth.toPx(),
                cap = StrokeCap.Round
            )

            // Фон кольца
            drawArc(
                color = backgroundColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = stroke
            )

            // Прогресс
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        progressColor.copy(alpha = 0.8f),
                        progressColor
                    )
                ),
                startAngle = -90f,
                sweepAngle = animatedProgress * 360f,
                useCenter = false,
                style = stroke
            )
        }

        // Аватар внутри
        val avatarSize = size - strokeWidth * 2 - 8.dp
        Box(
            modifier = Modifier
                .size(avatarSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .then(
                    if (onAvatarClick != null && !isUploading) {
                        Modifier.clickable { onAvatarClick() }
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            val url = childPhotoUrl(photoUrl, photoVersion)
            if (url != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(url)
                        .crossfade(true)
                        .build(),
                    contentDescription = name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(avatarSize * 0.5f),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // Оверлей для редактирования
            if (onAvatarClick != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Изменить фото",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Анимированный счётчик объёма с подписью.
 */
@Composable
fun AnimatedCounter(
    targetValue: Int,
    label: String,
    color: Color = MaterialTheme.colorScheme.primary,
    caption: String? = null,
    modifier: Modifier = Modifier
) {
    val animatedValue by animateIntAsState(
        targetValue = targetValue,
        animationSpec = tween(durationMillis = 800),
        label = "counter"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "$animatedValue мл",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        if (caption != null) {
            Text(
                text = caption,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
