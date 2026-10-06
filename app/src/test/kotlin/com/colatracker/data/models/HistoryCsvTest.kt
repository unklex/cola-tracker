package com.colatracker.data.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryCsvTest {

    private fun drink(id: Int, ml: Int, ts: String) = DrinkHistoryItem(id, 1, ml, ts)

    @Test
    fun `csv начинается с BOM, заголовок и строки в порядке истории`() {
        val csv = historyToCsv(
            "Артем",
            listOf(drink(2, 330, "2026-10-06T18:01:00"), drink(1, 250, "2026-10-05T09:30:00"))
        )

        assertTrue(csv.startsWith(CSV_BOM))
        assertEquals(
            listOf(
                "Дата;Время;Объём (мл);Ребёнок",
                "06.10.2026;18:01;330;Артем",
                "05.10.2026;09:30;250;Артем",
                ""
            ),
            csv.removePrefix(CSV_BOM).split("\r\n")
        )
    }

    @Test
    fun `пустая история — только заголовок`() {
        assertEquals("Дата;Время;Объём (мл);Ребёнок\r\n", historyToCsv("Маша", emptyList()).removePrefix(CSV_BOM))
    }

    @Test
    fun `разделитель и кавычки в имени экранируются`() {
        assertEquals("\"Маша; \"\"Мышь\"\"\"", csvCell("Маша; \"Мышь\""))
        assertEquals("обычное", csvCell("обычное"))
    }

    @Test
    fun `значения, похожие на формулу, не исполняются в Excel`() {
        assertEquals("'=1+1", csvCell("=1+1"))
        assertEquals("'@SUM(A1)", csvCell("@SUM(A1)"))
        assertEquals("'-5", csvCell("-5"))
    }

    @Test
    fun `имя файла безопасно`() {
        assertEquals("cola-Артем-2026-10-06.csv", csvFileName("Артем", "2026-10-06"))
        assertEquals("cola-ab-2026-10-06.csv", csvFileName("a/b\\:*?", "2026-10-06"))
        assertEquals("cola-ребенок-2026-10-06.csv", csvFileName("///", "2026-10-06"))
    }
}
