package com.colatracker.viewmodels

/**
 * Повторяемое добавление напитка.
 *
 * [requestId] создаётся один раз на нажатие и при повторе передаётся тот же: если первый запрос
 * дошёл до сервера, а ответ потерялся (обрыв связи), сервер узнает ключ и вернёт уже созданную
 * запись вместо дубля.
 */
data class RetryAddDrink(
    val childId: Int,
    val amountMl: Int,
    val requestId: String
)

/**
 * Сообщение для снекбара. Если [retry] задан, в снекбаре появляется кнопка «Повторить».
 */
data class UiMessage(
    val text: String,
    val retry: RetryAddDrink? = null
)
