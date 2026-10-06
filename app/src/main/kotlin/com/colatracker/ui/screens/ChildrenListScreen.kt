package com.colatracker.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import com.colatracker.data.models.Child
import com.colatracker.ui.components.ChildCardSkeleton
import com.colatracker.ui.components.ColaIcons
import com.colatracker.ui.components.CompactGradientProgressBar
import com.colatracker.ui.components.balanceColor
import com.colatracker.ui.components.childPhotoUrl
import com.colatracker.ui.components.formatLastUpdated
import com.colatracker.ui.components.formatVolume
import com.colatracker.ui.components.monthUsageColor
import com.colatracker.ui.components.needsAttention
import com.colatracker.ui.theme.ColaTheme
import com.colatracker.ui.theme.ProgressRed
import com.colatracker.viewmodels.ChildrenListViewModel
import com.colatracker.viewmodels.ChildrenUiState

/** Объём «одной банки» для быстрого добавления с главного экрана. */
private const val QUICK_ADD_ML = 330

/**
 * Главный экран — Effervescent Archive
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildrenListScreen(
    onChildClick: (Child) -> Unit,
    onSettingsClick: () -> Unit = {},
    viewModel: ChildrenListViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val quickAddFor by viewModel.quickAddFor.collectAsStateWithLifecycle()
    val lastUpdatedMillis by viewModel.lastUpdatedMillis.collectAsStateWithLifecycle()

    var showQuickAddSheet by remember { mutableStateOf(false) }
    val children = (uiState as? ChildrenUiState.Success)?.children.orEmpty()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        message?.let { msg ->
            val result = snackbarHostState.showSnackbar(
                message = msg.text,
                actionLabel = msg.retry?.let { "Повторить" },
                duration = if (msg.retry != null) SnackbarDuration.Long else SnackbarDuration.Short
            )
            viewModel.clearMessage()
            if (result == SnackbarResult.ActionPerformed) {
                // Тот же requestId: если первый запрос дошёл до сервера, дубля не будет
                msg.retry?.let { viewModel.quickAddDrink(it.childId, it.amountMl, it.requestId) }
            }
        }
    }

    // Жест «потянуть вниз»: запускает то же обновление, что и кнопка в шапке
    val pullState = rememberPullToRefreshState()
    if (pullState.isRefreshing) {
        LaunchedEffect(true) { viewModel.refresh() }
    }
    LaunchedEffect(isRefreshing) {
        if (!isRefreshing) pullState.endRefresh()
    }

    val lastUpdatedText = lastUpdatedMillis?.let {
        formatLastUpdated(
            updated = LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault()),
            now = LocalDateTime.now()
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .then(
                if (children.isNotEmpty()) Modifier.nestedScroll(pullState.nestedScrollConnection)
                else Modifier
            )
    ) {
        when (val state = uiState) {
            is ChildrenUiState.Loading -> {
                LoadingContent()
            }

            is ChildrenUiState.Success -> {
                if (state.children.isEmpty()) {
                    EmptyContent(onRefresh = { viewModel.refresh() })
                } else {
                    EffervescentHomeContent(
                        children = state.children,
                        onChildClick = onChildClick,
                        onRefresh = { viewModel.refresh() },
                        onSettingsClick = onSettingsClick,
                        lastUpdatedText = lastUpdatedText
                    )
                }
            }

            is ChildrenUiState.Error -> {
                ErrorContent(
                    message = state.message,
                    onRetry = { viewModel.loadChildren() }
                )
            }
        }

        // Индикатор обновления (при жесте «потянуть» вместо него работает круглый индикатор)
        if (isRefreshing && !pullState.isRefreshing) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .align(Alignment.TopCenter),
                color = MaterialTheme.colorScheme.primary,
                trackColor = ColaTheme.containers.surfaceContainerLow
            )
        }

        if (children.isNotEmpty()) {
            PullToRefreshContainer(
                state = pullState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
            )
        }

        // FAB — быстрое добавление. Прячем, когда добавлять некому.
        if (children.isNotEmpty()) {
            FloatingActionButton(
                onClick = { showQuickAddSheet = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 24.dp, bottom = 24.dp)
                    .size(64.dp),
                shape = CircleShape,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = ColaTheme.gradients.colaCtaGradient,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Быстро добавить напиток",
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        )
    }

    if (showQuickAddSheet) {
        QuickAddSheet(
            children = children,
            busyChildId = quickAddFor,
            onDismiss = { showQuickAddSheet = false },
            onQuickAdd = { child ->
                showQuickAddSheet = false
                viewModel.quickAddDrink(child.id, QUICK_ADD_ML)
            },
            onOpenChild = { child ->
                showQuickAddSheet = false
                onChildClick(child)
            }
        )
    }
}

/**
 * Шторка быстрого добавления.
 *
 * Раньше эта шторка называлась «Добавить напиток», но по нажатию только
 * открывала экран ребёнка. Теперь она действительно добавляет банку.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickAddSheet(
    children: List<Child>,
    busyChildId: Int?,
    onDismiss: () -> Unit,
    onQuickAdd: (Child) -> Unit,
    onOpenChild: (Child) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ColaTheme.containers.surfaceContainerLowest,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Добавить $QUICK_ADD_ML мл",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Нажмите на имя, чтобы открыть карточку целиком",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            children.forEach { child ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onOpenChild(child) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ChildPhoto(child = child, size = 40.dp, iconSize = 20.dp)

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = child.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Баланс: ${formatVolume(child.remaining)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = balanceColor(child)
                            )
                        }
                    }

                    Button(
                        onClick = { onQuickAdd(child) },
                        enabled = busyChildId == null,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        if (busyChildId == child.id) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$QUICK_ADD_ML",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Основной контент главной страницы
 */
@Composable
private fun EffervescentHomeContent(
    children: List<Child>,
    onChildClick: (Child) -> Unit,
    onRefresh: () -> Unit,
    onSettingsClick: () -> Unit = {},
    lastUpdatedText: String? = null
) {
    // Агрегаты по семье
    val totalConsumed = children.sumOf { it.consumedThisMonth }
    val totalLimit = children.sumOf { it.monthlyLimit }
    val totalBalance = children.sumOf { it.remaining }
    val monthUsagePercent = if (totalLimit > 0) {
        (totalConsumed.toFloat() / totalLimit * 100f).toInt()
    } else 0

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item(key = "topbar") {
            EffervescentTopBar(
                firstChild = children.firstOrNull(),
                onRefresh = onRefresh,
                onSettingsClick = onSettingsClick
            )
        }

        if (lastUpdatedText != null) {
            item(key = "updated") {
                Text(
                    text = lastUpdatedText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 24.dp, top = 12.dp)
                )
            }
        }

        item(key = "hero") {
            MonthUsageHero(
                usagePercent = monthUsagePercent,
                totalConsumed = totalConsumed,
                totalLimit = totalLimit
            )
        }

        item(key = "meter") {
            FamilyColaMeterCard(
                totalBalance = totalBalance,
                monthlyTopUp = totalLimit,
                childrenCount = children.size
            )
        }

        items(children, key = { "member_${it.id}" }) { child ->
            MemberCard(
                child = child,
                onClick = { onChildClick(child) }
            )
        }

        item(key = "insights") {
            InsightsSection(
                monthUsagePercent = monthUsagePercent,
                children = children
            )
        }
    }
}

// ============================================================
// Top App Bar
// ============================================================

@Composable
private fun EffervescentTopBar(
    firstChild: Child?,
    onRefresh: () -> Unit,
    onSettingsClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(ColaTheme.containers.surfaceBright.copy(alpha = 0.9f))
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ColaIcons.AppLogo(
                    size = 32.dp,
                    primaryColor = MaterialTheme.colorScheme.primary,
                    accentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Effervescent Archive",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = (-0.5).sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onRefresh, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Обновить",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(ColaTheme.containers.surfaceContainerHigh)
                        .clickable { onSettingsClick() },
                    contentAlignment = Alignment.Center
                ) {
                    if (firstChild != null) {
                        ChildPhoto(
                            child = firstChild,
                            size = 40.dp,
                            iconSize = 22.dp,
                            contentDescription = "Настройки"
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Настройки",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

// ============================================================
// Hero — расход за ТЕКУЩИЙ МЕСЯЦ (раньше ошибочно назывался «сегодняшним»)
// ============================================================

@Composable
private fun MonthUsageHero(
    usagePercent: Int,
    totalConsumed: Int,
    totalLimit: Int
) {
    val progress = if (totalLimit > 0) totalConsumed.toFloat() / totalLimit else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(1000),
        label = "heroProgress"
    )

    val accentColor = monthUsageColor(progress)
    val ringTrackColor = ColaTheme.containers.surfaceContainerHigh

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "ВЫПИТО В ЭТОМ МЕСЯЦЕ",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$usagePercent%",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                letterSpacing = (-2).sp,
                lineHeight = 56.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = monthUsageComment(usagePercent),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )
        }

        Box(
            modifier = Modifier.size(140.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)

                drawArc(
                    color = ringTrackColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = stroke
                )

                drawArc(
                    color = accentColor,
                    startAngle = -90f,
                    sweepAngle = animatedProgress * 360f,
                    useCenter = false,
                    style = stroke
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ColaIcons.GlassIcon(
                    size = 32.dp,
                    glassColor = accentColor.copy(alpha = 0.3f),
                    liquidColor = accentColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatVolume(totalConsumed),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "/ ${formatVolume(totalLimit)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun monthUsageComment(percent: Int): String = when {
    percent < 30 -> "Расход небольшой — отличный результат!"
    percent < 60 -> "Расход в норме, баланс соблюдён."
    percent < 80 -> "Расход выше среднего — стоит присмотреться."
    percent <= 100 -> "Месячное начисление почти израсходовано."
    else -> "Начисление превышено — тратится накопленный запас."
}

// ============================================================
// Кола-метр семьи
//
// Это НЕ «цель на месяц»: remaining копится из месяца в месяц и не сгорает.
// ============================================================

@Composable
private fun FamilyColaMeterCard(
    totalBalance: Int,
    monthlyTopUp: Int,
    childrenCount: Int
) {
    val isOverdrawn = totalBalance < 0

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(24.dp),
                    ambientColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                    spotColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                )
                .background(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Кола-метр семьи",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "+${formatVolume(monthlyTopUp)} 1-го числа · $childrenCount чел.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatVolume(totalBalance),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isOverdrawn) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onTertiaryContainer
                        },
                        lineHeight = 36.sp
                    )
                    Text(
                        text = if (isOverdrawn) "перерасход" else "накоплено",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

// ============================================================
// Карточка ребёнка
// ============================================================

@Composable
private fun MemberCard(
    child: Child,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
                spotColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)
            )
            .background(
                color = ColaTheme.containers.surfaceContainerLowest,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ChildPhoto(child = child, size = 48.dp, iconSize = 24.dp)

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = child.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "За месяц: ${formatVolume(child.consumedThisMonth)} " +
                                "из ${formatVolume(child.monthlyLimit)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (needsAttention(child)) {
                    AttentionBadge(isOverdrawn = child.isOverdrawn)
                }
            }

            // Расход месяца
            CompactGradientProgressBar(
                progress = child.monthUsageProgress,
                height = 10.dp
            )

            // Кола-метр ребёнка
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = ColaTheme.containers.surfaceContainerLow,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Кола-метр",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatVolume(child.remaining),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = balanceColor(child)
                    )
                }

                val usageColor = monthUsageColor(child)
                Text(
                    text = "${child.monthUsagePercent}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = usageColor,
                    modifier = Modifier
                        .background(
                            color = usageColor.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(50)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun AttentionBadge(isOverdrawn: Boolean) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = ProgressRed.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = ProgressRed,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = if (isOverdrawn) "Минус" else "Много",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = ProgressRed
            )
        }
    }
}

/**
 * Фото ребёнка либо placeholder.
 */
@Composable
private fun ChildPhoto(
    child: Child,
    size: Dp,
    iconSize: Dp,
    contentDescription: String? = null
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(ColaTheme.containers.surfaceContainerHigh),
        contentAlignment = Alignment.Center
    ) {
        val url = childPhotoUrl(child.photoUrl)
        if (url != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(url)
                    .crossfade(true)
                    .build(),
                contentDescription = contentDescription ?: child.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

// ============================================================
// Наблюдения
// ============================================================

@Composable
private fun InsightsSection(
    monthUsagePercent: Int,
    children: List<Child>
) {
    Column(
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Text(
            text = "Наблюдения",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            letterSpacing = (-0.3).sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        InsightItem(
            iconText = "📊",
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            title = "Расход за месяц",
            subtitle = monthUsageComment(monthUsagePercent)
        )

        Spacer(modifier = Modifier.height(10.dp))

        val leader = children.maxByOrNull { it.consumedThisMonth }
        if (leader != null) {
            InsightItem(
                iconText = "⭐",
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                title = "Больше всех: ${leader.name}",
                subtitle = "Выпито ${formatVolume(leader.consumedThisMonth)} " +
                        "из начисления ${formatVolume(leader.monthlyLimit)}"
            )
        }

        val overdrawn = children.filter { it.isOverdrawn }
        if (overdrawn.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            InsightItem(
                iconText = "⚠️",
                containerColor = MaterialTheme.colorScheme.errorContainer,
                title = "Кола-метр в минусе",
                subtitle = overdrawn.joinToString(", ") { "${it.name} (${formatVolume(it.remaining)})" }
            )
        }
    }
}

@Composable
private fun InsightItem(
    iconText: String,
    containerColor: Color,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = ColaTheme.containers.surfaceContainer,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(color = containerColor, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = iconText,
                style = MaterialTheme.typography.titleMedium
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }
    }
}

// ============================================================
// Loading / Empty / Error
// ============================================================

@Composable
private fun LoadingContent() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .background(
                        ColaTheme.containers.surfaceContainerLow,
                        RoundedCornerShape(16.dp)
                    )
            )
        }
        items(3) {
            ChildCardSkeleton()
        }
    }
}

@Composable
private fun EmptyContent(onRefresh: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                ColaIcons.BottleIcon(
                    size = 48.dp,
                    liquidColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
            }

            Text(
                text = "Пока никого нет",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Добавьте детей через бэкенд\nдля начала отслеживания",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            RetryButton(text = "Обновить", onClick = onRefresh)
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "😕",
                    style = MaterialTheme.typography.displayMedium
                )
            }

            Text(
                text = "Не удалось загрузить",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            RetryButton(text = "Повторить", onClick = onRetry)
        }
    }
}

@Composable
private fun RetryButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        shape = RoundedCornerShape(50)
    ) {
        Icon(
            Icons.Default.Refresh,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text)
    }
}
