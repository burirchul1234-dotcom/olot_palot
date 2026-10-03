package com.olotpalot.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.olotpalot.core.common.TimeFormat
import com.olotpalot.core.design.components.Artwork
import com.olotpalot.core.design.components.ContentRow
import com.olotpalot.core.design.components.DisplayText
import com.olotpalot.core.design.components.Eyebrow
import com.olotpalot.core.design.components.IconButtonPlain
import com.olotpalot.core.design.components.LoadingBlocks
import com.olotpalot.core.design.components.Rule
import com.olotpalot.core.design.components.SectionHeader
import com.olotpalot.core.design.theme.OPFonts
import com.olotpalot.core.design.theme.OPTheme
import com.olotpalot.core.navigation.LibrarySectionRoute
import com.olotpalot.domain.model.Bookmark
import com.olotpalot.domain.model.ConsumptionMode
import com.olotpalot.domain.model.ContentItem
import com.olotpalot.domain.model.HistoryKind
import com.olotpalot.domain.model.LibrarySection
import com.olotpalot.domain.model.LibrarySnapshot
import com.olotpalot.domain.usecase.ObserveLibraryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(observeLibrary: ObserveLibraryUseCase) : ViewModel() {
    val state: StateFlow<LibrarySnapshot?> = observeLibrary().map<LibrarySnapshot, LibrarySnapshot?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@HiltViewModel
class LibrarySectionViewModel @Inject constructor(savedStateHandle: SavedStateHandle, observeLibrary: ObserveLibraryUseCase) : ViewModel() {
    val section: LibrarySection = runCatching { LibrarySection.valueOf(savedStateHandle.toRoute<LibrarySectionRoute>().section) }.getOrDefault(LibrarySection.HISTORY)
    val state: StateFlow<LibrarySnapshot?> = observeLibrary().map<LibrarySnapshot, LibrarySnapshot?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

data class LibraryActions(
    val openContent: (String) -> Unit,
    val openMode: (String, ConsumptionMode) -> Unit,
    val openReaderAt: (String) -> Unit,
    val openSection: (LibrarySection) -> Unit,
)

private const val PREVIEW = 3

@Composable
fun LibraryScreen(actions: LibraryActions, viewModel: LibraryViewModel = hiltViewModel()) {
    val snapshot by viewModel.state.collectAsStateWithLifecycle()
    val c = OPTheme.colors
    val gutter = OPTheme.spacing.gutter
    LazyColumn(Modifier.fillMaxSize().background(c.background), contentPadding = PaddingValues(start = gutter, end = gutter, bottom = 24.dp)) {
        item("head") {
            Column(Modifier.windowInsetsPadding(WindowInsets.statusBars).padding(top = 14.dp, bottom = 8.dp)) {
                DisplayText("Library", style = OPTheme.type.headline)
                Text("Where you are, what you kept, what you finished", style = OPTheme.type.caption, color = c.inkMuted)
            }
        }
        val s = snapshot
        if (s == null) { item("loading") { LoadingBlocks() }; return@LazyColumn }
        LibrarySection.entries.forEach { section -> sectionItems(section, s, actions, preview = true) }
    }
}

@Composable
fun LibrarySectionScreen(onBack: () -> Unit, actions: LibraryActions, viewModel: LibrarySectionViewModel = hiltViewModel()) {
    val snapshot by viewModel.state.collectAsStateWithLifecycle()
    val c = OPTheme.colors
    val gutter = OPTheme.spacing.gutter
    Column(Modifier.fillMaxSize().background(c.background).windowInsetsPadding(WindowInsets.statusBars)) {
        Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = gutter, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButtonPlain(Icons.AutoMirrored.Outlined.ArrowBack, "Back", onBack)
            Column { Eyebrow("Library"); DisplayText(viewModel.section.title, style = OPTheme.type.title) }
        }
        Rule()
        val s = snapshot
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = gutter, end = gutter, top = 8.dp, bottom = 24.dp)) {
            if (s == null) { item("loading") { LoadingBlocks() }; return@LazyColumn }
            sectionItems(viewModel.section, s, actions, preview = false)
        }
    }
}

private fun LazyListScope.sectionItems(section: LibrarySection, s: LibrarySnapshot, actions: LibraryActions, preview: Boolean) {
    val key = section.name
    val total: Int
    val rows: List<@Composable (Boolean) -> Unit>

    when (section) {
        LibrarySection.CONTINUE -> {
            total = s.continueItems.size
            rows = s.continueItems.map { ci -> { last -> ContentRow(ci.content, onClick = { actions.openMode(ci.content.id, ci.mode) }, meta = "${ci.mode.label} · ${TimeFormat.duration(ci.remainingMs)} left", progress = ci.fraction, showRule = !last) } }
        }
        LibrarySection.SAVED -> { total = s.savedForLater.size; rows = contentRows(s.savedForLater, actions) }
        LibrarySection.FAVOURITES -> { total = s.favourites.size; rows = contentRows(s.favourites, actions) }
        LibrarySection.COMPLETED -> { total = s.completed.size; rows = contentRows(s.completed, actions) }
        LibrarySection.DOWNLOADS -> { total = s.downloaded.size; rows = contentRows(s.downloaded, actions) }
        LibrarySection.BOOKMARKS -> {
            total = s.bookmarks.size
            rows = s.bookmarks.map { (b, item) -> { last -> BookmarkRow(b, item, onClick = { actions.openReaderAt(item.id) }, showRule = !last) } }
        }
        LibrarySection.HISTORY -> {
            total = s.history.size
            rows = s.history.map { h ->
                { last ->
                    val verb = when (h.kind) { HistoryKind.OPENED -> "Opened"; HistoryKind.LISTENED -> "Listened"; HistoryKind.READ -> "Read"; HistoryKind.WATCHED -> "Watched"; HistoryKind.COMPLETED -> "Finished" }
                    ContentRow(h.content, onClick = { actions.openContent(h.content.id) }, meta = "$verb · ${TimeFormat.relative(h.at)}", showRule = !last)
                }
            }
        }
    }

    item("$key-header") {
        SectionHeader(
            if (total > 0) "${section.title}  ·  $total" else section.title,
            Modifier.padding(top = 22.dp),
            action = if (preview && total > PREVIEW) "All" else null,
            onAction = { actions.openSection(section) },
        )
    }
    if (total == 0) {
        item("$key-empty") {
            Column(Modifier.padding(top = 2.dp, bottom = 6.dp)) {
                Text(section.emptyTitle, style = OPTheme.type.label, color = OPTheme.colors.inkMuted)
                Text(section.emptyBody, style = OPTheme.type.caption, color = OPTheme.colors.inkFaint)
            }
        }
        return
    }
    val shown = if (preview) rows.take(PREVIEW) else rows
    items(shown.size, key = { "$key-$it" }) { i -> shown[i](i == shown.lastIndex) }
}

private fun contentRows(items: List<ContentItem>, actions: LibraryActions): List<@Composable (Boolean) -> Unit> =
    items.map { item -> { last -> ContentRow(item, onClick = { actions.openContent(item.id) }, showRule = !last) } }

@Composable
private fun BookmarkRow(b: Bookmark, item: ContentItem, onClick: () -> Unit, showRule: Boolean) {
    val c = OPTheme.colors
    Column(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick)) {
        Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Artwork(item.thumbnailUrl, null, Modifier.width(36.dp).height(48.dp), fallbackLetter = item.title)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "“${b.excerpt}”",
                    style = OPTheme.type.reader.copy(fontSize = 15.sp, lineHeight = 21.sp, fontStyle = FontStyle.Italic, fontFamily = OPFonts.serifFor(b.excerpt)),
                    color = c.ink, maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text("${item.title}  ·  Chapter ${b.chapterIndex + 1}", style = OPTheme.type.caption, color = c.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (showRule) Rule()
    }
}
