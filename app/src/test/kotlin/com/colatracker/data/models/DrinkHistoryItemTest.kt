package com.colatracker.data.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/**
 * Группировка истории по дням и подписи дней.
 */
class DrinkHistoryItemTest {

    private fun drink(id: Int, ml: Int, timestamp: String) =
        DrinkHistoryItem(id = id, childId = 1, amountMl = ml, timestamp = timestamp)

    private val today = LocalDate.of(2026, 10, 6)

    @Test
    fun `записи одного дня собираются в группу с суммой`() {
        val groups = groupHistoryByDay(
            listOf(
                drink(3, 330, "2026-10-06T18:00:00"),
                drink(2, 250, "2026-10-06T09:00:00"),
                drink(1, 100, "2026-10-05T20:00:00")
            )
        )

        assertEquals(2, groups.size)
        assertEquals(LocalDate.of(2026, 10, 6), groups[0].date)
        assertEquals(580, groups[0].totalMl)
        assertEquals(listOf(3, 2), groups[0].items.map { it.id })
        assertEquals(100, groups[1].totalMl)
    }

    @Test
    fun `порядок дней сохраняется как пришёл с сервера`() {
        val groups = groupHistoryByDay(
            listOf(
                drink(3, 1, "2026-10-06T10:00:00"),
                drink(2, 1, "2026-09-30T10:00:00"),
                drink(1, 1, "2026-09-01T10:00:00")
            )
        )

        assertEquals(
            listOf(LocalDate.of(2026, 10, 6), LocalDate.of(2026, 9, 30), LocalDate.of(2026, 9, 1)),
            groups.map { it.date }
        )
    }

    @Test
    fun `запись с неразобранной датой попадает в конец отдельной группой`() {
        val groups = groupHistoryByDay(
            listOf(
                drink(2, 100, "не дата"),
                drink(1, 200, "2026-10-06T10:00:00")
            )
        )

        assertEquals(2, groups.size)
        assertEquals(LocalDate.of(2026, 10, 6), groups[0].date)
        assertNull(groups[1].date)
        assertEquals(100, groups[1].totalMl)
    }

    @Test
    fun `пустая история — пустой список групп`() {
        assertEquals(emptyList<DrinkDayGroup>(), groupHistoryByDay(emptyList()))
    }

    @Test
    fun `подписи дней`() {
        assertEquals("Сегодня", dayLabel(today, today))
        assertEquals("Вчера", dayLabel(today.minusDays(1), today))
        assertEquals("04.10.2026", dayLabel(today.minusDays(2), today))
        assertEquals("Без даты", dayLabel(null, today))
    }
}
