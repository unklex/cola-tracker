package com.colatracker.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Запись в истории потребления
 */
@Serializable
data class DrinkHistoryItem(
    @SerialName("id")
    val id: Int,

    @SerialName("child_id")
    val childId: Int,

    @SerialName("amount_ml")
    val amountMl: Int,

    @SerialName("timestamp")
    val timestamp: String
) {
    /**
     * Разбор метки времени.
     *
     * Бэкенд сейчас отдаёт «наивное» время без таймзоны (`datetime.now().isoformat()`),
     * поэтому оно трактуется как локальное. Если на сервере появится смещение
     * (`+03:00` или `Z`), время корректно переведётся в часовой пояс устройства.
     */
    fun dateTimeOrNull(): LocalDateTime? {
        return try {
            OffsetDateTime.parse(timestamp)
                .atZoneSameInstant(ZoneId.systemDefault())
                .toLocalDateTime()
        } catch (_: Exception) {
            try {
                LocalDateTime.parse(timestamp)
            } catch (_: Exception) {
                null
            }
        }
    }

    /** Дата записи в часовом поясе устройства. */
    fun dateOrNull(): LocalDate? = dateTimeOrNull()?.toLocalDate()

    /**
     * Форматированная дата и время для отображения.
     * Пример: "13.01.2025 14:30"
     */
    fun getFormattedDateTime(): String =
        dateTimeOrNull()?.format(DATE_TIME_FORMAT) ?: timestamp

    /**
     * Только дата для отображения.
     * Пример: "13.01.2025"
     */
    fun getFormattedDate(): String =
        dateTimeOrNull()?.format(DATE_FORMAT) ?: timestamp

    /**
     * Только время для отображения.
     * Пример: "14:30"
     */
    fun getFormattedTime(): String =
        dateTimeOrNull()?.format(TIME_FORMAT) ?: ""

    private companion object {
        // Locale.ROOT — чтобы цифры всегда были арабскими (0-9) независимо от локали устройства
        val DATE_TIME_FORMAT: DateTimeFormatter =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", Locale.ROOT)
        val DATE_FORMAT: DateTimeFormatter =
            DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.ROOT)
        val TIME_FORMAT: DateTimeFormatter =
            DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)
    }
}

/**
 * Записи одного дня с суммой выпитого.
 * [date] == null — записи, у которых не удалось разобрать метку времени.
 */
data class DrinkDayGroup(
    val date: LocalDate?,
    val items: List<DrinkHistoryItem>
) {
    val totalMl: Int
        get() = items.sumOf { it.amountMl }
}

/**
 * Группирует историю по дням, сохраняя порядок (новые дни сверху, как пришло с сервера).
 * Записи без разобранной даты уходят в конец отдельной группой.
 */
fun groupHistoryByDay(history: List<DrinkHistoryItem>): List<DrinkDayGroup> {
    val byDate = history.groupBy { it.dateOrNull() }
    val dated = byDate.filterKeys { it != null }.map { (date, items) -> DrinkDayGroup(date, items) }
    val undated = byDate[null]?.let { listOf(DrinkDayGroup(null, it)) }.orEmpty()
    return dated + undated
}

/**
 * Подпись дня для заголовка группы: «Сегодня», «Вчера» или дата.
 * [today] передаётся снаружи, чтобы функцию можно было проверить тестом.
 */
fun dayLabel(date: LocalDate?, today: LocalDate): String = when (date) {
    null -> "Без даты"
    today -> "Сегодня"
    today.minusDays(1) -> "Вчера"
    else -> date.format(DAY_LABEL_FORMAT)
}

private val DAY_LABEL_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.ROOT)
