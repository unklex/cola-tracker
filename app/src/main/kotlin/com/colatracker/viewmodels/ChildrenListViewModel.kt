package com.colatracker.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colatracker.data.models.Child
import com.colatracker.data.repository.ApiProvider
import com.colatracker.data.repository.ColaTrackerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * UI состояние для списка детей
 */
sealed class ChildrenUiState {
    object Loading : ChildrenUiState()
    data class Success(val children: List<Child>) : ChildrenUiState()
    data class Error(val message: String) : ChildrenUiState()
}

/**
 * ViewModel для главного экрана со списком детей.
 *
 * Зависимости с значениями по умолчанию: приложение создаёт её без аргументов
 * (`viewModel()`), а тесты подставляют фейковый репозиторий и часы.
 */
class ChildrenListViewModel(
    // Общий на процесс репозиторий (см. ApiProvider)
    private val repository: ColaTrackerRepository = ApiProvider.repository,
    private val clock: () -> Long = System::currentTimeMillis
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChildrenUiState>(ChildrenUiState.Loading)
    val uiState: StateFlow<ChildrenUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    /** Когда данные списка последний раз успешно получены с сервера (мс, null — ещё не было). */
    private val _lastUpdatedMillis = MutableStateFlow<Long?>(null)
    val lastUpdatedMillis: StateFlow<Long?> = _lastUpdatedMillis.asStateFlow()

    /** Разовые сообщения для снекбара (ошибки обновления, подтверждения). */
    private val _message = MutableStateFlow<UiMessage?>(null)
    val message: StateFlow<UiMessage?> = _message.asStateFlow()

    /** id ребёнка, для которого сейчас выполняется быстрое добавление. */
    private val _quickAddFor = MutableStateFlow<Int?>(null)
    val quickAddFor: StateFlow<Int?> = _quickAddFor.asStateFlow()

    init {
        loadChildren()
    }

    /**
     * Первичная загрузка (и повтор после ошибки) — со скелетонами.
     */
    fun loadChildren() {
        viewModelScope.launch {
            _uiState.value = ChildrenUiState.Loading
            fetchChildren()
        }
    }

    /**
     * Обновить данные, не убирая уже показанный список. Повторные вызовы во время
     * обновления (кнопка + жест «потянуть») игнорируются.
     */
    fun refresh() {
        if (_isRefreshing.value) return

        viewModelScope.launch {
            _isRefreshing.value = true
            fetchChildren()
            _isRefreshing.value = false
        }
    }

    /**
     * Быстрое добавление напитка прямо с главного экрана.
     *
     * [requestId] — ключ идемпотентности нажатия; «Повторить» в снекбаре передаёт тот же,
     * поэтому потерянный ответ не приведёт к дублю записи.
     */
    fun quickAddDrink(childId: Int, amountMl: Int, requestId: String = UUID.randomUUID().toString()) {
        if (_quickAddFor.value != null) return

        viewModelScope.launch {
            _quickAddFor.value = childId
            repository.addDrink(childId, amountMl, requestId)
                .onSuccess { response ->
                    // Сервер вернул обновлённого ребёнка — подменяем его точечно,
                    // без перезагрузки всего списка.
                    val current = _uiState.value
                    if (current is ChildrenUiState.Success) {
                        _uiState.value = ChildrenUiState.Success(
                            current.children.map { child ->
                                if (child.id == childId) response.child else child
                            }
                        )
                    }
                    _message.value = UiMessage("${response.child.name}: добавлено $amountMl мл")
                }
                .onFailure { error ->
                    _message.value = UiMessage(
                        text = error.userMessage(),
                        retry = if (error.isRetryable()) RetryAddDrink(childId, amountMl, requestId) else null
                    )
                }
            _quickAddFor.value = null
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    private suspend fun fetchChildren() {
        repository.getChildren()
            .onSuccess { children ->
                _uiState.value = ChildrenUiState.Success(children)
                _lastUpdatedMillis.value = clock()
            }
            .onFailure { error ->
                val current = _uiState.value
                if (current is ChildrenUiState.Success && current.children.isNotEmpty()) {
                    // Данные уже на экране — не сносим их из-за неудачного обновления
                    _message.value = UiMessage(error.userMessage())
                } else {
                    _uiState.value = ChildrenUiState.Error(error.userMessage())
                }
            }
    }
}
