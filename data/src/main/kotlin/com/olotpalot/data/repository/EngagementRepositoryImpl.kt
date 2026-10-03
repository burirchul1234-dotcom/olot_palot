package com.olotpalot.data.repository

import com.olotpalot.core.common.AppDispatchers
import com.olotpalot.core.common.Clock
import com.olotpalot.core.database.OlotPalotDatabase
import com.olotpalot.core.database.dao.ContentDao
import com.olotpalot.core.database.dao.EngagementDao
import com.olotpalot.core.database.dao.ProgressDao
import com.olotpalot.core.database.entity.EngagementEventEntity
import com.olotpalot.core.database.entity.HistoryEntity
import com.olotpalot.core.database.entity.UserContentStateEntity
import com.olotpalot.data.mapper.toDomain
import com.olotpalot.domain.model.ContentId
import com.olotpalot.domain.model.ContentItem
import com.olotpalot.domain.model.HistoryEntry
import com.olotpalot.domain.model.HistoryKind
import com.olotpalot.domain.model.UserContentState
import com.olotpalot.domain.model.UserStats
import com.olotpalot.domain.repository.EngagementRepository
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EngagementRepositoryImpl @Inject constructor(
    private val db: OlotPalotDatabase,
    private val engagementDao: EngagementDao,
    private val progressDao: ProgressDao,
    private val contentDao: ContentDao,
    private val dispatchers: AppDispatchers,
    private val clock: Clock,
) : EngagementRepository {

    override fun observeState(id: ContentId): Flow<UserContentState> = combine(
        engagementDao.observeState(id),
        progressDao.observeReading(id),
        progressDao.observeAudio(id),
        progressDao.observeVideo(id),
    ) { state, r, a, v -> state.toDomain(id, r, a, v) }

    override fun observeStates(): Flow<Map<ContentId, UserContentState>> = combine(
        engagementDao.observeAll(),
        progressDao.observeReading(),
        progressDao.observeAudio(),
        progressDao.observeVideo(),
    ) { states, r, a, v ->
        val rm = r.associateBy { it.contentId }
        val am = a.associateBy { it.contentId }
        val vm = v.associateBy { it.contentId }
        val ids = states.map { it.contentId }.toSet() + rm.keys + am.keys + vm.keys
        val sm = states.associateBy { it.contentId }
        ids.associateWith { id -> sm[id].toDomain(id, rm[id], am[id], vm[id]) }
    }

    private suspend fun current(id: ContentId) = engagementDao.get(id) ?: UserContentStateEntity(contentId = id)

    override suspend fun setLiked(id: ContentId, liked: Boolean) = withContext(dispatchers.io) {
        db.withTransaction {
            val s = current(id)
            if (s.liked == liked) return@withTransaction
            engagementDao.upsert(s.copy(liked = liked, likedAt = clock.now(), dirty = true))
            contentDao.bumpLikes(id, if (liked) 1 else -1)
        }
    }

    override suspend fun setFavourite(id: ContentId, favourite: Boolean) = withContext(dispatchers.io) {
        val s = current(id)
        engagementDao.upsert(s.copy(favourite = favourite, favouritedAt = clock.now(), dirty = true))
    }

    override suspend fun setSavedForLater(id: ContentId, saved: Boolean) = withContext(dispatchers.io) {
        val s = current(id)
        engagementDao.upsert(s.copy(savedForLater = saved, savedAt = clock.now(), dirty = true))
    }

    /** One rating per user per content: a re-rate replaces, it never adds a second vote. */
    override suspend fun rate(id: ContentId, stars: Int) = withContext(dispatchers.io) {
        db.withTransaction {
            val s = current(id)
            val content = contentDao.getById(id)?.content ?: return@withTransaction
            val prev = s.rating
            val count = content.ratingCount
            val sum = content.ratingAverage * count
            val (newSum, newCount) = if (prev == null) (sum + stars) to (count + 1) else (sum - prev + stars) to count
            contentDao.setRating(id, if (newCount > 0) newSum / newCount else 0f, newCount)
            engagementDao.upsert(s.copy(rating = stars, ratedAt = clock.now(), dirty = true))
        }
    }

    override suspend fun recordOpen(id: ContentId): Boolean = withContext(dispatchers.io) {
        val now = clock.now()
        db.withTransaction {
            val last = engagementDao.lastEventAt(id, OPEN)
            engagementDao.upsert(current(id).copy(lastOpenedAt = now))
            if (last != null && now - last < VIEW_WINDOW_MS) return@withTransaction false
            engagementDao.insertEvent(EngagementEventEntity(contentId = id, kind = OPEN, at = now))
            contentDao.bumpViews(id)
            true
        }
    }

    override suspend fun recordShare(id: ContentId) = withContext(dispatchers.io) {
        engagementDao.insertEvent(EngagementEventEntity(contentId = id, kind = SHARE, at = clock.now()))
        contentDao.bumpShares(id)
    }

    override suspend fun recordHistory(id: ContentId, kind: HistoryKind) = withContext(dispatchers.io) {
        db.withTransaction {
            engagementDao.deleteHistory(id, kind.name)
            engagementDao.insertHistory(HistoryEntity(contentId = id, kind = kind.name, at = clock.now()))
        }
    }

    override fun observeHistory(limit: Int): Flow<List<HistoryEntry>> =
        combine(engagementDao.observeHistory(limit), contentDao.observeAll()) { rows, content ->
            val byId = content.associate { it.content.id to it.toDomain() }
            rows.mapNotNull { r -> byId[r.contentId]?.let { HistoryEntry(it, HistoryKind.valueOf(r.kind), r.at) } }
        }

    override fun observeFavourites(): Flow<List<ContentItem>> = engagementDao.observeFavourites().map { it.map { r -> r.toDomain() } }
    override fun observeSavedForLater(): Flow<List<ContentItem>> = engagementDao.observeSavedForLater().map { it.map { r -> r.toDomain() } }
    override fun observeCompleted(): Flow<List<ContentItem>> = engagementDao.observeCompleted().map { it.map { r -> r.toDomain() } }

    override fun observeStats(): Flow<UserStats> = combine(
        engagementDao.observeCompletedCount(),
        progressDao.observeListenedMs(),
        engagementDao.observeVideosWatched(),
        engagementDao.observeSavedCount(),
    ) { completed, listenedMs, videos, saved ->
        UserStats(storiesCompleted = completed, minutesListened = listenedMs / 60_000, videosWatched = videos, saved = saved)
    }

    private companion object {
        const val OPEN = "OPEN"
        const val SHARE = "SHARE"
        const val VIEW_WINDOW_MS = 30 * 60_000L
    }
}
