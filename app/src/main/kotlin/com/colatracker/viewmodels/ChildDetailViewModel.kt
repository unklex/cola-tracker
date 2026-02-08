package com.colatracker.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colatracker.AppConfig
import com.colatracker.data.api.ColaTrackerApi
import com.colatracker.data.models.Child
import com.colatracker.data.models.DrinkHistoryItem
import com.colatracker.data.repository.ColaTrackerRepository
import com.colatracker.data.repository.MockRepository
import com.colatracker.data.repository.RealRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI состояние для экрана ребёнка
 */
sealed class ChildDetailUiState {
    object Loading : ChildDetailUiState()
    data class Success(
        val child: Child,
        val history: List<DrinkHistoryItem>
    ) : ChildDetailUiState()
    data class Error(val message: String) : ChildDetailUiState()
}

/**
 * ViewModel для экрана деталей ребёнка
 */
class ChildDetailViewModel(
    private val childId: Int,
    private val initialChild: Child
) : ViewModel() {
    
    private val repository: ColaTrackerRepository = if (AppConfig.USE_MOCK_DATA) {
        MockRepository()
    } else {
        RealRepository(ColaTrackerApi())
    }
    
    // UI состояние
    private val _uiState = MutableStateFlow<ChildDetailUiState>(
        ChildDetailUiState.Success(initialChild, emptyList())
    )
    val uiState: StateFlow<ChildDetailUiState> = _uiState.asStateFlow()
    
    // Состояние добавления записи
    private val _isAddingDrink = MutableStateFlow(false)
    val isAddingDrink: StateFlow<Boolean> = _isAddingDrink.asStateFlow()

    // Состояние загрузки фото
    private val _isUploadingPhoto = MutableStateFlow(false)
    val isUploadingPhoto: StateFlow<Boolean> = _isUploadingPhoto.asStateFlow()
    
    // Сообщение об успехе/ошибке
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    // Флаг изменения данных (для обновления списка при возврате)
    private val _hasChanges = MutableStateFlow(false)
    val hasChanges: StateFlow<Boolean> = _hasChanges.asStateFlow()

    init {
        loadChildData()
    }
    
    /**
     * Загрузить данные ребёнка и историю
     */
    private fun loadChildData() {
        viewModelScope.launch {
            // Не переключаем в Loading — остаёмся на Success(initial) до завершения.
            // Резкая смена контента (Loading ↔ Success) может вызывать layout crash в Scaffold.

            // Загружаем свежие данные ребёнка с сервера
            val childResult = repository.getChildren()
            val historyResult = repository.getChildHistory(childId)

            childResult
                .onSuccess { children ->
                    val freshChild = children.find { it.id == childId } ?: initialChild
                    historyResult
                        .onSuccess { history ->
                            _uiState.value = ChildDetailUiState.Success(
                                child = freshChild,
                                history = history
                            )
                        }
                        .onFailure { error ->
                            // Если история не загрузилась, показываем хотя бы данные ребёнка
                            _uiState.value = ChildDetailUiState.Success(
                                child = freshChild,
                                history = emptyList()
                            )
                            _message.value = "Ошибка загрузки истории: ${error.message}"
                        }
                }
                .onFailure { error ->
                    _uiState.value = ChildDetailUiState.Error(
                        error.message ?: "Ошибка загрузки данных"
                    )
                }
        }
    }
    
    /**
     * Добавить запись о выпитом
     */
    fun addDrink(amountMl: Int) {
        viewModelScope.launch {
            _isAddingDrink.value = true
            
            repository.addDrink(childId, amountMl)
                .onSuccess { response ->
                    // Обновляем состояние с новыми данными
                    val currentState = _uiState.value
                    if (currentState is ChildDetailUiState.Success) {
                        // Батчим обновления состояния чтобы избежать множественных remeasure
                        _uiState.value = ChildDetailUiState.Success(
                            child = response.child,
                            history = listOf(response.drink) + currentState.history
                        )
                        _hasChanges.value = true
                        // Задержка перед сообщением чтобы избежать state change во время measure
                        kotlinx.coroutines.delay(50)
                        _message.value = "Добавлено $amountMl мл"
                    }
                }
                .onFailure { error ->
                    kotlinx.coroutines.delay(50)
                    _message.value = "Ошибка: ${error.message}"
                }

            // Задержка перед сбросом isLoading чтобы избежать remeasure во время draw
            kotlinx.coroutines.delay(100)
            _isAddingDrink.value = false
        }
    }

    /**
     * Удалить запись из истории
     */
    fun deleteDrink(drinkId: Int) {
        viewModelScope.launch {
            repository.deleteDrink(drinkId)
                .onSuccess { response ->
                    // Обновляем историю - удаляем запись
                    val currentState = _uiState.value
                    if (currentState is ChildDetailUiState.Success) {
                        val updatedHistory = currentState.history
                            .filter { it.id != drinkId }

                        // Пересчитываем данные ребёнка
                        val updatedChild = currentState.child.copy(
                            consumedThisMonth = (currentState.child.consumedThisMonth - response.deletedDrink.amountMl).coerceAtLeast(0),
                            remaining = currentState.child.remaining + response.deletedDrink.amountMl
                        )

                        _uiState.value = ChildDetailUiState.Success(
                            child = updatedChild,
                            history = updatedHistory
                        )
                    }
                    _message.value = "Запись удалена"
                    _hasChanges.value = true
                }
                .onFailure { error ->
                    _message.value = "Ошибка удаления: ${error.message}"
                }
        }
    }
    
    /**
     * Загрузить фото ребёнка
     */
    /**
     * Обработать и загрузить фото (в фоне)
     * Эффективно обрабатывает изображения любого размера без перегрузки памяти
     */
    fun processAndUploadPhoto(uri: android.net.Uri, context: android.content.Context) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _isUploadingPhoto.value = true
            try {
                // Шаг 1: Читаем размеры изображения БЕЗ загрузки в память
                val bounds = context.contentResolver.openInputStream(uri)?.use { stream ->
                    val opts = android.graphics.BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    android.graphics.BitmapFactory.decodeStream(stream, null, opts)
                    opts
                }
                
                if (bounds == null || bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                    _message.value = "Не удалось прочитать изображение"
                    return@launch
                }
                
                // Шаг 2: Вычисляем оптимальный inSampleSize для уменьшения изображения
                val targetSize = 1024
                val sampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, targetSize, targetSize)
                
                // Шаг 3: Загружаем уменьшенное изображение
                val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                    val opts = android.graphics.BitmapFactory.Options().apply {
                        inSampleSize = sampleSize
                        inJustDecodeBounds = false
                    }
                    android.graphics.BitmapFactory.decodeStream(stream, null, opts)
                }
                
                if (bitmap == null) {
                    _message.value = "Ошибка декодирования изображения"
                    return@launch
                }
                
                // Шаг 4: Сжимаем в JPEG
                val jpegBytes = java.io.ByteArrayOutputStream().use { out ->
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, out)
                    bitmap.recycle() // Освобождаем память
                    out.toByteArray()
                }
                
                // Шаг 5: Загружаем на сервер
                val fileName = "photo_${System.currentTimeMillis()}.jpg"
                repository.uploadPhoto(childId, jpegBytes, fileName)
                    .onSuccess { response ->
                        _message.value = response.message
                        _hasChanges.value = true
                        loadChildData()
                    }
                    .onFailure { error ->
                        _message.value = "Ошибка загрузки фото: ${error.message}"
                    }
                    
            } catch (e: Exception) {
                _message.value = "Ошибка: ${e.message}"
            } finally {
                _isUploadingPhoto.value = false
            }
        }
    }
    
    /**
     * Вычисляет оптимальный inSampleSize для уменьшения изображения
     */
    private fun calculateInSampleSize(width: Int, height: Int, reqWidth: Int, reqHeight: Int): Int {
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
    
    /**
     * Загрузить фото ребёнка (прямая загрузка байтов)
     */
    private suspend fun uploadPhoto(imageBytes: ByteArray, fileName: String) {
        // Этот метод оставим приватным или для внутреннего использования
         repository.uploadPhoto(childId, imageBytes, fileName)
            .onSuccess { response ->
                _message.value = response.message
                _hasChanges.value = true
                loadChildData()
            }
            .onFailure { error ->
                _message.value = "Ошибка загрузки фото: ${error.message}"
            }
    }



    /**
     * Очистить сообщение
     */
    fun clearMessage() {
        _message.value = null
    }

    /**
     * Обновить данные
     */
    fun refresh() {
        loadChildData()
    }
}
