package app.pocketsense.data

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

private val INDIA: Locale = Locale.forLanguageTag("en-IN")

fun formatRupees(paise: Long): String {
    val rupees = BigDecimal.valueOf(paise).movePointLeft(2)
    val fmt = NumberFormat.getCurrencyInstance(INDIA)
    fmt.minimumFractionDigits = if (paise % 100L == 0L) 0 else 2
    fmt.maximumFractionDigits = 2
    return fmt.format(rupees)
}

fun parseRupeesToPaise(input: String): Long? {
    val cleaned = input.trim().removePrefix("₹").replace(",", "")
    if (cleaned.isEmpty()) return null
    val bd = cleaned.toBigDecimalOrNull() ?: return null
    if (bd < BigDecimal.ZERO) return null
    if (bd.scale() > 2) return null
    return bd.movePointRight(2).setScale(0).toLong()
}

fun paiseToRupeeText(paise: Long): String {
    val absPaise = if (paise < 0) -paise else paise
    val rupees = absPaise / 100
    val pa = (absPaise % 100).toInt()
    val sign = if (paise < 0) "-" else ""
    return if (pa == 0) "$sign$rupees" else "$sign$rupees.${pa.toString().padStart(2, '0')}"
}
