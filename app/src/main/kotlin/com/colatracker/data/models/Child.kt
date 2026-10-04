package com.colatracker.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Модель данных ребёнка.
 *
 * ВАЖНО про семантику полей (см. `check_and_update_limits` на бэкенде):
 * - [monthlyLimit] — это НЕ «цель», а ежемесячное НАЧИСЛЕНИЕ: 1-го числа
 *   это количество мл добавляется к балансу.
 * - [consumedThisMonth] — выпито с начала текущего месяца, обнуляется 1-го числа.
 * - [remaining] — НАКОПЛЕННЫЙ БАЛАНС («кола-метр»). Он не сгорает и переносится
 *   из месяца в месяц, поэтому может быть БОЛЬШЕ [monthlyLimit] и может уйти в МИНУС.
 *
 * Отсюда следует главное: `remaining != monthlyLimit - consumedThisMonth`.
 * Расход месяца и баланс — две независимые величины, и в UI их нельзя смешивать.
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
    // ===== Расход текущего месяца =====

    /** Доля израсходованного месячного начисления. Может быть > 1 при перерасходе. */
    val monthUsageRatio: Float
        get() = if (monthlyLimit > 0) consumedThisMonth.toFloat() / monthlyLimit else 0f

    /** Расход месяца в процентах (не ограничен сверху: 150% так и покажет 150). */
    val monthUsagePercent: Int
        get() = (monthUsageRatio * 100f).toInt()

    /** Значение для прогресс-баров: 0f..1f. */
    val monthUsageProgress: Float
        get() = monthUsageRatio.coerceIn(0f, 1f)

    /** Превышено ли месячное начисление. */
    val isOverLimit: Boolean
        get() = consumedThisMonth > monthlyLimit

    /** На сколько мл превышено месячное начисление (0, если превышения нет). */
    val overLimitMl: Int
        get() = (consumedThisMonth - monthlyLimit).coerceAtLeast(0)

    // ===== Накопленный баланс («кола-метр») =====

    /** Баланс ушёл в минус — выпито больше, чем накоплено. */
    val isOverdrawn: Boolean
        get() = remaining < 0

    /**
     * Баланс, выраженный в «месяцах начисления»:
     * 2.4f означает, что накопленного хватит примерно на 2.4 месяца.
     */
    val balanceInMonths: Float
        get() = if (monthlyLimit > 0) remaining.toFloat() / monthlyLimit else 0f
}
