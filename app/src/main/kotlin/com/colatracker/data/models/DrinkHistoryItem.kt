package com.colatracker.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
     * Форматированная дата и время для отображения
     * Пример: "13.01.2025 14:30"
     */
    fun getFormattedDateTime(): String {
        return try {
            // Парсим ISO формат: "2025-01-13T14:30:00.123456"
            val parts = timestamp.split("T")
            val dateParts = parts[0].split("-")
            val timeParts = parts.getOrNull(1)?.split(":")
            
            val day = dateParts.getOrNull(2) ?: "00"
            val month = dateParts.getOrNull(1) ?: "00"
            val year = dateParts.getOrNull(0) ?: "0000"
            val hour = timeParts?.getOrNull(0) ?: "00"
            val minute = timeParts?.getOrNull(1) ?: "00"
            
            "$day.$month.$year $hour:$minute"
        } catch (e: Exception) {
            timestamp
        }
    }
    
    /**
     * Только дата для отображения
     * Пример: "13.01.2025"
     */
    fun getFormattedDate(): String {
        return try {
            val dateParts = timestamp.split("T")[0].split("-")
            val day = dateParts.getOrNull(2) ?: "00"
            val month = dateParts.getOrNull(1) ?: "00"
            val year = dateParts.getOrNull(0) ?: "0000"
            "$day.$month.$year"
        } catch (e: Exception) {
            timestamp
        }
    }
    
    /**
     * Только время для отображения
     * Пример: "14:30"
     */
    fun getFormattedTime(): String {
        return try {
            val timeParts = timestamp.split("T").getOrNull(1)?.split(":")
            val hour = timeParts?.getOrNull(0) ?: "00"
            val minute = timeParts?.getOrNull(1) ?: "00"
            "$hour:$minute"
        } catch (e: Exception) {
            ""
        }
    }
}
