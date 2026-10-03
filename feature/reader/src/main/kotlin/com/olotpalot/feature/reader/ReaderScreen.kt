package com.olotpalot.feature.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.olotpalot.core.design.components.DisplayText
import com.olotpalot.core.design.components.EmptyState
import com.olotpalot.core.design.components.Eyebrow
import com.olotpalot.core.design.components.IconButtonPlain
import com.olotpalot.core.design.components.LoadingBlocks
import com.olotpalot.core.design.components.PrimaryButton
import com.olotpalot.core.design.components.ProgressHairline
import com.olotpalot.core.design.components.Rule
import com.olotpalot.core.design.components.SecondaryButton
import com.olotpalot.core.design.components.TextAction
import com.olotpalot.core.design.theme.OPFonts
import com.olotpalot.core.design.theme.OPMotion
import com.olotpalot.core.design.theme.OPShapes
import com.olotpalot.core.design.theme.OPTheme
import kotlinx.coroutines.flow.distinctUntilChanged

private const val HEADER_ITEMS = 1

@Composable
fun ReaderScreen(onBack: () -> Unit, viewModel: ReaderViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = OPTheme.colors
    var chromeVisible by rememberSaveable { mutableStateOf(true) }
    var showTypeSheet by remember { mutableStateOf(false) }
    var showBookmarks by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(c.background)) {
        when {
            state.notFound -> Column(Modifier.windowInsetsPadding(WindowInsets.statusBars).padding(OPTheme.spacing.gutter)) {
                IconButtonPlain(Icons.AutoMirrored.Outlined.ArrowBack, "Back", onBack)
                EmptyState("Nothing to read here", "This story has no text chapters.", action = "Go back", onAction = onBack)
            }
            !state.ready -> Column(Modifier.windowInsetsPadding(WindowInsets.statusBars).padding(OPTheme.spacing.gutter)) { LoadingBlocks() }
            else -> ReaderBody(state, viewModel, onToggleChrome = { chromeVisible = !chromeVisible })
        }

        // Top chrome
        AnimatedVisibility(
            visible = chromeVisible && state.ready,
            enter = fadeIn() + slideInVertically { -it / 2 },
            exit = fadeOut() + slideOutVertically { -it / 2 },
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Column(Modifier.fillMaxWidth().background(c.background)) {
                Row(
                    Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButtonPlain(Icons.AutoMirrored.Outlined.ArrowBack, "Back", onBack)
                    Column(Modifier.weight(1f)) {
                        Text(state.item?.title.orEmpty(), style = OPTheme.type.caption, color = c.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("Chapter ${state.chapterIndex + 1} of ${state.chapters.size}", style = OPTheme.type.label, color = c.ink, maxLines = 1)
                    }
                    IconButtonPlain(Icons.Outlined.Bookmarks, "Bookmarks (${state.bookmarks.size})", { showBookmarks = true }, tint = if (state.bookmarks.isNotEmpty()) c.accent else c.ink)
                    IconButtonPlain(Icons.Outlined.TextFields, "Text size", { showTypeSheet = true })
                }
                Rule()
            }
        }

        // Progress: always present, 2dp, bottom edge.
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
            AnimatedVisibility(visible = chromeVisible && state.ready, enter = fadeIn() + slideInVertically { it / 2 }, exit = fadeOut() + slideOutVertically { it / 2 }) {
                Column(Modifier.fillMaxWidth().background(c.background)) {
                    Rule()
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextAction("Previous", viewModel::previousChapter, color = if (state.hasPrevious) c.ink else c.inkFaint)
                        Spacer(Modifier.weight(1f))
                        Text("${(state.overallFraction * 100).toInt()}%", style = OPTheme.type.caption, color = c.inkMuted)
                        Spacer(Modifier.weight(1f))
                        TextAction("Next", viewModel::nextChapter, color = if (state.hasNext) c.ink else c.inkFaint)
                    }
                    Spacer(Modifier.windowInsetsPadding(WindowInsets.navigationBars))
                }
            }
            if (state.ready) ProgressHairline(state.overallFraction, trackColor = c.rule)
        }
    }

    if (showTypeSheet) {
        TypeSheet(state.fontScale, onChange = viewModel::setFontScale, onDismiss = { showTypeSheet = false })
    }
    if (showBookmarks) {
        BookmarksSheet(state, onJump = { viewModel.goToChapter(it.chapterIndex, it.paragraphIndex); showBookmarks = false }, onRemove = { viewModel.removeBookmark(it.id) }, onDismiss = { showBookmarks = false })
    }
}

@Composable
private fun ReaderBody(state: ReaderUiState, vm: ReaderViewModel, onToggleChrome: () -> Unit) {
    val c = OPTheme.colors
    val haptics = LocalHapticFeedback.current
    val listState = rememberLazyListState()
    val chapter = state.chapter ?: return
    val reader = OPTheme.type.reader
    val bodyStyle = reader.copy(
        fontSize = reader.fontSize * state.fontScale,
        lineHeight = reader.lineHeight * state.fontScale,
        fontFamily = OPFonts.serifFor(chapter.body),
    )

    // Jump to the saved paragraph when the chapter changes; the key is (chapter, requested paragraph).
    LaunchedEffect(state.chapterIndex, state.initialParagraph) {
        listState.scrollToItem(if (state.initialParagraph <= 0) 0 else state.initialParagraph + HEADER_ITEMS)
    }

    LaunchedEffect(listState, state.chapterIndex) {
        snapshotFlow {
            val info = listState.layoutInfo
            val total = info.totalItemsCount
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            val first = (listState.firstVisibleItemIndex - HEADER_ITEMS).coerceAtLeast(0)
            val fraction = if (total <= 1) 0f else (last.toFloat() / (total - 1)).coerceIn(0f, 1f)
            first to fraction
        }.distinctUntilChanged().collect { (first, fraction) -> vm.onScrolled(first, fraction) }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().clickable(interactionSource = null, indication = null, onClick = onToggleChrome),
        contentPadding = PaddingValues(top = 92.dp, bottom = 120.dp),
    ) {
        item("header") {
            Column(Modifier.fillMaxWidth().padding(horizontal = OPTheme.spacing.gutter).padding(bottom = 28.dp)) {
                Eyebrow("Chapter ${state.chapterIndex + 1}", color = c.accent)
                Spacer(Modifier.height(10.dp))
                DisplayText(chapter.title, style = OPTheme.type.readerTitle)
                Spacer(Modifier.height(16.dp))
                Box(Modifier.width(36.dp).height(2.dp).background(c.ink))
            }
        }
        items(state.paragraphs.size, key = { "p-${state.chapterIndex}-$it" }) { i ->
            val text = state.paragraphs[i]
            val marked = state.isBookmarked(i)
            Row(
                Modifier.fillMaxWidth()
                    .combinedClickable(
                        interactionSource = null, indication = null,
                        onClick = onToggleChrome,
                        onLongClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); vm.toggleBookmark(i, text) },
                        onLongClickLabel = if (marked) "Remove bookmark" else "Bookmark this paragraph",
                    )
                    .padding(bottom = 18.dp),
            ) {
                Box(Modifier.width(OPTheme.spacing.gutter).fillMaxHeight()) {
                    if (marked) Box(Modifier.width(3.dp).height(26.dp).padding(top = 6.dp).background(c.accent).align(Alignment.TopEnd))
                }
                Text(text, style = bodyStyle, color = c.ink, modifier = Modifier.weight(1f).widthIn(max = 640.dp))
                Spacer(Modifier.width(OPTheme.spacing.gutter))
            }
        }
        item("end") {
            Column(Modifier.fillMaxWidth().padding(horizontal = OPTheme.spacing.gutter).padding(top = 12.dp)) {
                Rule()
                Spacer(Modifier.height(20.dp))
                if (state.hasNext) {
                    val next = state.chapters[state.chapterIndex + 1]
                    Eyebrow("Next")
                    Spacer(Modifier.height(6.dp))
                    DisplayText(next.title, style = OPTheme.type.title)
                    Spacer(Modifier.height(14.dp))
                    PrimaryButton("Continue reading", vm::nextChapter)
                } else {
                    Eyebrow("The end", color = c.accent)
                    Spacer(Modifier.height(6.dp))
                    Text("You've finished ${state.item?.title.orEmpty()}.", style = OPTheme.type.body.copy(fontStyle = FontStyle.Italic), color = c.inkMuted)
                    Spacer(Modifier.height(14.dp))
                    SecondaryButton("Back to the beginning", { vm.goToChapter(0) })
                }
            }
        }
    }
}

@Composable
private fun TypeSheet(scale: Float, onChange: (Float) -> Unit, onDismiss: () -> Unit) {
    val c = OPTheme.colors
    var local by remember { mutableStateOf(scale) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = c.surface, contentColor = c.ink, shape = OPShapes.sheet, dragHandle = null) {
        Column(Modifier.padding(OPTheme.spacing.gutter).windowInsetsPadding(WindowInsets.navigationBars)) {
            Eyebrow("Reading text size")
            Spacer(Modifier.height(12.dp))
            Text(
                "The ferry stopped coming in the third year of the new timetable.",
                style = OPTheme.type.reader.copy(fontSize = OPTheme.type.reader.fontSize * local, lineHeight = OPTheme.type.reader.lineHeight * local),
                color = c.ink,
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("A", style = OPTheme.type.caption, color = c.inkMuted)
                Slider(
                    value = local, onValueChange = { local = it }, onValueChangeFinished = { onChange(local) },
                    valueRange = 0.85f..1.6f, steps = 5, modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                    colors = SliderDefaults.colors(thumbColor = c.ink, activeTrackColor = c.ink, inactiveTrackColor = c.rule, activeTickColor = c.background, inactiveTickColor = c.inkFaint),
                )
                Text("A", style = OPTheme.type.title, color = c.ink)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun BookmarksSheet(state: ReaderUiState, onJump: (com.olotpalot.domain.model.Bookmark) -> Unit, onRemove: (com.olotpalot.domain.model.Bookmark) -> Unit, onDismiss: () -> Unit) {
    val c = OPTheme.colors
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = c.surface, contentColor = c.ink, shape = OPShapes.sheet, dragHandle = null) {
        Column(Modifier.padding(OPTheme.spacing.gutter).windowInsetsPadding(WindowInsets.navigationBars)) {
            Eyebrow("Bookmarks")
            Spacer(Modifier.height(8.dp))
            if (state.bookmarks.isEmpty()) {
                Text("Press and hold a paragraph to bookmark it.", style = OPTheme.type.bodySmall, color = c.inkMuted)
                Spacer(Modifier.height(16.dp))
            } else {
                state.bookmarks.forEachIndexed { i, b ->
                    Column(Modifier.fillMaxWidth().clip(OPShapes.control).clickable(role = Role.Button) { onJump(b) }.padding(vertical = 10.dp)) {
                        Text(
                            "“${b.excerpt}”",
                            style = OPTheme.type.reader.copy(fontSize = 15.sp, lineHeight = 22.sp, fontStyle = FontStyle.Italic, fontFamily = OPFonts.serifFor(b.excerpt)),
                            color = c.ink, maxLines = 3, overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Chapter ${b.chapterIndex + 1}", style = OPTheme.type.caption, color = c.inkMuted)
                            Spacer(Modifier.weight(1f))
                            TextAction("Remove", { onRemove(b) }, color = c.inkMuted)
                        }
                    }
                    if (i < state.bookmarks.lastIndex) Rule()
                }
            }
        }
    }
}
