package com.dapursappa.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class SappaColors(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val text: Color,
    val text2: Color,
    val text3: Color,
    val border: Color,
    val header: Color,
    val headerText: Color,
    val accent: Color,
    val accentSoft: Color,
    val accentText: Color,
    val onAccent: Color,
    val ok: Color,
    val okSoft: Color,
    val warn: Color,
    val warnSoft: Color,
)

val LightSappa = SappaColors(
    bg = Color(0xFFFFF8F0), surface = Color(0xFFFFFFFF), surface2 = Color(0xFFFBEFE3),
    text = Color(0xFF3B1F14), text2 = Color(0xFF7A5A4B), text3 = Color(0xFF8E7263),
    border = Color(0xFFEBD9CB), header = Color(0xFF5A2D1C), headerText = Color(0xFFFFF4E6),
    accent = Color(0xFFD93A58), accentSoft = Color(0xFFFBE1E7), accentText = Color(0xFFB02A47),
    onAccent = Color(0xFFFFFFFF), ok = Color(0xFF3F6B1E), okSoft = Color(0xFFE6EFD3),
    warn = Color(0xFF8A4F00), warnSoft = Color(0xFFFCE9C4),
)

val DarkSappa = SappaColors(
    bg = Color(0xFF1D1410), surface = Color(0xFF2A1D17), surface2 = Color(0xFF35261F),
    text = Color(0xFFFBEFE4), text2 = Color(0xFFCDB3A3), text3 = Color(0xFFA88E7E),
    border = Color(0xFF4B372D), header = Color(0xFF2F170E), headerText = Color(0xFFFFF1E2),
    accent = Color(0xFFF0637E), accentSoft = Color(0xFF4C2432), accentText = Color(0xFFFFA3B5),
    onAccent = Color(0xFF2B0F18), ok = Color(0xFFB5D681), okSoft = Color(0xFF2F3B1D),
    warn = Color(0xFFF4C873), warnSoft = Color(0xFF493818),
)

val LocalSappa = staticCompositionLocalOf { LightSappa }

object Sappa {
    val c: SappaColors
        @Composable get() = LocalSappa.current
}

/** mode: 0 = ikuti HP, 1 = terang, 2 = gelap */
@Composable
fun DapurSappaTheme(mode: Int, content: @Composable () -> Unit) {
    val dark = when (mode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    val c = if (dark) DarkSappa else LightSappa
    val scheme = if (dark) {
        darkColorScheme(
            primary = c.accent, onPrimary = c.onAccent,
            background = c.bg, onBackground = c.text,
            surface = c.surface, onSurface = c.text,
        )
    } else {
        lightColorScheme(
            primary = c.accent, onPrimary = c.onAccent,
            background = c.bg, onBackground = c.text,
            surface = c.surface, onSurface = c.text,
        )
    }
    CompositionLocalProvider(LocalSappa provides c) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
