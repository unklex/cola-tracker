package com.colatracker.viewmodels

/**
 * Текст ошибки для показа пользователю.
 *
 * `ApiException` уже формирует человекопонятное сообщение, поэтому здесь
 * остаётся только подстраховка на случай исключений без текста.
 */
internal fun Throwable.userMessage(): String =
    message?.takeIf { it.isNotBlank() } ?: "Неизвестная ошибка"

/**
 * Имеет ли смысл повторить запрос: сеть, таймауты, 5xx. Ошибки 4xx (неверный токен, ребёнок
 * не найден, плохая сумма) от повтора не пройдут.
 */
internal fun Throwable.isRetryable(): Boolean =
    (this as? com.colatracker.data.api.ApiException)?.retryable ?: false
