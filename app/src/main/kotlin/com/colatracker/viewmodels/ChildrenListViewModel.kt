package com.colatracker.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colatracker.AppConfig
import com.colatracker.data.api.ColaTrackerApi
import com.colatracker.data.models.Child
import com.colatracker.data.repository.ColaTrackerRepository
import com.colatracker.data.repository.MockRepository
import com.colatracker.data.repository.RealRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI состояние для списка детей
 */
sealed class ChildrenUiState {
    object Loading : ChildrenUiState()
    data class Success(val children: List<Child>) : ChildrenUiState()
    data class Error(val message: String) : ChildrenUiState()
}

/**
 * ViewModel для главного экрана со списком детей
 */
class ChildrenListViewModel : ViewModel() {
    
    // Репозиторий (Mock или Real в зависимости от настроек)
    private val repository: ColaTrackerRepository = if (AppConfig.USE_MOCK_DATA) {
        MockRepository()
    } else {
        RealRepository(ColaTrackerApi())
    }
    
    // UI состояние. Стартуем с Success(empty) — не переключаем на Loading,
    // чтобы избежать смены Box→LazyColumn и layout crash при performTraversals.
    private val _uiState = MutableStateFlow<ChildrenUiState>(ChildrenUiState.Success(emptyList()))
    val uiState: StateFlow<ChildrenUiState> = _uiState.asStateFlow()
    
    init {
        loadChildren()
    }
    
    /**
     * Загрузить список детей
     */
    fun loadChildren() {
        viewModelScope.launch {
            repository.getChildren()
                .onSuccess { children ->
                    _uiState.value = ChildrenUiState.Success(children)
                }
                .onFailure { error ->
                    _uiState.value = ChildrenUiState.Error(
                        error.message ?: "Неизвестная ошибка"
                    )
                }
        }
    }
    
    /**
     * Обновить данные (pull-to-refresh)
     */
    fun refresh() {
        loadChildren()
    }
}
