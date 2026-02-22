package com.tinaut1986.netpulse.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    secondary = PrimaryPurple,
    tertiary = Pink80,
    background = BackgroundDark,
    surface = CardBackground,
    onBackground = Color.White,
    onSurface = Color.White,
    onPrimary = Color.Black
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0056D2), // Darker blue for contrast in light mode
    secondary = Color(0xFF5E57FF),
    tertiary = Pink40,
    background = Color(0xFFF5F7FA),
    surface = Color.White,
    onBackground = Color(0xFF1A1A1A),
    onSurface = Color(0xFF1A1A1A),
    onSurfaceVariant = Color(0xFF49454F), // Standard M3 dark gray for labels
    surfaceVariant = Color(0xFFE7E0EC),
    onPrimary = Color.White
)

@Composable
fun NetPulseTheme(
    themeMode: String = "system",
    // Dynamic color disabled by default for better design consistency
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        themeMode == "system" && dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
