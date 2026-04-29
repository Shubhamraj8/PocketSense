package app.pocketsense.ui.auth

import androidx.compose.ui.graphics.Color

enum class PasswordStrengthTier(val filledSegments: Int, val label: String) {
    NONE(0, ""),
    WEAK(1, "Weak"),
    FAIR(2, "Fair"),
    GOOD(3, "Good"),
    STRONG(4, "Strong"),
}

fun passwordStrengthTier(password: String): PasswordStrengthTier {
    if (password.isEmpty()) return PasswordStrengthTier.NONE
    var score = 0
    if (password.length >= 8) score++
    if (password.any { it.isUpperCase() }) score++
    if (password.any { it.isDigit() }) score++
    if (password.length >= 14 || password.any { !it.isLetterOrDigit() }) score++
    return when {
        score <= 1 -> PasswordStrengthTier.WEAK
        score == 2 -> PasswordStrengthTier.FAIR
        score == 3 -> PasswordStrengthTier.GOOD
        else -> PasswordStrengthTier.STRONG
    }
}

fun segmentColor(index: Int, tier: PasswordStrengthTier, inactive: Color): Color {
    val active = index < tier.filledSegments
    if (!active) return inactive
    return when (tier) {
        PasswordStrengthTier.NONE -> inactive
        PasswordStrengthTier.WEAK -> AuthColors.errorText
        PasswordStrengthTier.FAIR -> AuthColors.amberFair
        PasswordStrengthTier.GOOD -> AuthColors.yellowGood
        PasswordStrengthTier.STRONG -> AuthColors.greenStrong
    }
}
