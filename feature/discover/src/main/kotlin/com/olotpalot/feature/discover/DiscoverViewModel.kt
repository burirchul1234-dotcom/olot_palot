package com.olotpalot.feature.discover

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.olotpalot.core.common.AnalyticsEvent
import com.olotpalot.core.common.AnalyticsTracker
import com.olotpalot.core.navigation.DiscoverRoute
import com.olotpalot.domain.model.Collection
import com.olotpalot.domain.model.ContentItem
import com.olotpalot.domain.model.SearchFilters
import com.olotpalot.domain.model.SearchQuery
import com.olotpalot.domain.model.SortOption
import com.olotpalot.domain.model.Taxon
import com.olotpalot.domain.model.TaxonKind
import com.olotpalot.domain.repository.ContentRepository
import com.olotpalot.domain.repository.PreferencesRepository
import com.olotpalot.domain.usecase.SearchContentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DiscoverUiState(
    val query: SearchQuery = SearchQuery(),
    val results: List<ContentItem>? = null,
    val searching: Boolean = false,
    val recentSearches: List<String> = emptyList(),
    val categories: List<Taxon> = emptyList(),
    val genres: List<Taxon> = emptyList(),
    val moods: List<Taxon> = emptyList(),
    val collections: List<Pair<Collection, List<ContentItem>>> = emptyList(),
    val newest: List<ContentItem> = emptyList(),
    val focusSearch: Boolean = false,
    val filterSheetOpen: Boolean = false,
) {
    val isActive: Boolean get() = !query.isBlank
    val activeTaxonNames: List<String>
        get() = (categories + genres + moods).filter { it.id in query.filters.taxonIds }.map { it.name }
}

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val search: SearchContentUseCase,
    private val content: ContentRepository,
    private val preferences: PreferencesRepository,
    private val analytics: AnalyticsTracker,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<DiscoverRoute>()

    private val query = MutableStateFlow(
        SearchQuery(
            text = route.query.orEmpty(),
            filters = SearchFilters(taxonIds = setOfNotNull(route.taxonId)),
        ),
    )
    private val results = MutableStateFlow<List<ContentItem>?>(null)
    private val searching = MutableStateFlow(false)
    private val sheetOpen = MutableStateFlow(false)
    private val collections = MutableStateFlow<List<Collection>>(emptyList())
    private var searchJob: Job? = null

    private val indexFlow = combine(
        content.observeTaxa(TaxonKind.CATEGORY),
        content.observeTaxa(TaxonKind.GENRE),
        content.observeTaxa(TaxonKind.MOOD),
        content.observeAll(),
        collections,
    ) { cats, genres, moods, all, cols ->
        val byId = all.associateBy { it.id }
        Index(cats, genres, moods, cols.map { c -> c to c.contentIds.mapNotNull { byId[it] } }, all.sortedByDescending { it.publishedAt }.take(6))
    }

    private data class Index(
        val categories: List<Taxon>,
        val genres: List<Taxon>,
        val moods: List<Taxon>,
        val collections: List<Pair<Collection, List<ContentItem>>>,
        val newest: List<ContentItem>,
    )

    val state: StateFlow<DiscoverUiState> = combine(
        query, results, searching, sheetOpen,
        combine(indexFlow, preferences.preferences.map { it.recentSearches }) { i, r -> i to r },
    ) { q, res, busy, sheet, (index, recent) ->
        DiscoverUiState(
            query = q, results = res, searching = busy, recentSearches = recent,
            categories = index.categories, genres = index.genres, moods = index.moods,
            collections = index.collections, newest = index.newest,
            focusSearch = route.focusSearch, filterSheetOpen = sheet,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DiscoverUiState(query = query.value, focusSearch = route.focusSearch))

    init {
        viewModelScope.launch { collections.value = content.collections() }
        if (!query.value.isBlank) runSearch(remember = false)
    }

    fun onQueryText(text: String) {
        query.update { it.copy(text = text) }
        runSearch(debounce = true, remember = false)
    }

    fun onSubmit() = runSearch(remember = true)

    fun onRecent(text: String) {
        query.update { it.copy(text = text) }
        runSearch(remember = true)
    }

    fun clearRecent() = viewModelScope.launch { preferences.clearRecentSearches() }

    fun onSort(sort: SortOption) {
        query.update { it.copy(sort = sort) }
        runSearch()
    }

    fun onFilters(filters: SearchFilters) {
        query.update { it.copy(filters = filters) }
        analytics.track(AnalyticsEvent.FILTER_APPLIED, "active" to filters.activeCount)
        runSearch()
    }

    fun clearFilters() {
        query.update { it.copy(filters = SearchFilters.NONE, sort = SortOption.RELEVANCE) }
        runSearch()
    }

    fun clearAll() {
        query.value = SearchQuery()
        results.value = null
    }

    fun openFilters() { sheetOpen.value = true }
    fun closeFilters() { sheetOpen.value = false }

    private fun runSearch(debounce: Boolean = false, remember: Boolean = false) {
        searchJob?.cancel()
        val q = query.value
        if (q.isBlank) {
            results.value = null
            searching.value = false
            return
        }
        searchJob = viewModelScope.launch {
            if (debounce) delay(220)
            searching.value = true
            results.value = search(q, remember = remember)
            if (remember && q.text.isNotBlank()) analytics.track(AnalyticsEvent.SEARCH_PERFORMED, "query" to q.text, "results" to (results.value?.size ?: 0))
            searching.value = false
        }
    }
}
