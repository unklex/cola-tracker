package com.colatracker.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Модель данных ребёнка
 */
@Serializable
data class Child(
    @SerialName("id")
    val id: Int,
    
    @SerialName("name")
    val name: String,
    
    @SerialName("photo_url")
    val photoUrl: String? = null,
    
    @SerialName("monthly_limit")
    val monthlyLimit: Int,
    
    @SerialName("consumed_this_month")
    val consumedThisMonth: Int,
    
    @SerialName("remaining")
    val remaining: Int
) {
    /**
     * Прогресс потребления в процентах (0..100)
     */
    val consumptionProgress: Float
        get() = if (monthlyLimit > 0) {
            (consumedThisMonth.toFloat() / monthlyLimit.toFloat() * 100f).coerceIn(0f, 100f)
        } else {
            0f
        }
    
    /**
     * Полный URL фотографии
     */
    fun getFullPhotoUrl(baseUrl: String): String? {
        return photoUrl?.let { "$baseUrl/$it" }
    }
}
