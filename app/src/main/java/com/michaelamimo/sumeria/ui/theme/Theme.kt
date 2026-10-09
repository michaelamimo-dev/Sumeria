package com.michaelamimo.sumeria.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = SumeriaColors.ActionPrimary,
    secondary = SumeriaColors.Accent,
    tertiary = SumeriaColors.Muted,
    background = SumeriaColors.SurfaceWhite,
    surface = SumeriaColors.SurfaceWhite,
    onPrimary = SumeriaColors.SurfaceWhite,
    onBackground = SumeriaColors.TextPrimary,
    onSurface = SumeriaColors.TextPrimary
)

private val LightColorScheme = lightColorScheme(
    primary = SumeriaColors.ActionPrimary,
    secondary = SumeriaColors.Accent,
    tertiary = SumeriaColors.Muted,
    background = SumeriaColors.SurfaceWhite,
    surface = SumeriaColors.SurfaceWhite,
    onPrimary = SumeriaColors.SurfaceWhite,
    onBackground = SumeriaColors.TextPrimary,
    onSurface = SumeriaColors.TextPrimary
)

@Composable
fun SumeriaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // false = White Icons (because our status bar background is dark green)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}