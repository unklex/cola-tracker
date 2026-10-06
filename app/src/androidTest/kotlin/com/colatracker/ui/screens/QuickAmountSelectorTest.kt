package com.colatracker.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.colatracker.ui.theme.ColaTrackerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI-тест выбора объёма. Главное — сценарий «нажать 250 мл»: в этот момент появляется
 * неопределённый LinearProgressIndicator, и при несовместимых версиях material3/animation
 * приложение падало с NoSuchMethodError.
 */
@RunWith(AndroidJUnit4::class)
class QuickAmountSelectorTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun tapping_250_shows_loading_indicator_without_crash() {
        val amounts = mutableListOf<Int>()
        var loading by mutableStateOf(false)

        rule.setContent {
            ColaTrackerTheme {
                QuickAmountSelector(
                    isLoading = loading,
                    onAmountSelected = {
                        amounts += it
                        loading = true // как во ViewModel: isAddingDrink = true
                    }
                )
            }
        }

        rule.onNodeWithText("250 мл").assertIsEnabled().performClick()
        rule.waitForIdle()

        assertEquals(listOf(250), amounts)
        rule.onNodeWithText("250 мл").assertIsNotEnabled()
        rule.onNodeWithText("330 мл").assertIsNotEnabled()
    }

    @Test
    fun tapping_330_reports_amount() {
        val amounts = mutableListOf<Int>()
        rule.setContent {
            ColaTrackerTheme {
                QuickAmountSelector(isLoading = false, onAmountSelected = { amounts += it })
            }
        }

        rule.onNodeWithText("330 мл").performClick()

        assertEquals(listOf(330), amounts)
    }

    @Test
    fun custom_amount_dialog_validates_input() {
        val amounts = mutableListOf<Int>()
        rule.setContent {
            ColaTrackerTheme {
                QuickAmountSelector(isLoading = false, onAmountSelected = { amounts += it })
            }
        }

        rule.onNodeWithText("Другое").performClick()
        rule.onNodeWithText("Добавить").performClick() // пустое поле
        rule.onNodeWithText("Введите число").assertExists()
        assertEquals(emptyList<Int>(), amounts)
    }
}
