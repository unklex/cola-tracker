package com.colatracker.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.colatracker.AppConfig
import com.colatracker.data.models.Child
import com.colatracker.ui.theme.ProgressGreen
import com.colatracker.ui.theme.ProgressRed
import com.colatracker.ui.theme.ProgressYellow
import java.util.Locale

/**
 * Полный URL фотографии ребёнка.
 *
 * [version] ломает кэш Coil: бэкенд хранит фото под постоянным именем
 * (`photos/child_1.jpg`), поэтому без версии после перезагрузки фото
 * показывалась бы старая картинка из кэша.
 */
fun childPhotoUrl(photoUrl: String?, version: Long = 0L): String? {
    if (photoUrl.isNullOrBlank()) return null
    val url = "${AppConfig.BASE_URL}/${photoUrl.trimStart('/')}"
    return if (version > 0L) "$url?v=$version" else url
}

/**
 * Цвет расхода за текущий месяц.
 *
 * Считается от `consumed / monthly_limit`, а НЕ от `remaining / monthly_limit`:
 * `remaining` — это накопительный баланс, он может быть больше месячного
 * начисления, из-за чего старая формула держала индикатор вечно зелёным.
 */
@Composable
fun monthUsageColor(child: Child): Color = monthUsageColor(child.monthUsageRatio)

@Composable
fun monthUsageColor(usageRatio: Float): Color = when {
    usageRatio <= 0.5f -> ProgressGreen
    usageRatio <= 0.8f -> ProgressYellow
    else -> ProgressRed
}

/**
 * Цвет накопленного баланса («кола-метра»).
 * Тревожно только если баланса меньше месячного начисления или он ушёл в минус.
 */
@Composable
fun balanceColor(child: Child): Color = when {
    child.isOverdrawn -> ProgressRed
    child.balanceInMonths >= 1f -> ProgressGreen
    child.balanceInMonths >= 0.3f -> ProgressYellow
    else -> ProgressRed
}

/**
 * Нужно ли показать предупреждение по ребёнку.
 */
fun needsAttention(child: Child): Boolean =
    child.isOverdrawn || child.monthUsageRatio > 0.8f

/**
 * Форматирование объёма: "750 мл" / "1.5 л".
 * Locale.ROOT — чтобы разделитель и цифры не зависели от локали устройства.
 */
fun formatVolume(ml: Int): String {
    val abs = kotlin.math.abs(ml)
    return if (abs >= 1000) {
        String.format(Locale.ROOT, "%.1f л", ml / 1000f)
    } else {
        "$ml мл"
    }
}

/**
 * Человеческое описание накопленного баланса в месяцах: "хватит на ~2.4 мес."
 */
fun formatBalanceInMonths(child: Child): String? {
    if (child.monthlyLimit <= 0 || child.remaining <= 0) return null
    return String.format(Locale.ROOT, "хватит примерно на %.1f мес.", child.balanceInMonths)
}
