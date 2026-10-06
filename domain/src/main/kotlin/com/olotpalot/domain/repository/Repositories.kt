package com.olotpalot.domain.repository

import com.olotpalot.core.common.AppResult
import com.olotpalot.domain.model.Bookmark
import com.olotpalot.domain.model.Chapter
import com.olotpalot.domain.model.Collection
import com.olotpalot.domain.model.Comment
import com.olotpalot.domain.model.ConsumptionMode
import com.olotpalot.domain.model.ContentId
import com.olotpalot.domain.model.ContentItem
import com.olotpalot.domain.model.ContinueItem
import com.olotpalot.domain.model.HistoryEntry
import com.olotpalot.domain.model.HistoryKind
import com.olotpalot.domain.model.LocalUser
import com.olotpalot.domain.model.MediaProgress
import com.olotpalot.domain.model.ReadingProgress
import com.olotpalot.domain.model.SearchQuery
import com.olotpalot.domain.model.Taxon
import com.olotpalot.domain.model.TaxonKind
import com.olotpalot.domain.model.ThemeMode
import com.olotpalot.domain.model.UserContentState
import com.olotpalot.domain.model.UserPreferences
import com.olotpalot.domain.model.UserStats
import kotlinx.coroutines.flow.Flow

/**
 * Catalog access. Local cache (Room) is the single source the UI observes;
 * [refresh] pulls from whatever remote is bound (seed catalog today, Supabase later).
 */
interface ContentRepository {
    fun observeAll(): Flow<List<ContentItem>>
    fun observeContent(id: ContentId): Flow<ContentItem?>
    fun observeByIds(ids: List<ContentId>): Flow<List<ContentItem>>
    suspend fun getContent(id: ContentId): ContentItem?
    suspend fun getChapters(id: ContentId): List<Chapter>
    suspend fun search(query: SearchQuery): List<ContentItem>
    fun observeTaxa(kind: TaxonKind): Flow<List<Taxon>>
    suspend fun collections(): List<Collection>
    suspend fun refresh(): AppResult<Unit>
    suspend fun ensureSeeded()
}

/** Explicit user↔content relationships: like, favourite, saved-for-later, rating, view. */
interface EngagementRepository {
    fun observeState(id: ContentId): Flow<UserContentState>
    fun observeStates(): Flow<Map<ContentId, UserContentState>>
    suspend fun setLiked(id: ContentId, liked: Boolean)
    suspend fun setFavourite(id: ContentId, favourite: Boolean)
    suspend fun setSavedForLater(id: ContentId, saved: Boolean)
    suspend fun rate(id: ContentId, stars: Int)
    /** Counts at most one view per content per session window; returns true if a view was recorded. */
    suspend fun recordOpen(id: ContentId): Boolean
    suspend fun recordShare(id: ContentId)
    suspend fun recordHistory(id: ContentId, kind: HistoryKind)
    fun observeHistory(limit: Int = 50): Flow<List<HistoryEntry>>
    fun observeFavourites(): Flow<List<ContentItem>>
    fun observeSavedForLater(): Flow<List<ContentItem>>
    fun observeCompleted(): Flow<List<ContentItem>>
    fun observeStats(): Flow<UserStats>
}

interface ProgressRepository {
    fun observeContinue(limit: Int = 10): Flow<List<ContinueItem>>
    suspend fun saveReading(progress: ReadingProgress)
    suspend fun saveAudio(progress: MediaProgress)
    suspend fun saveVideo(progress: MediaProgress)
    suspend fun markCompleted(id: ContentId, mode: ConsumptionMode)
    suspend fun reading(id: ContentId): ReadingProgress?
    suspend fun audio(id: ContentId): MediaProgress?
    suspend fun video(id: ContentId): MediaProgress?
}

interface BookmarkRepository {
    fun observeForContent(id: ContentId): Flow<List<Bookmark>>
    fun observeAll(): Flow<List<Pair<Bookmark, ContentItem>>>
    suspend fun add(id: ContentId, chapterIndex: Int, paragraphIndex: Int, excerpt: String, note: String? = null): Bookmark
    suspend fun remove(bookmarkId: Long)
}

interface CommentRepository {
    fun observeComments(id: ContentId): Flow<List<Comment>>
    suspend fun loadPage(id: ContentId, offset: Int, limit: Int): AppResult<List<Comment>>
    suspend fun create(id: ContentId, body: String): AppResult<Comment>
    suspend fun edit(commentId: String, body: String): AppResult<Comment>
    suspend fun delete(commentId: String): AppResult<Unit>
}

interface PreferencesRepository {
    val preferences: Flow<UserPreferences>
    suspend fun setTheme(theme: ThemeMode)
    suspend fun setFontScale(scale: Float)
    suspend fun setPlaybackSpeed(speed: Float)
    suspend fun setReducedMotion(enabled: Boolean)
    suspend fun setDisplayName(name: String)
    suspend fun addRecentSearch(text: String)
    suspend fun clearRecentSearches()
    suspend fun setOnboarded()
    suspend fun setFavouriteMoods(ids: Set<String>)
    /** Records an app open for [isoDay]; returns the updated streak length. */
    suspend fun recordDailyOpen(isoDay: String): Int
}

interface UserRepository {
    val user: Flow<LocalUser>
    suspend fun current(): LocalUser
}
