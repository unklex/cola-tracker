package com.colatracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.colatracker.BuildConfig
import com.colatracker.data.models.Child
import com.colatracker.ui.components.balanceColor
import com.colatracker.ui.components.childPhotoUrl
import com.colatracker.ui.components.formatBalanceInMonths
import com.colatracker.ui.components.formatVolume
import com.colatracker.ui.components.monthUsageColor
import com.colatracker.ui.theme.ColaTheme
import com.colatracker.viewmodels.ChildrenListViewModel
import com.colatracker.viewmodels.ChildrenUiState

/**
 * Экран настроек.
 *
 * Показывает только реальные данные с сервера. Всё, что бэкенд пока не умеет
 * менять (лимиты, уведомления, приватность), помечено как «скоро» и не
 * притворяется рабочим — раньше эти поля выглядели как настройки, но
 * ничего не сохраняли.
 */
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: ChildrenListViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val children = (uiState as? ChildrenUiState.Success)?.children.orEmpty()
    val scrollState = rememberScrollState()

    val totalBalance = children.sumOf { it.remaining }
    val totalTopUp = children.sumOf { it.monthlyLimit }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .navigationBarsPadding()
                .padding(bottom = 32.dp)
        ) {
            SettingsTopBar(onNavigateBack = onNavigateBack)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Настройки",
                        fontSize = 40.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Лимиты семьи и состояние кола-метра.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                FamilyMeterCard(
                    totalBalance = totalBalance,
                    totalTopUp = totalTopUp,
                    childrenCount = children.size
                )

                if (children.isNotEmpty()) {
                    Text(
                        text = "Участники",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    children.forEach { child ->
                        MemberSettingsCard(child = child)
                    }
                }

                ComingSoonSection()

                AboutSection()
            }
        }
    }
}

@Composable
private fun SettingsTopBar(onNavigateBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onNavigateBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Назад",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "Effervescent Archive",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = (-0.3).sp
        )
        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.size(48.dp))
    }
}

/**
 * Сводка по кола-метру семьи.
 */
@Composable
private fun FamilyMeterCard(
    totalBalance: Int,
    totalTopUp: Int,
    childrenCount: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "КОЛА-МЕТР СЕМЬИ",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = formatVolume(totalBalance),
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black,
                    color = if (totalBalance < 0) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onTertiaryContainer
                    },
                    letterSpacing = (-2).sp,
                    lineHeight = 48.sp
                )
                Text(
                    text = if (totalBalance < 0) "перерасход" else "накоплено",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            Text(
                text = "Баланс не сгорает: 1-го числа он пополняется " +
                        "на ${formatVolume(totalTopUp)} на $childrenCount чел.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

/**
 * Карточка участника: только фактические данные с сервера.
 */
@Composable
private fun MemberSettingsCard(child: Child) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = ColaTheme.containers.surfaceContainerLowest,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    val url = childPhotoUrl(child.photoUrl)
                    if (url != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(url)
                                .crossfade(true)
                                .build(),
                            contentDescription = child.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = child.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = formatBalanceInMonths(child)
                            ?: if (child.isOverdrawn) "кола-метр в минусе" else "запаса нет",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            StatRow(
                label = "Начисление в месяц",
                value = formatVolume(child.monthlyLimit),
                valueColor = MaterialTheme.colorScheme.onSurface
            )
            StatRow(
                label = "Выпито в этом месяце",
                value = "${formatVolume(child.consumedThisMonth)} " +
                        "(${child.monthUsagePercent}%)",
                valueColor = monthUsageColor(child)
            )
            StatRow(
                label = "Кола-метр (накоплено)",
                value = formatVolume(child.remaining),
                valueColor = balanceColor(child)
            )
        }
    }
}

@Composable
private fun StatRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = ColaTheme.containers.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
        }
    }
}

/**
 * Честный блок про то, чего пока нет.
 *
 * Изменение лимитов, уведомления и приватность требуют эндпоинтов на бэкенде
 * (`PUT /children/{id}` и т.п.). Пока их нет — не показываем неработающие
 * переключатели.
 */
@Composable
private fun ComingSoonSection() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = ColaTheme.containers.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Скоро",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "• Изменение месячного начисления прямо в приложении\n" +
                        "• Уведомления о превышении расхода\n" +
                        "• Управление доступом и хранением истории",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp
            )
            Text(
                text = "Сейчас лимиты меняются на стороне сервера.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AboutSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Cola Tracker ${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Данные хранятся на вашем сервере.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
