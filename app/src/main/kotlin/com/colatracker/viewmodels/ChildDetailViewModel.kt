package com.colatracker.viewmodels

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colatracker.data.models.Child
import com.colatracker.data.models.DrinkHistoryItem
import com.colatracker.data.repository.ApiProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * UI состояние для экрана ребёнка
 */
sealed class ChildDetailUiState {
    data class Success(
        val child: Child,
        val history: List<DrinkHistoryItem>
    ) : ChildDetailUiState()

    data class Error(val message: String) : ChildDetailUiState()
}

/**
 * ViewModel для экрана деталей ребёнка.
 *
 * Загрузка запускается не из `init`, а из экрана (`LaunchedEffect`): ViewModel
 * живёт в сторе Activity и переиспользуется при повторном открытии того же
 * ребёнка, поэтому данные нужно обновлять на каждый вход. Раньше работали оба
 * механизма сразу и каждый вход стоил четырёх запросов вместо двух.
 */
class ChildDetailViewModel(
    private val childId: Int,
    private val initialChild: Child
) : ViewModel() {

    // Общий на процесс репозиторий (см. ApiProvider)
    private val repository = ApiProvider.repository

    // Стартуем с уже известных данных — экран рисуется мгновенно,
    // свежие цифры приезжают следом.
    private val _uiState = MutableStateFlow<ChildDetailUiState>(
        ChildDetailUiState.Success(initialChild, emptyList())
    )
    val uiState: StateFlow<ChildDetailUiState> = _uiState.asStateFlow()

    /** Идёт первичная загрузка истории (для скелетонов). */
    private val _isLoadingHistory = MutableStateFlow(true)
    val isLoadingHistory: StateFlow<Boolean> = _isLoadingHistory.asStateFlow()

    private val _isAddingDrink = MutableStateFlow(false)
    val isAddingDrink: StateFlow<Boolean> = _isAddingDrink.asStateFlow()

    private val _isUploadingPhoto = MutableStateFlow(false)
    val isUploadingPhoto: StateFlow<Boolean> = _isUploadingPhoto.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    /** Флаг изменения данных (для обновления списка при возврате). */
    private val _hasChanges = MutableStateFlow(false)
    val hasChanges: StateFlow<Boolean> = _hasChanges.asStateFlow()

    /**
     * Версия фотографии для сброса кэша Coil.
     *
     * Бэкенд сохраняет фото под постоянным именем (`photos/child_1.jpg`), поэтому
     * URL после перезагрузки не меняется и Coil продолжает отдавать старую
     * картинку. Версия добавляется в URL как `?v=...` и ломает кэш.
     */
    private val _photoVersion = MutableStateFlow(0L)
    val photoVersion: StateFlow<Long> = _photoVersion.asStateFlow()

    /**
     * Загрузить данные ребёнка и историю. Вызывается при входе на экран.
     */
    fun load() {
        viewModelScope.launch {
            _isLoadingHistory.value = true

            val childResult = repository.getChildren()
            val historyResult = repository.getChildHistory(childId)

            childResult
                .onSuccess { children ->
                    val freshChild = children.find { it.id == childId }
                    if (freshChild == null) {
                        _uiState.value = ChildDetailUiState.Error(
                            "Ребёнок не найден на сервере. Возможно, его удалили."
                        )
                        _isLoadingHistory.value = false
                        return@launch
                    }

                    historyResult
                        .onSuccess { history ->
                            _uiState.value = ChildDetailUiState.Success(freshChild, history)
                        }
                        .onFailure { error ->
                            // История не загрузилась — показываем хотя бы данные ребёнка
                            _uiState.value = ChildDetailUiState.Success(freshChild, emptyList())
                            _message.value = error.userMessage()
                        }
                }
                .onFailure { error ->
                    _uiState.value = ChildDetailUiState.Error(error.userMessage())
                }

            _isLoadingHistory.value = false
        }
    }

    /** Повторить загрузку после ошибки. */
    fun retry() = load()

    /**
     * Добавить запись о выпитом
     */
    fun addDrink(amountMl: Int) {
        if (_isAddingDrink.value) return

        viewModelScope.launch {
            _isAddingDrink.value = true

            repository.addDrink(childId, amountMl)
                .onSuccess { response ->
                    val current = _uiState.value
                    // Сервер вернул и обновлённого ребёнка, и созданную запись
                    _uiState.value = ChildDetailUiState.Success(
                        child = response.child,
                        history = listOf(response.drink) +
                                (current as? ChildDetailUiState.Success)?.history.orEmpty()
                    )
                    _hasChanges.value = true
                    _message.value = "Добавлено $amountMl мл"
                }
                .onFailure { error ->
                    _message.value = error.userMessage()
                }

            _isAddingDrink.value = false
        }
    }

    /**
     * Удалить запись из истории.
     *
     * Счётчики ребёнка не пересчитываются вручную — это дублировало бы бизнес-логику
     * бэкенда. Запись убирается из списка сразу (быстрый отклик), а актуальные
     * цифры подтягиваются с сервера.
     */
    fun deleteDrink(drinkId: Int) {
        viewModelScope.launch {
            repository.deleteDrink(drinkId)
                .onSuccess {
                    val current = _uiState.value
                    if (current is ChildDetailUiState.Success) {
                        _uiState.value = current.copy(
                            history = current.history.filter { it.id != drinkId }
                        )
                    }
                    _hasChanges.value = true
                    _message.value = "Запись удалена"
                    refreshChildCounters()
                }
                .onFailure { error ->
                    _message.value = error.userMessage()
                }
        }
    }

    /**
     * Обработать и загрузить фото (в фоне).
     * Эффективно обрабатывает изображения любого размера без перегрузки памяти.
     */
    fun processAndUploadPhoto(uri: Uri, context: Context) {
        if (_isUploadingPhoto.value) return

        viewModelScope.launch {
            _isUploadingPhoto.value = true
            try {
                // Обработка изображения на IO-потоке, мутации StateFlow — на Main
                val jpegBytes = withContext(Dispatchers.IO) {
                    // Шаг 1: Читаем размеры изображения БЕЗ загрузки в память
                    val bounds = context.contentResolver.openInputStream(uri)?.use { stream ->
                        val opts = BitmapFactory.Options().apply {
                            inJustDecodeBounds = true
                        }
                        BitmapFactory.decodeStream(stream, null, opts)
                        opts
                    }

                    if (bounds == null || bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                        return@withContext null
                    }

                    // Шаг 2: Вычисляем оптимальный inSampleSize
                    val targetSize = 1024
                    val sampleSize = calculateInSampleSize(
                        bounds.outWidth, bounds.outHeight, targetSize, targetSize
                    )

                    // Шаг 3: Загружаем уменьшенное изображение
                    val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                        val opts = BitmapFactory.Options().apply {
                            inSampleSize = sampleSize
                            inJustDecodeBounds = false
                        }
                        BitmapFactory.decodeStream(stream, null, opts)
                    } ?: return@withContext null

                    // Шаг 4: Сжимаем в JPEG
                    ByteArrayOutputStream().use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                        bitmap.recycle()
                        out.toByteArray()
                    }
                }

                if (jpegBytes == null) {
                    _message.value = "Не удалось обработать изображение"
                    return@launch
                }

                // Шаг 5: Загружаем на сервер
                val fileName = "photo_${System.currentTimeMillis()}.jpg"
                repository.uploadPhoto(childId, jpegBytes, fileName)
                    .onSuccess { response ->
                        _message.value = response.message
                        _hasChanges.value = true

                        // Сбрасываем кэш картинки: путь на сервере не меняется
                        _photoVersion.value = System.currentTimeMillis()

                        val current = _uiState.value
                        val newPhotoUrl = response.photoUrl
                        if (current is ChildDetailUiState.Success && newPhotoUrl != null) {
                            // Сервер уже вернул новый путь — лишний запрос не нужен
                            _uiState.value = current.copy(
                                child = current.child.copy(photoUrl = newPhotoUrl)
                            )
                        } else {
                            load()
                        }
                    }
                    .onFailure { error ->
                        _message.value = error.userMessage()
                    }
            } catch (e: CancellationException) {
                // Уход с экрана во время загрузки — не ошибка
                throw e
            } catch (e: Exception) {
                _message.value = "Не удалось обработать изображение: ${e.message}"
            } finally {
                _isUploadingPhoto.value = false
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    /** Подтянуть актуальные счётчики ребёнка, не трогая историю. */
    private suspend fun refreshChildCounters() {
        repository.getChildren()
            .onSuccess { children ->
                val fresh = children.find { it.id == childId } ?: return@onSuccess
                val current = _uiState.value
                if (current is ChildDetailUiState.Success) {
                    _uiState.value = current.copy(child = fresh)
                }
            }
    }

    /**
     * Вычисляет оптимальный inSampleSize для уменьшения изображения
     */
    private fun calculateInSampleSize(
        width: Int,
        height: Int,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
