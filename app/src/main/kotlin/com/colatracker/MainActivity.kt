package com.colatracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.colatracker.data.models.Child
import com.colatracker.ui.screens.ChildDetailScreen
import com.colatracker.ui.screens.ChildrenListScreen
import com.colatracker.ui.theme.ColaTrackerTheme
import com.colatracker.viewmodels.ChildrenListViewModel
import com.colatracker.viewmodels.ChildrenUiState

/**
 * Главная Activity приложения
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Force Coil to use BitmapFactoryDecoder explicitly
        // This acts as a failsafe if the Application class config isn't picked up
        val imageLoader = coil.ImageLoader.Builder(this)
            .components {
                add(coil.decode.BitmapFactoryDecoder.Factory())
            }
            .build()
        coil.Coil.setImageLoader(imageLoader)

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
 * Навигация без NavHost для избежания AnimatedContent
 */
sealed class Screen {
    object ChildrenList : Screen()
    data class ChildDetail(val child: Child) : Screen()
}

/**
 * Основное приложение с простой навигацией
 */
@Composable
fun ColaTrackerApp() {
    // Текущий экран
    var currentScreen by remember { mutableStateOf<Screen>(Screen.ChildrenList) }

    // Общий ViewModel для списка детей
    val childrenListViewModel: ChildrenListViewModel = viewModel()

    // Флаг для обновления
    var shouldRefresh by remember { mutableStateOf(false) }

    // Обновляем после возврата с деталей
    LaunchedEffect(shouldRefresh) {
        if (shouldRefresh && currentScreen is Screen.ChildrenList) {
            childrenListViewModel.refresh()
            shouldRefresh = false
        }
    }

    when (val screen = currentScreen) {
        is Screen.ChildrenList -> {
            ChildrenListScreen(
                onChildClick = { child ->
                    currentScreen = Screen.ChildDetail(child)
                },
                viewModel = childrenListViewModel
            )
        }

        is Screen.ChildDetail -> {
            ChildDetailScreen(
                child = screen.child,
                onNavigateBack = { hasChanges ->
                    if (hasChanges) {
                        shouldRefresh = true
                    }
                    currentScreen = Screen.ChildrenList
                }
            )
        }
    }
}
