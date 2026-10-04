package com.colatracker.data.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Тесты производных величин [Child].
 *
 * Главное, что проверяется: расход месяца и накопленный баланс — независимые
 * числа (`remaining != monthlyLimit - consumedThisMonth`).
 */
class ChildTest {

    private fun child(limit: Int, consumed: Int, remaining: Int) = Child(
        id = 1,
        name = "Тест",
        monthlyLimit = limit,
        consumedThisMonth = consumed,
        remaining = remaining
    )

    @Test
    fun `расход месяца считается от начисления, а не от баланса`() {
        // Баланс накоплен за несколько месяцев и намного больше начисления
        val c = child(limit = 1000, consumed = 500, remaining = 3000)

        assertEquals(0.5f, c.monthUsageRatio, 0.0001f)
        assertEquals(50, c.monthUsagePercent)
        assertEquals(0.5f, c.monthUsageProgress, 0.0001f)
        assertEquals(3.0f, c.balanceInMonths, 0.0001f)
        assertFalse(c.isOverLimit)
        assertFalse(c.isOverdrawn)
    }

    @Test
    fun `перерасход месяца не ограничивает процент, но ограничивает прогресс`() {
        val c = child(limit = 1000, consumed = 1500, remaining = 200)

        assertEquals(150, c.monthUsagePercent)
        assertEquals(1f, c.monthUsageProgress, 0.0001f)
        assertTrue(c.isOverLimit)
        assertEquals(500, c.overLimitMl)
        assertFalse(c.isOverdrawn)
    }

    @Test
    fun `баланс в минусе`() {
        val c = child(limit = 1000, consumed = 800, remaining = -250)

        assertTrue(c.isOverdrawn)
        assertEquals(-0.25f, c.balanceInMonths, 0.0001f)
        assertFalse(c.isOverLimit)
        assertEquals(0, c.overLimitMl)
    }

    @Test
    fun `нулевое начисление не приводит к делению на ноль`() {
        val c = child(limit = 0, consumed = 300, remaining = 100)

        assertEquals(0f, c.monthUsageRatio, 0.0001f)
        assertEquals(0, c.monthUsagePercent)
        assertEquals(0f, c.balanceInMonths, 0.0001f)
        assertTrue(c.isOverLimit)
        assertEquals(300, c.overLimitMl)
    }

    @Test
    fun `ровно израсходованное начисление — ещё не перерасход`() {
        val c = child(limit = 1000, consumed = 1000, remaining = 0)

        assertEquals(100, c.monthUsagePercent)
        assertFalse(c.isOverLimit)
        assertFalse(c.isOverdrawn)
    }
}
