package com.colatracker.viewmodels

/**
 * Текст ошибки для показа пользователю.
 *
 * `ApiException` уже формирует человекопонятное сообщение, поэтому здесь
 * остаётся только подстраховка на случай исключений без текста.
 */
internal fun Throwable.userMessage(): String =
    message?.takeIf { it.isNotBlank() } ?: "Неизвестная ошибка"
