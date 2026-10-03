package com.olotpalot.domain.model

/** Persisted appearance choice. Lives in DataStore, never only in Compose state. */
enum class ThemeMode(val label: String) {
    STANDARD("Standard"),
    LIGHT("Light"),
    DARK("Dark"),
    SUNRISE("Sunrise"),
    CUSTOM("Custom");

    companion object {
        fun fromName(name: String?): ThemeMode = entries.firstOrNull { it.name == name } ?: STANDARD
    }
}

/** ARGB ints derived from a user image; only meaningful when theme == CUSTOM. */
data class CustomPalette(
    val background: Int,
    val surface: Int,
    val ink: Int,
    val accent: Int,
    val isDark: Boolean,
)

data class UserPreferences(
    val theme: ThemeMode = ThemeMode.STANDARD,
    val customPalette: CustomPalette? = null,
    val fontScale: Float = 1f,
    val playbackSpeed: Float = 1f,
    val reducedMotion: Boolean = false,
    val displayName: String = "Reader",
    val onboarded: Boolean = false,
    val recentSearches: List<String> = emptyList(),
)
