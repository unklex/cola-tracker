package com.colatracker.ui.screens

import android.Manifest
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.colatracker.data.models.Child
import com.colatracker.data.models.DrinkHistoryItem
import com.colatracker.data.models.dayLabel
import com.colatracker.data.models.groupHistoryByDay
import com.colatracker.ui.components.AnimatedCounter
import com.colatracker.ui.components.CircularProgressAvatar
import com.colatracker.ui.components.ColaDetailTopAppBar
import com.colatracker.ui.components.ColaIcons
import com.colatracker.ui.components.GradientProgressBar
import com.colatracker.ui.components.HistoryItemSkeleton
import com.colatracker.ui.components.StatisticsCard
import com.colatracker.ui.components.balanceColor
import com.colatracker.ui.components.formatBalanceInMonths
import com.colatracker.ui.components.formatVolume
import com.colatracker.ui.components.monthUsageColor
import com.colatracker.ui.theme.ColaRed
import com.colatracker.viewmodels.ChildDetailUiState
import com.colatracker.viewmodels.ChildDetailViewModel
import java.io.File
import java.time.LocalDate

/**
 * Экран деталей ребёнка
 */
@Composable
fun ChildDetailScreen(
    child: Child,
    onNavigateBack: (hasChanges: Boolean) -> Unit
) {
    val viewModel: ChildDetailViewModel = viewModel(
        key = "child_${child.id}",
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return ChildDetailViewModel(child.id, child) as T
            }
        }
    )

    // ViewModel переиспользуется при повторном открытии того же ребёнка,
    // поэтому данные обновляем на каждый вход (и только здесь, не в init).
    LaunchedEffect(child.id) {
        viewModel.load()
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isLoadingHistory by viewModel.isLoadingHistory.collectAsStateWithLifecycle()
    val isAddingDrink by viewModel.isAddingDrink.collectAsStateWithLifecycle()
    val isUploadingPhoto by viewModel.isUploadingPhoto.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val hasChanges by viewModel.hasChanges.collectAsStateWithLifecycle()
    val photoVersion by viewModel.photoVersion.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showPhotoBottomSheet by remember { mutableStateOf(false) }
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            tempPhotoUri?.let { uri ->
                viewModel.processAndUploadPhoto(uri, context)
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val photoFile = File.createTempFile("photo_", ".jpg", context.cacheDir)
            tempPhotoUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                photoFile
            )
            tempPhotoUri?.let { cameraLauncher.launch(it) }
        } else {
            Toast.makeText(context, "Нужно разрешение камеры", Toast.LENGTH_SHORT).show()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.processAndUploadPhoto(it, context) }
    }

    // Сообщения показываем снекбаром: тост исчезал слишком быстро
    // и терялся на фоне остальной анимации.
    LaunchedEffect(message) {
        message?.let { msg ->
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            ColaDetailTopAppBar(
                title = child.name,
                onNavigateBack = { onNavigateBack(hasChanges) }
            )

            when (val state = uiState) {
                is ChildDetailUiState.Success -> {
                    ChildDetailContent(
                        child = state.child,
                        history = state.history,
                        isLoadingHistory = isLoadingHistory,
                        isAddingDrink = isAddingDrink,
                        isUploadingPhoto = isUploadingPhoto,
                        photoVersion = photoVersion,
                        onAddDrink = { amount -> viewModel.addDrink(amount) },
                        onDeleteDrink = { drinkId -> viewModel.deleteDrink(drinkId) },
                        onAvatarClick = { showPhotoBottomSheet = true }
                    )
                }

                is ChildDetailUiState.Error -> {
                    ErrorContent(
                        message = state.message,
                        onRetry = { viewModel.retry() }
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

    if (showPhotoBottomSheet) {
        PhotoSourceBottomSheet(
            onDismiss = { showPhotoBottomSheet = false },
            onCameraSelected = {
                showPhotoBottomSheet = false
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            },
            onGallerySelected = {
                showPhotoBottomSheet = false
                galleryLauncher.launch("image/*")
            }
        )
    }
}

@Composable
private fun ChildDetailContent(
    child: Child,
    history: List<DrinkHistoryItem>,
    isLoadingHistory: Boolean,
    isAddingDrink: Boolean,
    isUploadingPhoto: Boolean,
    photoVersion: Long,
    onAddDrink: (Int) -> Unit,
    onDeleteDrink: (Int) -> Unit,
    onAvatarClick: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        ChildInfoHeader(
            child = child,
            isUploadingPhoto = isUploadingPhoto,
            photoVersion = photoVersion,
            onAvatarClick = onAvatarClick
        )

        Column(
            modifier = Modifier
                .padding(16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MonthUsageSection(child = child)

            QuickAmountSelector(
                isLoading = isAddingDrink,
                onAmountSelected = onAddDrink
            )

            if (history.isNotEmpty()) {
                StatisticsCard(history = history)
            }

            HistorySection(
                history = history,
                isLoading = isLoadingHistory,
                onDeleteDrink = onDeleteDrink
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Шапка: аватар с расходом месяца + накопленный баланс («кола-метр»).
 */
@Composable
private fun ChildInfoHeader(
    child: Child,
    isUploadingPhoto: Boolean,
    photoVersion: Long,
    onAvatarClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        Color.Transparent
                    )
                )
            )
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressAvatar(
                photoUrl = child.photoUrl,
                name = child.name,
                progress = child.monthUsageProgress,
                size = 120.dp,
                strokeWidth = 8.dp,
                isUploading = isUploadingPhoto,
                photoVersion = photoVersion,
                onAvatarClick = onAvatarClick
            )

            Text(
                text = child.name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Это накопленный баланс, а не «сколько осталось до месячной цели»:
            // он переносится между месяцами и может уйти в минус.
            AnimatedCounter(
                targetValue = child.remaining,
                label = "Кола-метр (накоплено)",
                color = balanceColor(child),
                caption = if (child.isOverdrawn) {
                    "перерасход — баланс в минусе"
                } else {
                    formatBalanceInMonths(child)
                }
            )
        }
    }
}

/**
 * Расход за текущий месяц.
 */
@Composable
private fun MonthUsageSection(child: Child) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Расход за месяц",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${formatVolume(child.consumedThisMonth)} / " +
                            formatVolume(child.monthlyLimit),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            GradientProgressBar(
                progress = child.monthUsageProgress,
                height = 20.dp,
                showPercentage = true,
                showTicks = true
            )

            Text(
                text = if (child.isOverLimit) {
                    "Начисление превышено на ${formatVolume(child.overLimitMl)} " +
                            "(${child.monthUsagePercent}%) — расходуется накопленное"
                } else {
                    "1-го числа кола-метр пополнится на ${formatVolume(child.monthlyLimit)}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (child.isOverLimit) {
                    monthUsageColor(child)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

// internal, а не private: экран проверяется UI-тестом (androidTest)
@Composable
internal fun QuickAmountSelector(
    isLoading: Boolean,
    onAmountSelected: (Int) -> Unit
) {
    var showCustomDialog by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Добавить напиток",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                ColaIcons.GlassIcon(
                    size = 24.dp,
                    liquidColor = MaterialTheme.colorScheme.primary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DrinkButton(
                    amount = 250,
                    label = "Стакан",
                    icon = {
                        ColaIcons.GlassIcon(
                            size = 20.dp,
                            liquidColor = MaterialTheme.colorScheme.primary
                        )
                    },
                    isPrimary = false,
                    enabled = !isLoading,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onAmountSelected(250)
                    },
                    modifier = Modifier.weight(1f)
                )

                DrinkButton(
                    amount = 330,
                    label = "Банка",
                    icon = {
                        ColaIcons.CanIcon(
                            size = 20.dp,
                            primaryColor = MaterialTheme.colorScheme.onPrimary
                        )
                    },
                    isPrimary = true,
                    enabled = !isLoading,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onAmountSelected(330)
                    },
                    modifier = Modifier.weight(1f)
                )

                DrinkButton(
                    amount = null,
                    label = "Другое",
                    icon = {
                        ColaIcons.DropIcon(
                            size = 20.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    isPrimary = false,
                    enabled = !isLoading,
                    onClick = { showCustomDialog = true },
                    modifier = Modifier.weight(1f)
                )
            }

            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    if (showCustomDialog) {
        CustomAmountDialog(
            onDismiss = { showCustomDialog = false },
            onConfirm = { amount ->
                onAmountSelected(amount)
                showCustomDialog = false
            }
        )
    }
}

@Composable
private fun DrinkButton(
    amount: Int?,
    label: String,
    icon: @Composable () -> Unit,
    isPrimary: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val content: @Composable ColumnScope.() -> Unit = {
        icon()
        Text(
            text = amount?.let { "$it мл" } ?: "?",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelLarge
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall
        )
    }

    if (isPrimary) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.height(72.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                content = content
            )
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.height(72.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                content = content
            )
        }
    }
}

@Composable
private fun HistorySection(
    history: List<DrinkHistoryItem>,
    isLoading: Boolean,
    onDeleteDrink: (Int) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "История",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        when {
            isLoading && history.isEmpty() -> {
                repeat(3) { HistoryItemSkeleton() }
            }

            history.isEmpty() -> {
                EmptyHistoryPlaceholder()
            }

            else -> {
                val groups = remember(history) { groupHistoryByDay(history) }
                val today = remember { LocalDate.now() }
                groups.forEach { group ->
                    DayHeader(
                        label = dayLabel(group.date, today),
                        totalMl = group.totalMl
                    )
                    group.items.forEach { drink ->
                        DrinkHistoryItemCard(
                            item = drink,
                            // Дата уже в заголовке группы — в карточке только время
                            showDate = group.date == null,
                            onDelete = { onDeleteDrink(drink.id) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Заголовок дня в истории: «Сегодня · 580 мл».
 */
@Composable
private fun DayHeader(label: String, totalMl: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, start = 4.dp, end = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = formatVolume(totalMl),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DrinkHistoryItemCard(
    item: DrinkHistoryItem,
    showDate: Boolean,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    ColaIcons.CanIcon(
                        size = 24.dp,
                        primaryColor = MaterialTheme.colorScheme.primary
                    )
                }

                Column {
                    Text(
                        text = "${item.amountMl} мл",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (showDate) item.getFormattedDateTime() else item.getFormattedTime(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Удалить",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удалить запись?") },
            text = { Text("Вы уверены, что хотите удалить запись о ${item.amountMl} мл?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDelete()
                }) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
private fun EmptyHistoryPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                ColaIcons.GlassIcon(
                    size = 32.dp,
                    liquidColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
            }

            Text(
                text = "История пуста",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Добавьте первую запись выше",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CustomAmountDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Своё количество") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { newValue ->
                        // Пускаем только цифры, чтобы не ловить ошибку после ввода
                        if (newValue.length <= 4 && newValue.all { it.isDigit() }) {
                            amount = newValue
                            error = null
                        }
                    },
                    label = { Text("Количество (мл)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = error != null,
                    supportingText = error?.let { err -> { Text(err) } },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val value = amount.toIntOrNull()
                    when {
                        value == null -> error = "Введите число"
                        value <= 0 -> error = "Количество должно быть больше 0"
                        value > 5000 -> error = "Слишком большое количество"
                        else -> onConfirm(value)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Добавить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
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
            Text(
                text = "😕",
                style = MaterialTheme.typography.displayMedium
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Повторить")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhotoSourceBottomSheet(
    onDismiss: () -> Unit,
    onCameraSelected: () -> Unit,
    onGallerySelected: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Выберите источник фото",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            PhotoSourceRow(
                title = "Камера",
                subtitle = "Сделать новое фото",
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                iconBackground = ColaRed.copy(alpha = 0.2f),
                icon = { ColaIcons.DropIcon(size = 24.dp, color = ColaRed) },
                onClick = onCameraSelected
            )

            PhotoSourceRow(
                title = "Галерея",
                subtitle = "Выбрать из галереи",
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                iconBackground = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                icon = {
                    ColaIcons.BottleIcon(
                        size = 24.dp,
                        liquidColor = MaterialTheme.colorScheme.secondary
                    )
                },
                onClick = onGallerySelected
            )
        }
    }
}

@Composable
private fun PhotoSourceRow(
    title: String,
    subtitle: String,
    containerColor: Color,
    iconBackground: Color,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = containerColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(iconBackground),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
