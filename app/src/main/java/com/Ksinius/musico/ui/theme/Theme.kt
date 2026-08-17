package com.Ksinius.musico.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val MusicoDarkColorScheme = darkColorScheme(
    primary = PurplePrimary,
    onPrimary = PureBlack,
    primaryContainer = PurpleDark,
    onPrimaryContainer = PurpleAccent,
    secondary = PurpleSecondary,
    onSecondary = PureBlack,
    secondaryContainer = SurfaceVariantDark,
    onSecondaryContainer = PurpleAccent,
    tertiary = PurpleAccent,
    onTertiary = PureBlack,
    background = PureBlack,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondary,
    outline = PurpleDark
)

@Composable
fun MusicoTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = PureBlack.toArgb()
            window.navigationBarColor = PureBlack.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = MusicoDarkColorScheme,
        typography = Typography,
        content = content
    )
}