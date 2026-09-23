package com.onewheel.ridetracker.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Palette mirrors the web Ride Telemetry tracker: warm off-white / graphite
// surfaces, a teal accent for the XRV board and a burnt-orange accent for X7.
val LightBg = Color(0xFFF4F5F3)
val LightSurface = Color(0xFFFFFFFF)
val LightSurface2 = Color(0xFFECEEEC)
val LightBorder = Color(0xFFD7DBD6)
val LightText = Color(0xFF171A18)
val LightTextMuted = Color(0xFF5B625C)
val LightTextFaint = Color(0xFF8A9089)

val DarkBg = Color(0xFF14170F)
val DarkSurface = Color(0xFF1B1F17)
val DarkSurface2 = Color(0xFF22271D)
val DarkBorder = Color(0xFF333A2C)
val DarkText = Color(0xFFEEF0E8)
val DarkTextMuted = Color(0xFFA6AC9D)
val DarkTextFaint = Color(0xFF767D6D)

val AccentXrvLight = Color(0xFF0D7D78)
val AccentX7Light = Color(0xFFB5591A)
val AccentXrvDark = Color(0xFF4FD6C9)
val AccentX7Dark = Color(0xFFF2A35C)

val BadLight = Color(0xFFB3392F)
val BadDark = Color(0xFFFF7A6E)

data class RideColors(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val border: Color,
    val text: Color,
    val textMuted: Color,
    val textFaint: Color,
    val accentXrv: Color,
    val accentX7: Color,
    val bad: Color
)

val LocalRideColors = androidx.compose.runtime.staticCompositionLocalOf {
    RideColors(
        LightBg, LightSurface, LightSurface2, LightBorder, LightText, LightTextMuted, LightTextFaint,
        AccentXrvLight, AccentX7Light, BadLight
    )
}

@Composable
fun RideTelemetryTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val rideColors = if (dark) {
        RideColors(DarkBg, DarkSurface, DarkSurface2, DarkBorder, DarkText, DarkTextMuted, DarkTextFaint, AccentXrvDark, AccentX7Dark, BadDark)
    } else {
        RideColors(LightBg, LightSurface, LightSurface2, LightBorder, LightText, LightTextMuted, LightTextFaint, AccentXrvLight, AccentX7Light, BadLight)
    }

    val scheme = if (dark) {
        darkColorScheme(
            background = rideColors.bg,
            surface = rideColors.surface,
            onBackground = rideColors.text,
            onSurface = rideColors.text,
            primary = rideColors.text,
            onPrimary = rideColors.bg,
            surfaceVariant = rideColors.surface2,
            outline = rideColors.border
        )
    } else {
        lightColorScheme(
            background = rideColors.bg,
            surface = rideColors.surface,
            onBackground = rideColors.text,
            onSurface = rideColors.text,
            primary = rideColors.text,
            onPrimary = rideColors.bg,
            surfaceVariant = rideColors.surface2,
            outline = rideColors.border
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = rideColors.bg.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !dark
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(LocalRideColors provides rideColors) {
        MaterialTheme(colorScheme = scheme, typography = RideTypography, content = content)
    }
}
