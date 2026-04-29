package app.pocketsense.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import app.pocketsense.data.DarkModePref

val AppLightColorScheme = lightColorScheme(
    primary = LightAccent,
    onPrimary = LightSurface,
    primaryContainer = LightSurfaceVariant,
    onPrimaryContainer = LightTextPrimary,
    inversePrimary = DarkAccent,
    secondary = LightTextSecondary,
    onSecondary = LightSurface,
    secondaryContainer = LightSurfaceVariant,
    onSecondaryContainer = LightTextPrimary,
    tertiary = InfoText,
    onTertiary = LightSurface,
    tertiaryContainer = InfoText.copy(alpha = 0.15f),
    onTertiaryContainer = LightTextPrimary,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    surfaceTint = Color.Transparent,
    inverseSurface = DarkSurface,
    inverseOnSurface = DarkTextPrimary,
    error = ErrorText,
    onError = LightSurface,
    errorContainer = ErrorText.copy(alpha = 0.15f),
    onErrorContainer = LightTextPrimary,
    outline = LightBorder,
    outlineVariant = LightBorder,
    scrim = Color.Black.copy(alpha = 0.45f),
)

val AppDarkColorScheme = darkColorScheme(
    primary = DarkAccent,
    onPrimary = DarkBackground,
    primaryContainer = DarkSurfaceRaised,
    onPrimaryContainer = DarkTextPrimary,
    inversePrimary = LightAccent,
    secondary = DarkTextSecondary,
    onSecondary = DarkBackground,
    secondaryContainer = DarkSurfaceRaised,
    onSecondaryContainer = DarkTextPrimary,
    tertiary = InfoText,
    onTertiary = DarkBackground,
    tertiaryContainer = InfoBgDark,
    onTertiaryContainer = DarkTextPrimary,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceRaised,
    onSurfaceVariant = DarkTextSecondary,
    surfaceTint = Color.Transparent,
    inverseSurface = LightSurface,
    inverseOnSurface = LightTextPrimary,
    error = ErrorText,
    onError = DarkTextPrimary,
    errorContainer = ErrorBgDark,
    onErrorContainer = DarkTextPrimary,
    outline = DarkBorder,
    outlineVariant = DarkBorder,
    scrim = Color.Black.copy(alpha = 0.55f),
)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = !darkTheme
        controller.isAppearanceLightNavigationBars = !darkTheme
    }

    MaterialTheme(
        colorScheme = if (darkTheme) AppDarkColorScheme else AppLightColorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}

@Composable
fun PocketSenseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    AppTheme(darkTheme = darkTheme, content = content)
}

@Composable
fun PocketSenseTheme(
    darkMode: DarkModePref = DarkModePref.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    @Suppress("UNUSED_VARIABLE")
    val ignoredDynamicColor = dynamicColor
    val darkTheme = when (darkMode) {
        DarkModePref.SYSTEM -> isSystemInDarkTheme()
        DarkModePref.LIGHT -> false
        DarkModePref.DARK -> true
    }
    AppTheme(darkTheme = darkTheme, content = content)
}
