package com.olotpalot.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.olotpalot.domain.model.ContinueItem
import com.olotpalot.domain.model.UserPreferences
import com.olotpalot.domain.repository.PreferencesRepository
import com.olotpalot.domain.repository.ProgressRepository
import com.olotpalot.feature.player.PlaybackController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * App-wide state that is *not* navigation: persisted preferences (theme,
 * onboarding), the continue strip, and the shared audio player. None of
 * these know about the back stack.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val preferencesRepo: PreferencesRepository,
    progress: ProgressRepository,
    val playback: PlaybackController,
) : ViewModel() {

    /** null until the first DataStore read lands; the splash waits for it so the first frame has the right theme. */
    val preferences: StateFlow<UserPreferences?> = preferencesRepo.preferences
        .map<UserPreferences, UserPreferences?> { it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** null while unknown, then false → onboarding, true → app. */
    val onboarded: StateFlow<Boolean?> = preferences.map { it?.onboarded }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val dismissedKey = MutableStateFlow<String?>(null)

    val continueItem: StateFlow<ContinueItem?> = combine(progress.observeContinue(limit = 1), dismissedKey) { items, dismissed ->
        items.firstOrNull()?.takeIf { it.key() != dismissed }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        // Streak: one open per calendar day counts; consecutive days extend it.
        viewModelScope.launch { preferencesRepo.recordDailyOpen(LocalDate.now().toString()) }
    }

    fun completeOnboarding(moodIds: Set<String>) {
        viewModelScope.launch {
            preferencesRepo.setFavouriteMoods(moodIds)
            preferencesRepo.setOnboarded()
        }
    }

    /** Session-only: hides the strip until progress changes again. */
    fun dismissContinue(item: ContinueItem) { dismissedKey.value = item.key() }

    private fun ContinueItem.key() = "${content.id}:${mode.name}:${(fraction * 1000).toInt()}"
}
