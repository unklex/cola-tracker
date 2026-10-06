package com.colatracker.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class ChildUiTest {

    private val now = LocalDateTime.of(2026, 10, 6, 15, 30)

    @Test
    fun `обновлено сегодня — только время`() {
        assertEquals("Обновлено в 09:05", formatLastUpdated(LocalDateTime.of(2026, 10, 6, 9, 5), now))
    }

    @Test
    fun `обновлено раньше — с датой`() {
        assertEquals("Обновлено 05.10 в 23:59", formatLastUpdated(LocalDateTime.of(2026, 10, 5, 23, 59), now))
    }
}
