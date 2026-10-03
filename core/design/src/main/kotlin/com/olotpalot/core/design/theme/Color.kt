package com.olotpalot.core.design.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.olotpalot.domain.model.CustomPalette
import com.olotpalot.domain.model.ThemeMode

/**
 * The product palette. Deliberately small: paper, ink, one accent.
 * Hierarchy comes from typography and rules, not from many colours.
 */
@Immutable
data class OPColors(
    val background: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val ink: Color,
    val inkMuted: Color,
    val inkFaint: Color,
    val rule: Color,
    val accent: Color,
    val onAccent: Color,
    val accentSoft: Color,
    val accentAlt: Color,
    val scrim: Color,
    val isDark: Boolean,
)

object Palettes {
    val Standard = OPColors(
        background = Color(0xFFF3EEE4),
        surface = Color(0xFFFAF6EE),
        surfaceRaised = Color(0xFFFFFDF8),
        ink = Color(0xFF1B1915),
        inkMuted = Color(0xFF6B655B),
        inkFaint = Color(0xFFA39C90),
        rule = Color(0xFFDCD4C5),
        accent = Color(0xFFB5432B),
        onAccent = Color(0xFFFFF8F0),
        accentSoft = Color(0xFFF1D9D1),
        accentAlt = Color(0xFF7D6A3C),
        scrim = Color(0xCC14110D),
        isDark = false,
    )
    val Light = OPColors(
        background = Color(0xFFFFFFFF),
        surface = Color(0xFFF6F6F4),
        surfaceRaised = Color(0xFFFFFFFF),
        ink = Color(0xFF141414),
        inkMuted = Color(0xFF5F5F5C),
        inkFaint = Color(0xFF9C9C98),
        rule = Color(0xFFE2E2DE),
        accent = Color(0xFF1F4E8C),
        onAccent = Color(0xFFFFFFFF),
        accentSoft = Color(0xFFDCE6F4),
        accentAlt = Color(0xFF4F6B3A),
        scrim = Color(0xCC0E0E0E),
        isDark = false,
    )
    val Dark = OPColors(
        background = Color(0xFF121110),
        surface = Color(0xFF1B1917),
        surfaceRaised = Color(0xFF252220),
        ink = Color(0xFFECE6DA),
        inkMuted = Color(0xFFA59D91),
        inkFaint = Color(0xFF6F685E),
        rule = Color(0xFF302B27),
        accent = Color(0xFFE07A5F),
        onAccent = Color(0xFF1B1210),
        accentSoft = Color(0xFF3B2A24),
        accentAlt = Color(0xFFC9A961),
        scrim = Color(0xD9000000),
        isDark = true,
    )
    val Sunrise = OPColors(
        background = Color(0xFFFBF0E6),
        surface = Color(0xFFFFF7F0),
        surfaceRaised = Color(0xFFFFFCF8),
        ink = Color(0xFF2A1D18),
        inkMuted = Color(0xFF7B6257),
        inkFaint = Color(0xFFB29A8E),
        rule = Color(0xFFEBD6C6),
        accent = Color(0xFFD5542E),
        onAccent = Color(0xFFFFF6EF),
        accentSoft = Color(0xFFF7DDCB),
        accentAlt = Color(0xFFE4A44A),
        scrim = Color(0xCC2A1A12),
        isDark = false,
    )

    fun forMode(mode: ThemeMode, custom: CustomPalette?): OPColors = when (mode) {
        ThemeMode.STANDARD -> Standard
        ThemeMode.LIGHT -> Light
        ThemeMode.DARK -> Dark
        ThemeMode.SUNRISE -> Sunrise
        ThemeMode.CUSTOM -> custom?.let(::fromCustom) ?: Standard
    }

    /** A custom palette only supplies four anchors; the rest is derived so it stays coherent. */
    fun fromCustom(c: CustomPalette): OPColors {
        val bg = Color(c.background)
        val ink = Color(c.ink)
        val accent = Color(c.accent)
        val base = if (c.isDark) Dark else Standard
        return base.copy(
            background = bg,
            surface = Color(c.surface),
            surfaceRaised = blend(Color(c.surface), if (c.isDark) Color.White else Color.White, 0.06f),
            ink = ink,
            inkMuted = blend(ink, bg, 0.42f),
            inkFaint = blend(ink, bg, 0.65f),
            rule = blend(ink, bg, 0.86f),
            accent = accent,
            onAccent = if (luminance(accent) > 0.5f) Color(0xFF1B1915) else Color(0xFFFFF8F0),
            accentSoft = blend(accent, bg, 0.78f),
            isDark = c.isDark,
        )
    }

    private fun blend(a: Color, b: Color, t: Float) = Color(
        red = a.red + (b.red - a.red) * t,
        green = a.green + (b.green - a.green) * t,
        blue = a.blue + (b.blue - a.blue) * t,
        alpha = 1f,
    )

    private fun luminance(c: Color) = 0.2126f * c.red + 0.7152f * c.green + 0.0722f * c.blue
}

val LocalOPColors = staticCompositionLocalOf { Palettes.Standard }
