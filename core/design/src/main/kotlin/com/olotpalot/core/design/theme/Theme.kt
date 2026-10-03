package com.olotpalot.core.design.theme

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import com.olotpalot.domain.model.CustomPalette
import com.olotpalot.domain.model.ThemeMode

object OPTheme {
    val colors: OPColors @Composable @ReadOnlyComposable get() = LocalOPColors.current
    val type: OPTypography @Composable @ReadOnlyComposable get() = LocalOPTypography.current
    val spacing: OPSpacing @Composable @ReadOnlyComposable get() = LocalOPSpacing.current
    val reducedMotion: Boolean @Composable @ReadOnlyComposable get() = LocalReducedMotion.current
}

/**
 * Theme is driven purely by [mode] + [custom], both read from persisted
 * preferences. Nothing here knows about navigation, and navigation never
 * reads theme state: the two can never knock each other over.
 */
@Composable
fun OlotPalotTheme(
    mode: ThemeMode,
    custom: CustomPalette? = null,
    reducedMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors = Palettes.forMode(mode, custom)
    val scheme = colors.toMaterial()
    CompositionLocalProvider(
        LocalOPColors provides colors,
        LocalOPTypography provides DefaultOPTypography,
        LocalOPSpacing provides OPSpacing(),
        LocalReducedMotion provides reducedMotion,
        LocalContentColor provides colors.ink,
        LocalIndication provides ripple(color = colors.ink),
        LocalTextSelectionColors provides TextSelectionColors(colors.accent, colors.accentSoft),
    ) {
        MaterialTheme(colorScheme = scheme, typography = materialTypography(DefaultOPTypography), content = content)
    }
}

private fun OPColors.toMaterial(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = accent,
        onPrimary = onAccent,
        primaryContainer = accentSoft,
        onPrimaryContainer = ink,
        secondary = inkMuted,
        onSecondary = background,
        secondaryContainer = surfaceRaised,
        onSecondaryContainer = ink,
        tertiary = accentAlt,
        background = background,
        onBackground = ink,
        surface = background,
        onSurface = ink,
        surfaceVariant = surface,
        onSurfaceVariant = inkMuted,
        surfaceContainer = surface,
        surfaceContainerLow = surface,
        surfaceContainerLowest = background,
        surfaceContainerHigh = surfaceRaised,
        surfaceContainerHighest = surfaceRaised,
        surfaceTint = accent,
        outline = rule,
        outlineVariant = rule,
        scrim = scrim,
        error = accent,
    )
}

private fun materialTypography(t: OPTypography) = Typography(
    displayLarge = t.display, displayMedium = t.display, displaySmall = t.headline,
    headlineLarge = t.headline, headlineMedium = t.headline, headlineSmall = t.title,
    titleLarge = t.title, titleMedium = t.titleSmall, titleSmall = t.label,
    bodyLarge = t.body, bodyMedium = t.body, bodySmall = t.bodySmall,
    labelLarge = t.label, labelMedium = t.caption, labelSmall = t.eyebrow,
)
