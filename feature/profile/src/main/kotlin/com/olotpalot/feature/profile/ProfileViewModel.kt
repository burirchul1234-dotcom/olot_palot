package com.olotpalot.feature.profile

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.palette.graphics.Palette
import com.olotpalot.core.common.AnalyticsEvent
import com.olotpalot.core.common.AnalyticsTracker
import com.olotpalot.core.common.AppDispatchers
import com.olotpalot.core.datastore.PreferencesRepositoryImpl
import com.olotpalot.domain.model.CustomPalette
import com.olotpalot.domain.model.LocalUser
import com.olotpalot.domain.model.ThemeMode
import com.olotpalot.domain.model.UserPreferences
import com.olotpalot.domain.model.UserStats
import com.olotpalot.domain.repository.EngagementRepository
import com.olotpalot.domain.repository.PreferencesRepository
import com.olotpalot.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ProfileUiState(
    val prefs: UserPreferences = UserPreferences(),
    val stats: UserStats = UserStats(),
    val user: LocalUser = LocalUser("pending", "Reader"),
    val loaded: Boolean = false,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: PreferencesRepository,
    engagement: EngagementRepository,
    users: UserRepository,
    private val analytics: AnalyticsTracker,
    private val dispatchers: AppDispatchers,
) : ViewModel() {

    val state: StateFlow<ProfileUiState> = combine(preferences.preferences, engagement.observeStats(), users.user) { p, s, u ->
        ProfileUiState(p, s, u, loaded = true)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    fun setTheme(mode: ThemeMode) = viewModelScope.launch {
        preferences.setTheme(mode)
        analytics.track(AnalyticsEvent.THEME_CHANGED, "theme" to mode.name)
    }

    fun setFontScale(scale: Float) = viewModelScope.launch { preferences.setFontScale(scale) }
    fun setReducedMotion(on: Boolean) = viewModelScope.launch { preferences.setReducedMotion(on) }
    fun setPlaybackSpeed(speed: Float) = viewModelScope.launch { preferences.setPlaybackSpeed(speed) }
    fun setDisplayName(name: String) = viewModelScope.launch { preferences.setDisplayName(name) }

    /** Custom theme: derive paper/ink/accent from a picture the user chose. */
    fun applyImageTheme(uri: Uri) = viewModelScope.launch {
        val palette = withContext(dispatchers.io) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                    BitmapFactory.decodeStream(stream, null, opts)
                }?.let { bmp -> Palette.from(bmp).maximumColorCount(24).generate() }
            }.getOrNull()
        } ?: return@launch
        val dominant = palette.dominantSwatch ?: return@launch
        val isDark = luminance(dominant.rgb) < 0.45f
        val background = if (isDark) (palette.darkMutedSwatch?.rgb ?: darken(dominant.rgb)) else (palette.lightMutedSwatch?.rgb ?: 0xFFF3EEE4.toInt())
        val surface = if (isDark) lighten(background, 0.06f) else lighten(background, 0.35f)
        val ink = if (isDark) 0xFFECE6DA.toInt() else 0xFF1B1915.toInt()
        val accent = palette.vibrantSwatch?.rgb ?: palette.darkVibrantSwatch?.rgb ?: palette.mutedSwatch?.rgb ?: 0xFFB5432B.toInt()
        (preferences as? PreferencesRepositoryImpl)?.setCustomPalette(CustomPalette(background, surface, ink, accent, isDark))
            ?: preferences.setTheme(ThemeMode.CUSTOM)
        analytics.track(AnalyticsEvent.THEME_CHANGED, "theme" to "CUSTOM")
    }

    private fun luminance(rgb: Int): Float {
        val r = (rgb shr 16 and 0xFF) / 255f; val g = (rgb shr 8 and 0xFF) / 255f; val b = (rgb and 0xFF) / 255f
        return 0.2126f * r + 0.7152f * g + 0.0722f * b
    }
    private fun darken(rgb: Int, f: Float = 0.6f): Int = mix(rgb, 0xFF000000.toInt(), f)
    private fun lighten(rgb: Int, f: Float): Int = mix(rgb, 0xFFFFFFFF.toInt(), f)
    private fun mix(a: Int, b: Int, t: Float): Int {
        fun ch(shift: Int) = (((a shr shift and 0xFF) * (1 - t)) + ((b shr shift and 0xFF) * t)).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
    }
}
