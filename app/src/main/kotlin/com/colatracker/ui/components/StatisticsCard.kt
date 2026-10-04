package com.colatracker.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.colatracker.data.models.DrinkHistoryItem
import com.colatracker.ui.theme.ProgressGreen
import com.colatracker.ui.theme.ProgressRed
import java.time.LocalDate

/**
 * Данные для статистики за день
 */
data class DayStats(
    val date: LocalDate,
    val amount: Int
)

/**
 * Карточка статистики с графиком за неделю
 */
@Composable
fun StatisticsCard(
    history: List<DrinkHistoryItem>,
    modifier: Modifier = Modifier
) {
    // Группируем по дням за последние 7 дней.
    // Дата берётся из DrinkHistoryItem.dateOrNull(), который учитывает
    // смещение таймзоны, если сервер его пришлёт.
    val last7Days = remember(history) {
        val today = LocalDate.now()
        val days = (0..6).map { today.minusDays(it.toLong()) }.reversed()
        val totalsByDate = history
            .mapNotNull { item -> item.dateOrNull()?.let { it to item.amountMl } }
            .groupBy({ it.first }, { it.second })

        days.map { date ->
            DayStats(date, totalsByDate[date]?.sum() ?: 0)
        }
    }

    val totalLast7Days = last7Days.sumOf { it.amount }
    val dailyAverage = if (last7Days.isNotEmpty()) totalLast7Days / last7Days.size else 0

    // Сравнение первой и второй половины недели
    val firstHalf = last7Days.take(3).sumOf { it.amount }
    val secondHalf = last7Days.takeLast(3).sumOf { it.amount }
    val trend = if (secondHalf > firstHalf) TrendDirection.UP else TrendDirection.DOWN
    val trendPercent = if (firstHalf > 0) {
        ((secondHalf - firstHalf).toFloat() / firstHalf * 100).toInt()
    } else 0

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Статистика за неделю",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                ColaIcons.DropIcon(
                    size = 24.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            WeeklyChart(
                data = last7Days,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricBox(
                    label = "Всего за неделю",
                    value = formatVolume(totalLast7Days),
                    color = MaterialTheme.colorScheme.primary
                )

                MetricBox(
                    label = "Среднее/день",
                    value = formatVolume(dailyAverage),
                    color = MaterialTheme.colorScheme.secondary
                )

                TrendMetricBox(
                    label = "Тренд",
                    percent = trendPercent,
                    trend = trend
                )
            }
        }
    }
}

/**
 * График потребления за неделю.
 *
 * Точки ставятся по центрам «слотов» дней, чтобы подписи под графиком
 * совпадали с точками (раньше точки шли от края до края, а подписи
 * раскладывались SpaceBetween — и они разъезжались).
 */
@Composable
private fun WeeklyChart(
    data: List<DayStats>,
    modifier: Modifier = Modifier
) {
    val maxValue = data.maxOfOrNull { it.amount }?.coerceAtLeast(100) ?: 100

    // Анимация «снизу вверх» при появлении: без переключения состояния
    // animateFloatAsState стартовал бы уже в конечной точке и не анимировал.
    var animationStarted by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animationStarted = true }
    val animatedFraction by animateFloatAsState(
        targetValue = if (animationStarted) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "chart_animation"
    )

    val lineColor = MaterialTheme.colorScheme.primary
    val fillTop = lineColor.copy(alpha = 0.30f)
    val fillBottom = lineColor.copy(alpha = 0.05f)
    val dotCoreColor = MaterialTheme.colorScheme.surface

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 22.dp)
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                if (data.isEmpty()) return@Canvas

                val width = size.width
                val height = size.height
                val slot = width / data.size

                val points = data.mapIndexed { index, stat ->
                    val x = slot * (index + 0.5f)
                    val y = height - (stat.amount.toFloat() / maxValue * height * animatedFraction)
                    Offset(x, y.coerceIn(0f, height))
                }

                if (points.size > 1) {
                    // Заливка под линией
                    val fillPath = Path().apply {
                        moveTo(points.first().x, height)
                        points.forEach { lineTo(it.x, it.y) }
                        lineTo(points.last().x, height)
                        close()
                    }
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(colors = listOf(fillTop, fillBottom))
                    )

                    // Линия графика
                    val linePath = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        for (i in 1 until points.size) {
                            val prev = points[i - 1]
                            val current = points[i]
                            val midX = (prev.x + current.x) / 2
                            cubicTo(midX, prev.y, midX, current.y, current.x, current.y)
                        }
                    }
                    drawPath(
                        path = linePath,
                        color = lineColor,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                points.forEach { point ->
                    drawCircle(color = lineColor, radius = 4.dp.toPx(), center = point)
                    drawCircle(color = dotCoreColor, radius = 2.dp.toPx(), center = point)
                }
            }
        }

        // Подписи дней — по одному слоту на день, ровно под точками
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            data.forEach { stat ->
                Text(
                    text = stat.date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Блок метрики
 */
@Composable
private fun MetricBox(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

/**
 * Направление тренда
 */
enum class TrendDirection {
    UP, DOWN
}

/**
 * Блок метрики с трендом
 */
@Composable
private fun TrendMetricBox(
    label: String,
    percent: Int,
    trend: TrendDirection,
    modifier: Modifier = Modifier
) {
    // Рост потребления — тревожный сигнал, снижение — хороший
    val color = if (trend == TrendDirection.UP) ProgressRed else ProgressGreen
    val icon = if (trend == TrendDirection.UP) {
        Icons.Default.KeyboardArrowUp
    } else {
        Icons.Default.KeyboardArrowDown
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "${kotlin.math.abs(percent)}%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}
