package com.colatracker.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.colatracker.AppConfig
import com.colatracker.data.models.Child
import com.colatracker.ui.theme.ColaRed
import com.colatracker.ui.theme.ColaRedLight
import com.colatracker.ui.theme.ProgressGreen
import com.colatracker.ui.theme.ProgressRed
import com.colatracker.ui.theme.ProgressYellow

/**
 * Улучшенная карточка ребёнка для главного экрана v2.0
 */
@Composable
fun ChildCard(
    child: Child,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Анимация масштаба при нажатии
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "scale"
    )

    // Анимация тени при нажатии
    val elevation by animateFloatAsState(
        targetValue = if (isPressed) 1f else 4f,
        animationSpec = tween(durationMillis = 100),
        label = "elevation"
    )

    // Предупреждение — по расходу месяца (≥ 80% начисления), а не по балансу
    val showWarningBadge = child.monthUsageRatio >= 0.8f
    val progress = child.monthUsageProgress

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .shadow(
                elevation = elevation.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = ColaRed.copy(alpha = 0.25f)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                        )
                    )
                )
        ) {
            // Декоративный градиент сверху
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                ColaRed,
                                ColaRedLight,
                                ColaRed
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .padding(16.dp)
            ) {
                // Шапка: аватар + имя + badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
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
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Warning Badge
                    if (showWarningBadge) {
                        WarningBadge()
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Прогресс бар
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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

                    // Градиентный прогресс бар
                    CompactGradientProgressBar(
                        progress = progress,
                        height = 10.dp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Остаток
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Иконка стакана
                        ColaIcons.GlassIcon(
                            size = 28.dp,
                            liquidColor = getRemainingColor(child.remaining, child.monthlyLimit)
                        )

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
                                color = getRemainingColor(child.remaining, child.monthlyLimit)
                            )
                        }
                    }

                    // Индикатор - нажми для деталей
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Подробнее",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Бейдж предупреждения о низком остатке
 */
@Composable
private fun WarningBadge() {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = ProgressRed.copy(alpha = 0.15f),
        modifier = Modifier.padding(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Предупреждение",
                tint = ProgressRed,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "Мало",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = ProgressRed
            )
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
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        if (photoUrl != null) {
            val fullUrl = "${AppConfig.BASE_URL}/$photoUrl"

            AsyncImage(
                model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                    .data(fullUrl)
                    .crossfade(true)
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
 * Цвет остатка в зависимости от процента от лимита
 */
@Composable
fun getRemainingColor(remaining: Int, limit: Int): Color {
    val percentage = if (limit > 0) remaining.toFloat() / limit else 1f
    return when {
        percentage > 0.5f -> ProgressGreen
        percentage > 0.2f -> ProgressYellow
        else -> ProgressRed
    }
}
