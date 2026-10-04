package com.colatracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.colatracker.ui.screens.ChildDetailScreen
import com.colatracker.ui.screens.ChildrenListScreen
import com.colatracker.ui.screens.SettingsScreen
import com.colatracker.ui.theme.ColaTrackerTheme
import com.colatracker.viewmodels.ChildrenListViewModel
import com.colatracker.viewmodels.ChildrenUiState

/**
 * Главная Activity приложения.
 *
 * ImageLoader для Coil настраивается один раз в [ColaTrackerApplication] —
 * здесь его переопределять не нужно, иначе при каждом пересоздании Activity
 * создавался бы новый загрузчик и терялся кэш картинок.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ColaTrackerTheme(darkTheme = isSystemInDarkTheme()) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ColaTrackerApp()
                }
            }
        }
    }
}

/**
 * Основное приложение с простой навигацией.
 *
 * Состояние навигации хранится в `rememberSaveable` (только id ребёнка, а не
 * объект целиком), поэтому поворот экрана больше не выбрасывает пользователя
 * обратно на список.
 */
@Composable
fun ColaTrackerApp() {
    val childrenListViewModel: ChildrenListViewModel = viewModel()
    val uiState by childrenListViewModel.uiState.collectAsStateWithLifecycle()

    var selectedChildId by rememberSaveable { mutableStateOf<Int?>(null) }
    var showSettings by rememberSaveable { mutableStateOf(false) }

    val children = (uiState as? ChildrenUiState.Success)?.children.orEmpty()
    val selectedChild = selectedChildId?.let { id -> children.find { it.id == id } }

    fun leaveDetail(hasChanges: Boolean) {
        selectedChildId = null
        if (hasChanges) childrenListViewModel.refresh()
    }

    // Системная кнопка «Назад» раньше просто закрывала приложение
    BackHandler(enabled = showSettings || selectedChildId != null) {
        when {
            showSettings -> showSettings = false
            else -> leaveDetail(hasChanges = true)
        }
    }

    // Ребёнка удалили на сервере, пока он был открыт — возвращаемся к списку
    LaunchedEffect(uiState, selectedChildId) {
        val id = selectedChildId
        if (id != null && uiState is ChildrenUiState.Success && children.none { it.id == id }) {
            selectedChildId = null
        }
    }

    when {
        showSettings -> {
            SettingsScreen(
                onNavigateBack = { showSettings = false },
                viewModel = childrenListViewModel
            )
        }

        selectedChildId != null -> {
            if (selectedChild != null) {
                ChildDetailScreen(
                    child = selectedChild,
                    onNavigateBack = { hasChanges -> leaveDetail(hasChanges) }
                )
            } else {
                // Список ещё грузится после восстановления состояния
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        else -> {
            ChildrenListScreen(
                onChildClick = { child -> selectedChildId = child.id },
                onSettingsClick = { showSettings = true },
                viewModel = childrenListViewModel
            )
        }
    }
}
