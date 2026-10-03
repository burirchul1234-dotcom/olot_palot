package com.olotpalot.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.olotpalot.domain.model.ContinueItem
import com.olotpalot.domain.model.UserPreferences
import com.olotpalot.domain.repository.PreferencesRepository
import com.olotpalot.domain.repository.ProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Holds the two pieces of app-wide state that are *not* navigation:
 * persisted preferences (theme) and the "continue" strip. Neither knows
 * about the other, and neither knows about the back stack.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    preferences: PreferencesRepository,
    progress: ProgressRepository,
) : ViewModel() {

    /** null until the first DataStore read lands; the splash waits for it so the first frame has the right theme. */
    val preferences: StateFlow<UserPreferences?> = preferences.preferences
        .map<UserPreferences, UserPreferences?> { it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val dismissedKey = MutableStateFlow<String?>(null)

    val continueItem: StateFlow<ContinueItem?> = combine(progress.observeContinue(limit = 1), dismissedKey) { items, dismissed ->
        items.firstOrNull()?.takeIf { it.key() != dismissed }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Session-only: hides the strip until progress changes again. */
    fun dismissContinue(item: ContinueItem) { dismissedKey.value = item.key() }

    private fun ContinueItem.key() = "${content.id}:${mode.name}:${(fraction * 1000).toInt()}"
}
