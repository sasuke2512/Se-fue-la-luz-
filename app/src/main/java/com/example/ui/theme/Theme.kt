package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.data.local.AppThemeMode

@Immutable
data class PowerSemanticColors(
    val powerOnAccent: Color,
    val powerOnContainer: Color,
    val powerOnContent: Color,
    val powerOffAccent: Color,
    val powerOffContainer: Color,
    val powerOffContent: Color,
    val amberGlow: Color
)

val LocalPowerSemanticColors = staticCompositionLocalOf {
    PowerSemanticColors(
        powerOnAccent = PowerOnGreenDark,
        powerOnContainer = PowerOnGreenContainerDark,
        powerOnContent = PowerOnGreenTextDark,
        powerOffAccent = PowerOffDangerDark,
        powerOffContainer = PowerOffContainerDark,
        powerOffContent = PowerOffTextDark,
        amberGlow = AmberGlowDark
    )
}

private val DarkColorScheme = darkColorScheme(
    primary = AmberPrimaryDark,
    onPrimary = OnAmberDark,
    primaryContainer = AmberContainerDark,
    onPrimaryContainer = OnAmberContainerDark,
    secondary = AmberGlowDark,
    onSecondary = OnAmberDark,
    secondaryContainer = CharcoalSurfaceHigh,
    onSecondaryContainer = TextPrimaryDark,
    tertiary = PowerOnGreenDark,
    onTertiary = OnAmberDark,
    background = CharcoalBackground,
    onBackground = TextPrimaryDark,
    surface = CharcoalSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = CharcoalSurfaceContainer,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainerLowest = CharcoalBackground,
    surfaceContainerLow = CharcoalSurfaceLow,
    surfaceContainer = CharcoalSurfaceContainer,
    surfaceContainerHigh = CharcoalSurfaceHigh,
    outline = CharcoalOutline,
    outlineVariant = CharcoalOutlineVariant,
    error = PowerOffDangerDark,
    onError = Color.White,
    errorContainer = PowerOffContainerDark,
    onErrorContainer = PowerOffTextDark
)

private val LightColorScheme = lightColorScheme(
    primary = AmberPrimaryLight,
    onPrimary = OnAmberLight,
    primaryContainer = AmberContainerLight,
    onPrimaryContainer = OnAmberContainerLight,
    secondary = Color(0xFFB45309),
    onSecondary = Color.White,
    secondaryContainer = IvorySurfaceContainer,
    onSecondaryContainer = TextPrimaryLight,
    tertiary = PowerOnGreenLight,
    onTertiary = Color.White,
    background = IvoryBackground,
    onBackground = TextPrimaryLight,
    surface = IvorySurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = IvorySurfaceContainer,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainerLowest = IvorySurface,
    surfaceContainerLow = IvorySurfaceLow,
    surfaceContainer = IvorySurfaceContainer,
    surfaceContainerHigh = IvorySurfaceHigh,
    outline = IvoryOutline,
    outlineVariant = IvoryOutlineVariant,
    error = PowerOffAccentLight,
    onError = Color.White,
    errorContainer = PowerOffContainerLight,
    onErrorContainer = PowerOffTextLight
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun SeFueLaLuzTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val semanticColors = if (darkTheme) {
        PowerSemanticColors(
            powerOnAccent = PowerOnGreenDark,
            powerOnContainer = PowerOnGreenContainerDark,
            powerOnContent = PowerOnGreenTextDark,
            powerOffAccent = PowerOffDangerDark,
            powerOffContainer = PowerOffContainerDark,
            powerOffContent = PowerOffTextDark,
            amberGlow = AmberGlowDark
        )
    } else {
        PowerSemanticColors(
            powerOnAccent = PowerOnGreenLight,
            powerOnContainer = PowerOnGreenContainerLight,
            powerOnContent = PowerOnGreenTextLight,
            powerOffAccent = PowerOffAccentLight,
            powerOffContainer = PowerOffContainerLight,
            powerOffContent = PowerOffTextLight,
            amberGlow = AmberPrimaryLight
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalPowerSemanticColors provides semanticColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}
