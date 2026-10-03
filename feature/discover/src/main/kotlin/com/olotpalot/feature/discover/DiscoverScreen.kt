package com.olotpalot.feature.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.olotpalot.core.common.TimeFormat
import com.olotpalot.core.design.components.Artwork
import com.olotpalot.core.design.components.CompactSearchBar
import com.olotpalot.core.design.components.ContentRow
import com.olotpalot.core.design.components.DisplayText
import com.olotpalot.core.design.components.EmptyState
import com.olotpalot.core.design.components.Eyebrow
import com.olotpalot.core.design.components.LoadingBlocks
import com.olotpalot.core.design.components.PrimaryButton
import com.olotpalot.core.design.components.Rule
import com.olotpalot.core.design.components.SectionHeader
import com.olotpalot.core.design.components.TextAction
import com.olotpalot.core.design.components.TextToggle
import com.olotpalot.core.design.theme.OPShapes
import com.olotpalot.core.design.theme.OPTheme
import com.olotpalot.domain.model.ConsumptionMode
import com.olotpalot.domain.model.ContentItem
import com.olotpalot.domain.model.Language
import com.olotpalot.domain.model.MinRating
import com.olotpalot.domain.model.MinViews
import com.olotpalot.domain.model.SearchFilters
import com.olotpalot.domain.model.SortOption
import com.olotpalot.domain.model.Taxon
import com.olotpalot.domain.model.UploadWindow
import com.olotpalot.domain.model.VideoLength

@Composable
fun DiscoverScreen(onOpenContent: (String) -> Unit, viewModel: DiscoverViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    var focused by remember { mutableStateOf(false) }

    LaunchedEffect(state.focusSearch) {
        if (state.focusSearch) runCatching { focusRequester.requestFocus() }
    }

    val gutter = OPTheme.spacing.gutter
    Column(Modifier.fillMaxSize().background(OPTheme.colors.background)) {
        Spacer(Modifier.windowInsetsPadding(WindowInsets.statusBars))
        Column(Modifier.padding(horizontal = gutter).padding(top = 12.dp, bottom = 8.dp)) {
            CompactSearchBar(
                value = state.query.text,
                onValueChange = viewModel::onQueryText,
                onSearch = { keyboard?.hide(); viewModel.onSubmit() },
                onFilter = viewModel::openFilters,
                filterCount = state.query.filters.activeCount,
                focusRequester = focusRequester,
                onFocusChanged = { focused = it },
            )
            ActiveFilterLine(state, onClear = viewModel::clearFilters, onOpen = viewModel::openFilters)
        }

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = gutter, end = gutter, bottom = 24.dp),
        ) {
            if (state.isActive) {
                resultsItems(state, onOpenContent, onClear = viewModel::clearAll, onSort = viewModel::openFilters)
            } else {
                indexItems(
                    state, focused,
                    onRecent = viewModel::onRecent, onClearRecent = viewModel::clearRecent,
                    onTaxon = { viewModel.onFilters(SearchFilters(taxonIds = setOf(it.id))) },
                    onOpenContent = onOpenContent,
                )
            }
        }
    }

    if (state.filterSheetOpen) {
        FilterSheet(
            sort = state.query.sort,
            filters = state.query.filters,
            onDismiss = viewModel::closeFilters,
            onApply = { sort, filters -> viewModel.onSort(sort); viewModel.onFilters(filters); viewModel.closeFilters() },
            onReset = { viewModel.clearFilters(); viewModel.closeFilters() },
        )
    }
}

@Composable
private fun ActiveFilterLine(state: DiscoverUiState, onClear: () -> Unit, onOpen: () -> Unit) {
    val f = state.query.filters
    if (f.isEmpty && state.query.sort == SortOption.RELEVANCE) return
    val parts = buildList {
        if (state.query.sort != SortOption.RELEVANCE) add(state.query.sort.label)
        if (f.uploaded != UploadWindow.ANY) add(f.uploaded.label)
        if (f.videoLength != VideoLength.ANY) add(f.videoLength.label)
        if (f.minRating != MinRating.ANY) add("Rated ${f.minRating.label}")
        if (f.minViews != MinViews.ANY) add("${f.minViews.label} views")
        if (f.modes.isNotEmpty()) add(f.modes.joinToString(" / ") { it.label })
        f.language?.let { add(it.label) }
        addAll(state.activeTaxonNames)
    }
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            parts.joinToString("  ·  "),
            style = OPTheme.type.caption, color = OPTheme.colors.inkMuted,
            modifier = Modifier.weight(1f).clip(OPShapes.control).clickable(role = Role.Button, onClick = onOpen),
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(8.dp))
        TextAction("Clear", onClear, color = OPTheme.colors.inkMuted)
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.resultsItems(
    state: DiscoverUiState,
    onOpen: (String) -> Unit,
    onClear: () -> Unit,
    onSort: () -> Unit,
) {
    val results = state.results
    when {
        results == null || (state.searching && results.isEmpty()) -> item("loading") { LoadingBlocks(Modifier.padding(top = 16.dp)) }
        results.isEmpty() -> item("empty") {
            EmptyState("Nothing found", "Try another search or filter.", action = "Clear search", onAction = onClear)
        }
        else -> {
            item("count") {
                Row(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Eyebrow("${results.size} ${if (results.size == 1) "story" else "stories"}")
                    Spacer(Modifier.weight(1f))
                    Text(
                        "Sorted by ${state.query.sort.label.lowercase()}",
                        style = OPTheme.type.caption, color = OPTheme.colors.inkMuted,
                        modifier = Modifier.clip(OPShapes.control).clickable(role = Role.Button, onClick = onSort).padding(4.dp),
                    )
                }
            }
            items(results.size, key = { results[it].id }) { i ->
                val item = results[i]
                ContentRow(item, onClick = { onOpen(item.id) }, meta = resultMeta(item, state.query.sort), showRule = i < results.lastIndex)
            }
        }
    }
}

private fun resultMeta(item: ContentItem, sort: SortOption): String = when (sort) {
    SortOption.NEWEST, SortOption.OLDEST -> TimeFormat.relative(item.publishedAt)
    SortOption.HIGHEST_RATED -> if (item.stats.ratingCount > 0) "★ %.1f (%d)".format(item.stats.ratingAverage, item.stats.ratingCount) else "No ratings yet"
    SortOption.MOST_VIEWED -> "${TimeFormat.compactCount(item.stats.views)} views"
    SortOption.SHORTEST_VIDEO, SortOption.LONGEST_VIDEO -> "Watch · ${TimeFormat.duration(item.videoDurationMs)}"
    SortOption.RELEVANCE -> {
        val mode = item.primaryMode
        if (mode != null && item.durationFor(mode) > 0) "${mode.label} · ${TimeFormat.duration(item.durationFor(mode))}" else item.category.name
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.indexItems(
    state: DiscoverUiState,
    focused: Boolean,
    onRecent: (String) -> Unit,
    onClearRecent: () -> Unit,
    onTaxon: (Taxon) -> Unit,
    onOpenContent: (String) -> Unit,
) {
    if (focused && state.recentSearches.isNotEmpty()) {
        item("recent") {
            Column(Modifier.padding(top = 8.dp)) {
                SectionHeader("Recent", action = "Clear", onAction = onClearRecent)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    state.recentSearches.forEach { TextToggle(it, selected = false, onClick = { onRecent(it) }) }
                }
            }
        }
    }

    if (state.collections.isNotEmpty()) {
        item("collections-header") { SectionHeader("Collections", Modifier.padding(top = 20.dp)) }
        items(state.collections.size, key = { "col-" + state.collections[it].first.id }) { i ->
            val (collection, items) = state.collections[i]
            CollectionBlock(collection.title, collection.blurb, items, onOpenContent)
            if (i < state.collections.lastIndex) Rule(Modifier.padding(vertical = 4.dp))
        }
    }

    if (state.genres.isNotEmpty()) {
        item("genres") {
            Column(Modifier.padding(top = 28.dp)) {
                SectionHeader("Genres")
                val cols = state.genres.chunked((state.genres.size + 1) / 2)
                Row(Modifier.fillMaxWidth()) {
                    cols.forEach { col ->
                        Column(Modifier.weight(1f)) {
                            col.forEach { g ->
                                Text(
                                    g.name,
                                    style = OPTheme.type.title.copy(fontSize = 19.sp, lineHeight = 34.sp),
                                    color = OPTheme.colors.ink,
                                    modifier = Modifier.fillMaxWidth().clip(OPShapes.control).clickable(role = Role.Button) { onTaxon(g) }.padding(horizontal = 2.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (state.moods.isNotEmpty()) {
        item("moods") {
            Column(Modifier.padding(top = 24.dp)) {
                SectionHeader("Moods")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    state.moods.forEach { m -> TextToggle(m.name, selected = false, onClick = { onTaxon(m) }) }
                }
            }
        }
    }

    if (state.categories.isNotEmpty()) {
        item("categories") {
            Column(Modifier.padding(top = 20.dp)) {
                SectionHeader("Formats")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    state.categories.forEach { c -> TextToggle(c.name, selected = false, onClick = { onTaxon(c) }) }
                }
            }
        }
    }

    if (state.newest.isNotEmpty()) {
        item("newest-header") { SectionHeader("New this week", Modifier.padding(top = 28.dp)) }
        items(state.newest.size, key = { "new-" + state.newest[it].id }) { i ->
            val item = state.newest[i]
            ContentRow(item, onClick = { onOpenContent(item.id) }, meta = TimeFormat.relative(item.publishedAt), showRule = i < state.newest.lastIndex)
        }
    }
}

@Composable
private fun CollectionBlock(title: String, blurb: String, items: List<ContentItem>, onOpen: (String) -> Unit) {
    val c = OPTheme.colors
    Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            DisplayText(title, style = OPTheme.type.title)
            Spacer(Modifier.height(4.dp))
            Text(blurb, style = OPTheme.type.bodySmall, color = c.inkMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items.forEach { item ->
                    Text(
                        item.title,
                        style = OPTheme.type.caption, color = c.ink,
                        modifier = Modifier.clip(OPShapes.control).clickable(role = Role.Button) { onOpen(item.id) }.padding(vertical = 2.dp),
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        Spacer(Modifier.width(16.dp))
        Box(Modifier.width(96.dp).height(72.dp)) {
            items.take(3).forEachIndexed { i, item ->
                Artwork(
                    item.thumbnailUrl, null,
                    Modifier.offset(x = (i * 20).dp, y = (i * 4).dp).width(48.dp).height(64.dp)
                        .clickable(role = Role.Button) { onOpen(item.id) },
                    fallbackLetter = item.title,
                )
            }
        }
    }
}

@Composable
private fun FilterSheet(
    sort: SortOption,
    filters: SearchFilters,
    onDismiss: () -> Unit,
    onApply: (SortOption, SearchFilters) -> Unit,
    onReset: () -> Unit,
) {
    val c = OPTheme.colors
    var draftSort by remember { mutableStateOf(sort) }
    var draft by remember { mutableStateOf(filters) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = c.surface,
        contentColor = c.ink,
        shape = OPShapes.sheet,
        dragHandle = null,
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = OPTheme.spacing.gutter).padding(top = 20.dp, bottom = 20.dp)
                .windowInsetsPadding(WindowInsets.navigationBars),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DisplayText("Sort & filter", style = OPTheme.type.title)
                Spacer(Modifier.weight(1f))
                TextAction("Reset", onReset, color = c.inkMuted)
            }
            Spacer(Modifier.height(16.dp))

            FilterGroup("Sort by") {
                SortOption.entries.forEach { TextToggle(it.label, draftSort == it, onClick = { draftSort = it }) }
            }
            FilterGroup("Uploaded") {
                UploadWindow.entries.forEach { TextToggle(it.label, draft.uploaded == it, onClick = { draft = draft.copy(uploaded = it) }) }
            }
            FilterGroup("Video length") {
                VideoLength.entries.forEach { TextToggle(it.label, draft.videoLength == it, onClick = { draft = draft.copy(videoLength = it) }) }
            }
            FilterGroup("Rating") {
                MinRating.entries.forEach { TextToggle(it.label, draft.minRating == it, onClick = { draft = draft.copy(minRating = it) }) }
            }
            FilterGroup("Views") {
                MinViews.entries.forEach { TextToggle(it.label, draft.minViews == it, onClick = { draft = draft.copy(minViews = it) }) }
            }
            FilterGroup("Format") {
                ConsumptionMode.entries.forEach { mode ->
                    val on = mode in draft.modes
                    TextToggle(mode.label, on, onClick = { draft = draft.copy(modes = if (on) draft.modes - mode else draft.modes + mode) })
                }
            }
            FilterGroup("Language") {
                TextToggle("Any", draft.language == null, onClick = { draft = draft.copy(language = null) })
                Language.entries.forEach { TextToggle(it.nativeLabel, draft.language == it, onClick = { draft = draft.copy(language = it) }) }
            }

            Spacer(Modifier.height(20.dp))
            PrimaryButton("Show results", onClick = { onApply(draftSort, draft) }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun FilterGroup(label: String, content: @Composable FlowRowScopeMarker.() -> Unit) {
    Column(Modifier.padding(bottom = 14.dp)) {
        Eyebrow(label)
        Spacer(Modifier.height(4.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(2.dp)) { FlowRowScopeMarker.content() }
    }
}

/** Tiny marker so group content reads as a block; FlowRow scope itself is not needed. */
object FlowRowScopeMarker
