package com.olotpalot.feature.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.olotpalot.core.common.AnalyticsEvent
import com.olotpalot.core.common.AnalyticsTracker
import com.olotpalot.core.common.ApplicationScope
import com.olotpalot.core.common.Clock
import com.olotpalot.core.navigation.ReaderRoute
import com.olotpalot.domain.model.Bookmark
import com.olotpalot.domain.model.Chapter
import com.olotpalot.domain.model.ConsumptionMode
import com.olotpalot.domain.model.ContentItem
import com.olotpalot.domain.model.ReadingProgress
import com.olotpalot.domain.repository.BookmarkRepository
import com.olotpalot.domain.repository.ContentRepository
import com.olotpalot.domain.repository.PreferencesRepository
import com.olotpalot.domain.repository.ProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ReaderUiState(
    val item: ContentItem? = null,
    val chapters: List<Chapter> = emptyList(),
    val chapterIndex: Int = 0,
    /** Paragraph to scroll to when a chapter opens; consumed once by the UI. */
    val initialParagraph: Int = 0,
    val paragraphs: List<String> = emptyList(),
    val bookmarks: List<Bookmark> = emptyList(),
    val fontScale: Float = 1f,
    val overallFraction: Float = 0f,
    val ready: Boolean = false,
    val notFound: Boolean = false,
) {
    val chapter: Chapter? get() = chapters.getOrNull(chapterIndex)
    val hasNext: Boolean get() = chapterIndex < chapters.lastIndex
    val hasPrevious: Boolean get() = chapterIndex > 0
    fun isBookmarked(paragraph: Int) = bookmarks.any { it.chapterIndex == chapterIndex && it.paragraphIndex == paragraph }
}

@HiltViewModel
class ReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val content: ContentRepository,
    private val progress: ProgressRepository,
    private val bookmarks: BookmarkRepository,
    private val preferences: PreferencesRepository,
    private val analytics: AnalyticsTracker,
    private val appScope: ApplicationScope,
    private val clock: Clock,
) : ViewModel() {

    val contentId: String = savedStateHandle.toRoute<ReaderRoute>().contentId

    private val item = MutableStateFlow<ContentItem?>(null)
    private val chapters = MutableStateFlow<List<Chapter>>(emptyList())
    private val position = MutableStateFlow(Position(0, 0, 0f)) // chapter, paragraph, fraction within chapter
    private val initialParagraph = MutableStateFlow(0)
    private val notFound = MutableStateFlow(false)
    private var completed = false

    private data class Position(val chapter: Int, val paragraph: Int, val chapterFraction: Float)

    val state: StateFlow<ReaderUiState> = combine(
        item, chapters, position, initialParagraph,
        combine(bookmarks.observeForContent(contentId), preferences.preferences.map { it.fontScale }, notFound) { b, f, nf -> Triple(b, f, nf) },
    ) { it, ch, pos, initial, (marks, scale, missing) ->
        val chapter = ch.getOrNull(pos.chapter)
        ReaderUiState(
            item = it,
            chapters = ch,
            chapterIndex = pos.chapter,
            initialParagraph = initial,
            paragraphs = chapter?.let(::splitParagraphs) ?: emptyList(),
            bookmarks = marks,
            fontScale = scale,
            overallFraction = if (ch.isEmpty()) 0f else ((pos.chapter + pos.chapterFraction) / ch.size).coerceIn(0f, 1f),
            ready = it != null && ch.isNotEmpty(),
            notFound = missing,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReaderUiState())

    init {
        viewModelScope.launch {
            content.ensureSeeded()
            val loaded = content.getContent(contentId)
            val ch = content.getChapters(contentId)
            if (loaded == null || ch.isEmpty()) { notFound.value = true; return@launch }
            val saved = progress.reading(contentId)
            val startChapter = saved?.chapterIndex?.coerceIn(0, ch.lastIndex) ?: 0
            val startParagraph = if (saved != null && saved.chapterIndex == startChapter) saved.paragraphIndex else 0
            item.value = loaded
            chapters.value = ch
            initialParagraph.value = startParagraph
            position.value = Position(startChapter, startParagraph, 0f)
            analytics.track(AnalyticsEvent.READ_STARTED, "contentId" to contentId, "chapter" to startChapter)
        }
        // Persist position at most ~once a second while scrolling; final flush happens in onCleared.
        viewModelScope.launch {
            position.debounce(900).filterNotNull().collect { persist(it) }
        }
    }

    fun onScrolled(firstParagraph: Int, chapterFraction: Float) {
        val current = position.value
        if (current.paragraph == firstParagraph && kotlin.math.abs(current.chapterFraction - chapterFraction) < 0.01f) return
        position.value = current.copy(paragraph = firstParagraph, chapterFraction = chapterFraction)
        val st = state.value
        if (!completed && !st.hasNext && chapterFraction >= 0.97f) {
            completed = true
            viewModelScope.launch {
                progress.markCompleted(contentId, ConsumptionMode.READ)
                analytics.track(AnalyticsEvent.READ_COMPLETED, "contentId" to contentId)
            }
        }
    }

    fun goToChapter(index: Int, paragraph: Int = 0) {
        val ch = chapters.value
        if (index !in ch.indices) return
        initialParagraph.value = paragraph
        position.value = Position(index, paragraph, 0f)
        viewModelScope.launch { persist(position.value) }
    }

    fun nextChapter() = goToChapter(position.value.chapter + 1)
    fun previousChapter() = goToChapter(position.value.chapter - 1)

    fun toggleBookmark(paragraph: Int, text: String) {
        val st = state.value
        viewModelScope.launch {
            val existing = st.bookmarks.firstOrNull { it.chapterIndex == st.chapterIndex && it.paragraphIndex == paragraph }
            if (existing != null) {
                bookmarks.remove(existing.id)
                analytics.track(AnalyticsEvent.BOOKMARK_REMOVED, "contentId" to contentId)
            } else {
                bookmarks.add(contentId, st.chapterIndex, paragraph, excerpt = text.take(140))
                analytics.track(AnalyticsEvent.BOOKMARK_ADDED, "contentId" to contentId)
            }
        }
    }

    fun removeBookmark(id: Long) = viewModelScope.launch { bookmarks.remove(id) }

    fun setFontScale(scale: Float) = viewModelScope.launch { preferences.setFontScale(scale) }

    private suspend fun persist(p: Position) {
        val total = chapters.value.size
        if (total == 0) return
        progress.saveReading(
            ReadingProgress(
                contentId = contentId,
                chapterIndex = p.chapter,
                paragraphIndex = p.paragraph,
                fraction = ((p.chapter + p.chapterFraction) / total).coerceIn(0f, 1f),
                updatedAt = clock.now(),
            ),
        )
    }

    override fun onCleared() {
        // The screen is gone; make sure the last position lands even though viewModelScope is cancelled.
        val p = position.value
        if (chapters.value.isNotEmpty()) appScope.scope.launch { withContext(NonCancellable) { persist(p) } }
    }

    private fun splitParagraphs(chapter: Chapter): List<String> =
        chapter.body.split(Regex("\\n\\s*\\n")).map { it.trim().replace(Regex("\\s*\\n\\s*"), " ") }.filter { it.isNotEmpty() }
}
