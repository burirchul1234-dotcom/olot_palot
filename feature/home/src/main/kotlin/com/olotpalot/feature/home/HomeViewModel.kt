package com.olotpalot.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.olotpalot.domain.model.HomeFeed
import com.olotpalot.domain.repository.ContentRepository
import com.olotpalot.domain.usecase.ObserveHomeFeedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Ready(val feed: HomeFeed) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    observeHomeFeed: ObserveHomeFeedUseCase,
    private val content: ContentRepository,
) : ViewModel() {

    val state: StateFlow<HomeUiState> = observeHomeFeed()
        .map<HomeFeed, HomeUiState> { feed ->
            // Before the catalog is seeded every list is empty; that's "loading", not "empty".
            if (feed.featured == null && feed.trending.isEmpty() && feed.recentlyAdded.isEmpty()) HomeUiState.Loading
            else HomeUiState.Ready(feed)
        }
        .catch { emit(HomeUiState.Error(it.message ?: "Couldn't load the front page.")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.Loading)

    fun retry() {
        viewModelScope.launch { content.refresh() }
    }
}
