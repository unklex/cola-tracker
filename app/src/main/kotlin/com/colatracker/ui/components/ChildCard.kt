package com.colatracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.colatracker.AppConfig
import com.colatracker.data.models.Child

/**
 * Карточка ребёнка для главного экрана
 */
@Composable
fun ChildCard(
    child: Child,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Шапка: аватар + имя
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Аватар
                ChildAvatar(
                    photoUrl = child.photoUrl,
                    name = child.name,
                    size = 56
                )
                
                // Имя
                Text(
                    text = child.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Прогресс бар
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Метка прогресса
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Выпито в этом месяце",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${child.consumedThisMonth} мл / ${child.monthlyLimit} мл",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                val targetProgress = (child.consumptionProgress / 100f).coerceIn(0f, 1f)
                val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
                    targetValue = targetProgress,
                    animationSpec = androidx.compose.animation.core.tween(durationMillis = 1000, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                    label = "ProgressAnimation"
                )
                
                val targetColor = getProgressColor(child.consumptionProgress)
                val animatedColor by androidx.compose.animation.animateColorAsState(
                    targetValue = targetColor,
                    animationSpec = androidx.compose.animation.core.tween(durationMillis = 500),
                    label = "ColorAnimation"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedProgress)
                            .clip(RoundedCornerShape(4.dp))
                            .background(animatedColor)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Остаток
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Осталось",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${child.remaining} мл",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = getRemainingColor(child.remaining)
                    )
                }
                
                // Индикатор - нажми для деталей
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Подробнее",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

/**
 * Аватар ребёнка с загрузкой фото или placeholder
 */
@Composable
fun ChildAvatar(
    photoUrl: String?,
    name: String,
    size: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        if (photoUrl != null) {
            val fullUrl = "${AppConfig.BASE_URL}/$photoUrl"
            android.util.Log.d("ChildAvatar", "Loading image from: $fullUrl")
            
            AsyncImage(
                model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                    .data(fullUrl)
                    .crossfade(true)
                    .listener(
                        onError = { _, result ->
                            android.util.Log.e("ChildAvatar", "Failed to load image: ${result.throwable.message}", result.throwable)
                        },
                        onSuccess = { _, _ ->
                            android.util.Log.d("ChildAvatar", "Successfully loaded image from: $fullUrl")
                        }
                    )
                    .build(),
                contentDescription = name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                error = androidx.compose.ui.graphics.painter.ColorPainter(MaterialTheme.colorScheme.errorContainer)
            )
        } else {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size((size * 0.57).toInt().dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

/**
 * Цвет прогресс бара в зависимости от процента
 */
@Composable
private fun getProgressColor(progress: Float): Color {
    return when {
        progress < 50 -> MaterialTheme.colorScheme.secondary
        progress < 80 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }
}

/**
 * Цвет остатка в зависимости от количества
 */
@Composable
private fun getRemainingColor(remaining: Int): Color {
    return when {
        remaining > 500 -> MaterialTheme.colorScheme.secondary
        remaining > 200 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }
}
