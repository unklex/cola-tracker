package com.colatracker.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Запрос на добавление записи о выпитом
 */
@Serializable
data class DrinkRequest(
    @SerialName("amount_ml")
    val amountMl: Int
)

/**
 * Ответ при добавлении записи о выпитом
 */
@Serializable
data class AddDrinkResponse(
    @SerialName("message")
    val message: String,
    
    @SerialName("child")
    val child: Child,
    
    @SerialName("drink")
    val drink: DrinkHistoryItem
)

/**
 * Ответ при удалении записи
 */
@Serializable
data class DeleteDrinkResponse(
    @SerialName("message")
    val message: String,
    
    @SerialName("deleted_drink")
    val deletedDrink: DrinkHistoryItem
)

/**
 * Общий ответ API с сообщением
 */
@Serializable
data class ApiResponse(
    @SerialName("message")
    val message: String,
    
    @SerialName("version")
    val version: String? = null
)

/**
 * Ответ с ошибкой
 */
@Serializable
data class ErrorResponse(
    @SerialName("detail")
    val detail: String
)

/**
 * Ответ при загрузке фото
 * Backend возвращает: {"message": "...", "photo_url": "...", "child_id": 1}
 */
@Serializable
data class UploadPhotoResponse(
    @SerialName("message")
    val message: String,

    @SerialName("photo_url")
    val photoUrl: String? = null,

    @SerialName("child_id")
    val childId: Int? = null
)
