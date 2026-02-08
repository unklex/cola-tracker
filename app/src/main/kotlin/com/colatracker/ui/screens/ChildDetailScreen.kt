package com.colatracker.ui.screens

import android.Manifest
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.io.ByteArrayOutputStream
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.colatracker.data.models.Child
import com.colatracker.data.models.DrinkHistoryItem
import com.colatracker.ui.components.ChildAvatar
import com.colatracker.viewmodels.ChildDetailUiState
import com.colatracker.viewmodels.ChildDetailViewModel
import java.io.File
import android.widget.Toast
import kotlinx.coroutines.delay

/**
 * Экран деталей ребёнка с добавлением записей и историей
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildDetailScreen(
    child: Child,
    onNavigateBack: (hasChanges: Boolean) -> Unit
) {
    // Создаём ViewModel с фабрикой
    val viewModel: ChildDetailViewModel = viewModel(
        key = "child_${child.id}",
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return ChildDetailViewModel(child.id, child) as T
            }
        }
    )

    // Обновляем данные при входе на экран
    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isAddingDrink by viewModel.isAddingDrink.collectAsStateWithLifecycle()
    val isUploadingPhoto by viewModel.isUploadingPhoto.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val hasChanges by viewModel.hasChanges.collectAsStateWithLifecycle()

    // Контекст для работы с файлами
    val context = LocalContext.current

    // Состояние для показа bottom sheet выбора фото
    var showPhotoBottomSheet by remember { mutableStateOf(false) }

    // URI для временного файла камеры
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }

    // Launcher для камеры (определяем первым, чтобы использовать в permission callback)
    // Launcher для камеры (определяем первым, чтобы использовать в permission callback)
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            tempPhotoUri?.let { uri ->
                viewModel.processAndUploadPhoto(uri, context)
            }
        }
    }

    // Запрос разрешения камеры перед запуском
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

    // Launcher для выбора из галереи
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { u ->
            viewModel.processAndUploadPhoto(u, context)
        }
    }

    // Toast вместо Snackbar — устраняет layout crash при Scaffold + SnackbarHost
    // Используем LaunchedEffect с задержкой чтобы избежать state change во время measure/draw
    LaunchedEffect(message) {
        message?.let { msg ->
            // Показываем Toast
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            // Ждём завершения текущего frame и ещё немного перед изменением state
            // Это предотвращает remeasure во время draw phase
            delay(200)
            viewModel.clearMessage()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Подробности") },
                navigationIcon = {
                    IconButton(onClick = { onNavigateBack(hasChanges) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is ChildDetailUiState.Loading -> {
                LoadingContent(Modifier.padding(paddingValues))
            }
            
            is ChildDetailUiState.Success -> {
                ChildDetailContent(
                    child = state.child,
                    history = state.history,
                    isAddingDrink = isAddingDrink,
                    isUploadingPhoto = isUploadingPhoto,
                    onAddDrink = { amount -> viewModel.addDrink(amount) },
                    onDeleteDrink = { drinkId -> viewModel.deleteDrink(drinkId) },
                    onAvatarClick = { showPhotoBottomSheet = true },
                    modifier = Modifier.padding(paddingValues)
                )
            }
            
            is ChildDetailUiState.Error -> {
                ErrorContent(
                    message = state.message,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }

    // Bottom sheet для выбора источника фото
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

/**
 * Основной контент экрана.
 * Column + verticalScroll вместо LazyColumn — устраняет "pending composition has not been applied"
 * при subcompose во время measure. История обычно небольшая, lazy не нужен.
 */
@Composable
private fun ChildDetailContent(
    child: Child,
    history: List<DrinkHistoryItem>,
    isAddingDrink: Boolean,
    isUploadingPhoto: Boolean,
    onAddDrink: (Int) -> Unit,
    onDeleteDrink: (Int) -> Unit,
    onAvatarClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ChildInfoHeader(
            child = child,
            isUploadingPhoto = isUploadingPhoto,
            onAvatarClick = onAvatarClick
        )
        QuickAmountSelector(
            isLoading = isAddingDrink,
            onAmountSelected = onAddDrink
        )
        Text(
            text = "История",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )
        if (history.isEmpty()) {
            EmptyHistoryPlaceholder()
        } else {
            history.forEach { drink ->
                DrinkHistoryItemCard(
                    item = drink,
                    onDelete = { onDeleteDrink(drink.id) },
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

/**
 * Шапка с информацией о ребёнке
 */
@Composable
private fun ChildInfoHeader(
    child: Child,
    isUploadingPhoto: Boolean,
    onAvatarClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Аватар с возможностью нажатия (явный размер для стабильного layout с overlay)
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .clickable(enabled = !isUploadingPhoto) { onAvatarClick() },
                contentAlignment = Alignment.Center
            ) {
                ChildAvatar(
                    photoUrl = child.photoUrl,
                    name = child.name,
                    size = 80
                )

                // Иконка добавления фото
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    if (isUploadingPhoto) {
                        Text(
                            text = "…",
                            style = MaterialTheme.typography.labelMedium,
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
            
            // Имя
            Text(
                text = child.name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            // Остаток
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Доступно",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Text(
                    text = "${child.remaining} мл",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Быстрый выбор количества
 */
@Composable
private fun QuickAmountSelector(
    isLoading: Boolean,
    onAmountSelected: (Int) -> Unit
) {
    var showCustomDialog by remember { mutableStateOf(false) }
    
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Сколько выпито?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Кнопка 250 мл
                OutlinedButton(
                    onClick = { onAmountSelected(250) },
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("250 мл", fontWeight = FontWeight.Bold)
                        Text("Стакан", style = MaterialTheme.typography.bodySmall)
                    }
                }
                
                // Кнопка 330 мл
                Button(
                    onClick = { onAmountSelected(330) },
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("330 мл", fontWeight = FontWeight.Bold)
                        Text("Банка", style = MaterialTheme.typography.bodySmall)
                    }
                }
                
                // Кнопка свое количество
                OutlinedButton(
                    onClick = { showCustomDialog = true },
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Свое", fontWeight = FontWeight.Bold)
                        Text("Другое", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            
            // Индикатор загрузки — только текст. Linear/CircularProgressIndicator вызывают
            // NoSuchMethodError (KeyframesSpec) с Compose BOM 2024.01.00.
            if (isLoading) {
                Text(
                    text = "Загрузка…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
    
    // Диалог для ввода кастомного количества
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

/**
 * Диалог для ввода своего количества
 */
@Composable
private fun CustomAmountDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Свое количество") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        amount = it
                        error = null
                    },
                    label = { Text("Количество (мл)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = error != null,
                    supportingText = error?.let { err -> { Text(err) } }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val value = amount.toIntOrNull()
                    when {
                        value == null -> error = "Введите число"
                        value <= 0 -> error = "Количество должно быть больше 0"
                        value > 5000 -> error = "Слишком большое количество"
                        else -> onConfirm(value)
                    }
                }
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

/**
 * Карточка записи в истории с поддержкой свайпа для удаления
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DrinkHistoryItemCard(
    item: DrinkHistoryItem,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "${item.amountMl} мл",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = item.getFormattedDateTime(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Удалить",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    // Диалог подтверждения удаления
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удалить запись?") },
            text = { Text("Вы уверены, что хотите удалить запись о ${item.amountMl} мл?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    }
                ) {
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

/**
 * Placeholder когда история пуста
 */
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "📋",
                style = MaterialTheme.typography.displayMedium
            )
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
private fun LoadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Загрузка…",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorContent(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Bottom sheet для выбора источника фото
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhotoSourceBottomSheet(
    onDismiss: () -> Unit,
    onCameraSelected: () -> Unit,
    onGallerySelected: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Выберите источник фото",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Камера
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCameraSelected() },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Камера",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            // Галерея
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onGallerySelected() },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Галерея",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

/** Конвертирует байты изображения (PNG/WebP и т.д.) в JPEG для загрузки. */
private fun encodeAsJpeg(input: ByteArray): ByteArray? {
    return try {
        val opts = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeByteArray(input, 0, input.size, opts)
        opts.inJustDecodeBounds = false
        opts.inSampleSize = when {
            opts.outWidth <= 1024 && opts.outHeight <= 1024 -> 1
            opts.outWidth <= 2048 && opts.outHeight <= 2048 -> 2
            else -> 4
        }
        val bm = BitmapFactory.decodeByteArray(input, 0, input.size, opts) ?: return null
        val out = ByteArrayOutputStream()
        bm.compress(Bitmap.CompressFormat.JPEG, 85, out)
        bm.recycle()
        out.toByteArray()
    } catch (_: Exception) {
        null
    }
}
