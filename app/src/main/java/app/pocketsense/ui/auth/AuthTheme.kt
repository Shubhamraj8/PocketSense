package app.pocketsense.ui.auth

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import app.pocketsense.data.DarkModePref

data class AuthPalette(
    val background: androidx.compose.ui.graphics.Color,
    val surface: androidx.compose.ui.graphics.Color,
    val surfaceVariant: androidx.compose.ui.graphics.Color,
    val border: androidx.compose.ui.graphics.Color,
    val textPrimary: androidx.compose.ui.graphics.Color,
    val textSecondary: androidx.compose.ui.graphics.Color,
    val textTertiary: androidx.compose.ui.graphics.Color,
    val textMuted: androidx.compose.ui.graphics.Color,
    val accent: androidx.compose.ui.graphics.Color,
    val onAccent: androidx.compose.ui.graphics.Color,
)

val LocalAuthPalette = staticCompositionLocalOf<AuthPalette> {
    error("AuthPalette not provided")
}

@Composable
fun AuthAppTheme(
    darkMode: DarkModePref,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (darkMode) {
        DarkModePref.SYSTEM -> systemDark
        DarkModePref.LIGHT -> false
        DarkModePref.DARK -> true
    }
    val palette = if (darkTheme) {
        AuthPalette(
            background = AuthColors.darkBg,
            surface = AuthColors.darkSurface,
            surfaceVariant = AuthColors.darkSurfaceRaised,
            border = AuthColors.darkBorder,
            textPrimary = AuthColors.darkTextPrimary,
            textSecondary = AuthColors.darkTextSecondary,
            textTertiary = AuthColors.darkTextTertiary,
            textMuted = AuthColors.darkTextMuted,
            accent = AuthColors.darkTextPrimary,
            onAccent = AuthColors.darkBg,
        )
    } else {
        AuthPalette(
            background = AuthColors.lightBg,
            surface = AuthColors.lightSurface,
            surfaceVariant = AuthColors.lightSurfaceVariant,
            border = AuthColors.lightBorder,
            textPrimary = AuthColors.lightTextPrimary,
            textSecondary = AuthColors.lightTextSecondary,
            textTertiary = AuthColors.lightTextTertiary,
            textMuted = AuthColors.lightTextMuted,
            accent = AuthColors.lightTextPrimary,
            onAccent = AuthColors.lightSurface,
        )
    }

    val colorScheme: ColorScheme = if (darkTheme) {
        darkColorScheme(
            primary = palette.accent,
            onPrimary = palette.onAccent,
            background = palette.background,
            onBackground = palette.textPrimary,
            surface = palette.surface,
            onSurface = palette.textPrimary,
            surfaceVariant = palette.surfaceVariant,
            onSurfaceVariant = palette.textSecondary,
            outline = palette.border,
            error = AuthColors.errorText,
            onError = palette.onAccent,
        )
    } else {
        lightColorScheme(
            primary = palette.accent,
            onPrimary = palette.onAccent,
            background = palette.background,
            onBackground = palette.textPrimary,
            surface = palette.surface,
            onSurface = palette.textPrimary,
            surfaceVariant = palette.surfaceVariant,
            onSurfaceVariant = palette.textSecondary,
            outline = palette.border,
            error = AuthColors.errorText,
            onError = palette.onAccent,
        )
    }

    CompositionLocalProvider(LocalAuthPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AuthTypography,
            content = content,
        )
    }
}
