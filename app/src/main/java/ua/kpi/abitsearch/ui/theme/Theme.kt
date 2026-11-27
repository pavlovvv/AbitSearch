package ua.kpi.abitsearch.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import ua.kpi.abitsearch.domain.model.ThemeMode

/** Токени дизайн-системи з розділу 3 (рис. 3.1). */
@Immutable
data class AbitColors(
    val bgTop: Color,
    val bgBottom: Color,
    val glass: Color,
    val glassStrong: Color,
    val outline: Color,
    val primary: Color,
    val primaryEnd: Color,
    val text: Color,
    val muted: Color,
    val faint: Color,
    val isDark: Boolean,
)

val DarkAbit = AbitColors(
    bgTop = Color(0xFF161229), bgBottom = Color(0xFF251B4A),
    glass = Color.White.copy(alpha = 0.075f), glassStrong = Color.White.copy(alpha = 0.12f),
    outline = Color.White.copy(alpha = 0.13f),
    primary = Color(0xFF9B8CFF), primaryEnd = Color(0xFF6E5CF6),
    text = Color(0xFFF4F2FF), muted = Color(0xFFE8E4FF).copy(alpha = 0.58f), faint = Color(0xFFE8E4FF).copy(alpha = 0.35f),
    isDark = true,
)

val LightAbit = AbitColors(
    bgTop = Color(0xFFF6F4FD), bgBottom = Color(0xFFECE8FB),
    glass = Color.White.copy(alpha = 0.62f), glassStrong = Color.White.copy(alpha = 0.85f),
    outline = Color(0xFF6E5CF6).copy(alpha = 0.12f),
    primary = Color(0xFF6E5CF6), primaryEnd = Color(0xFF5B48E8),
    text = Color(0xFF17142B), muted = Color(0xFF17142B).copy(alpha = 0.55f), faint = Color(0xFF17142B).copy(alpha = 0.35f),
    isDark = false,
)

val LocalAbitColors = staticCompositionLocalOf { DarkAbit }

object Abit {
    val colors: AbitColors @Composable get() = LocalAbitColors.current
}

/** Кольори графіків зарахування: бюджет і контракт. */
fun budgetColor(dark: Boolean): Color = if (dark) Color(0xFF34D399) else Color(0xFF059669)
fun contractColor(dark: Boolean): Color = if (dark) Color(0xFF9B8CFF) else Color(0xFF6E5CF6)
fun warningColor(dark: Boolean): Color = if (dark) Color(0xFFFBBF24) else Color(0xFFB45309)

@Composable
fun AbitTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val c = if (dark) DarkAbit else LightAbit
    val scheme = if (dark) {
        darkColorScheme(primary = c.primary, background = c.bgTop, surface = c.bgBottom, onSurface = c.text, onBackground = c.text)
    } else {
        lightColorScheme(primary = c.primary, background = c.bgTop, surface = c.bgTop, onSurface = c.text, onBackground = c.text)
    }
    CompositionLocalProvider(LocalAbitColors provides c) {
        MaterialTheme(colorScheme = scheme, typography = AbitTypography, content = content)
    }
}
