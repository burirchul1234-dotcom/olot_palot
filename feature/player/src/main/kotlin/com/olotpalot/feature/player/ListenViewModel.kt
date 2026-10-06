package com.olotpalot.feature.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.navigation.toRoute
import com.olotpalot.core.navigation.ListenRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** Thin binding: the Listen screen is a window onto the app-wide [PlaybackController]. */
@HiltViewModel
class ListenViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    val controller: PlaybackController,
) : ViewModel() {
    val contentId: String = savedStateHandle.toRoute<ListenRoute>().contentId
    val state: StateFlow<PlaybackState> = controller.state

    init { controller.load(contentId) }

    fun togglePlay() = controller.togglePlay()
    fun seekTo(fraction: Float) = controller.seekTo(fraction)
    fun skip(ms: Long) = controller.skip(ms)
    fun next() = controller.next()
    fun previous() = controller.previous()
    fun goTo(index: Int) = controller.goTo(index)
    fun setSpeed(speed: Float) = controller.setSpeed(speed)
    fun setSleepTimer(minutes: Int?) = controller.setSleepTimer(minutes)
    fun retry() = controller.retry()
}
