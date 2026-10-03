package com.olotpalot.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.olotpalot.domain.model.CustomPalette
import com.olotpalot.domain.model.ThemeMode
import com.olotpalot.domain.model.UserPreferences
import com.olotpalot.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * All user preferences live here, on disk, from the first write. Theme is read
 * back on every cold start before the first frame — this is the fix for
 * "theme resets after restart" and it is covered by [PreferencesRepositoryImplTest].
 */
@Singleton
class PreferencesRepositoryImpl @Inject constructor(
    private val store: DataStore<Preferences>,
) : PreferencesRepository {

    private object Keys {
        val theme = stringPreferencesKey("theme")
        val fontScale = floatPreferencesKey("font_scale")
        val playbackSpeed = floatPreferencesKey("playback_speed")
        val reducedMotion = booleanPreferencesKey("reduced_motion")
        val displayName = stringPreferencesKey("display_name")
        val onboarded = booleanPreferencesKey("onboarded")
        val recentSearches = stringPreferencesKey("recent_searches")
        val customBg = intPreferencesKey("custom_bg")
        val customSurface = intPreferencesKey("custom_surface")
        val customInk = intPreferencesKey("custom_ink")
        val customAccent = intPreferencesKey("custom_accent")
        val customDark = booleanPreferencesKey("custom_dark")
    }

    override val preferences: Flow<UserPreferences> = store.data.map { p ->
        UserPreferences(
            theme = ThemeMode.fromName(p[Keys.theme]),
            customPalette = p[Keys.customBg]?.let { bg ->
                CustomPalette(
                    background = bg,
                    surface = p[Keys.customSurface] ?: bg,
                    ink = p[Keys.customInk] ?: 0xFF1B1915.toInt(),
                    accent = p[Keys.customAccent] ?: 0xFFB5432B.toInt(),
                    isDark = p[Keys.customDark] ?: false,
                )
            },
            fontScale = p[Keys.fontScale] ?: 1f,
            playbackSpeed = p[Keys.playbackSpeed] ?: 1f,
            reducedMotion = p[Keys.reducedMotion] ?: false,
            displayName = p[Keys.displayName] ?: "Reader",
            onboarded = p[Keys.onboarded] ?: false,
            recentSearches = p[Keys.recentSearches]?.split(SEP)?.filter { it.isNotBlank() } ?: emptyList(),
        )
    }

    override suspend fun setTheme(theme: ThemeMode) {
        store.edit { it[Keys.theme] = theme.name }
    }

    suspend fun setCustomPalette(palette: CustomPalette) {
        store.edit {
            it[Keys.customBg] = palette.background
            it[Keys.customSurface] = palette.surface
            it[Keys.customInk] = palette.ink
            it[Keys.customAccent] = palette.accent
            it[Keys.customDark] = palette.isDark
            it[Keys.theme] = ThemeMode.CUSTOM.name
        }
    }

    override suspend fun setFontScale(scale: Float) {
        store.edit { it[Keys.fontScale] = scale.coerceIn(0.85f, 1.6f) }
    }

    override suspend fun setPlaybackSpeed(speed: Float) {
        store.edit { it[Keys.playbackSpeed] = speed.coerceIn(0.5f, 3f) }
    }

    override suspend fun setReducedMotion(enabled: Boolean) {
        store.edit { it[Keys.reducedMotion] = enabled }
    }

    override suspend fun setDisplayName(name: String) {
        store.edit { it[Keys.displayName] = name.trim().take(40).ifBlank { "Reader" } }
    }

    override suspend fun addRecentSearch(text: String) {
        store.edit { p ->
            val existing = p[Keys.recentSearches]?.split(SEP)?.filter { it.isNotBlank() } ?: emptyList()
            val next = (listOf(text) + existing.filterNot { it.equals(text, ignoreCase = true) }).take(8)
            p[Keys.recentSearches] = next.joinToString(SEP)
        }
    }

    override suspend fun clearRecentSearches() {
        store.edit { it.remove(Keys.recentSearches) }
    }

    override suspend fun setOnboarded() {
        store.edit { it[Keys.onboarded] = true }
    }

    private companion object {
        const val SEP = "\u001F"
    }
}
