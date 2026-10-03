package com.olotpalot.data.repository

import com.olotpalot.core.common.AppDispatchers
import com.olotpalot.core.common.Clock
import com.olotpalot.core.database.OlotPalotDatabase
import com.olotpalot.core.database.dao.ContentDao
import com.olotpalot.core.database.dao.EngagementDao
import com.olotpalot.core.database.dao.ProgressDao
import com.olotpalot.core.database.entity.HistoryEntity
import com.olotpalot.core.database.entity.UserContentStateEntity
import com.olotpalot.data.mapper.toAudioEntity
import com.olotpalot.data.mapper.toDomain
import com.olotpalot.data.mapper.toEntity
import com.olotpalot.data.mapper.toVideoEntity
import com.olotpalot.domain.model.ConsumptionMode
import com.olotpalot.domain.model.ContentId
import com.olotpalot.domain.model.ContinueItem
import com.olotpalot.domain.model.HistoryKind
import com.olotpalot.domain.model.MediaProgress
import com.olotpalot.domain.model.ReadingProgress
import com.olotpalot.domain.repository.ProgressRepository
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProgressRepositoryImpl @Inject constructor(
    private val db: OlotPalotDatabase,
    private val progressDao: ProgressDao,
    private val contentDao: ContentDao,
    private val engagementDao: EngagementDao,
    private val dispatchers: AppDispatchers,
    private val clock: Clock,
) : ProgressRepository {

    override fun observeContinue(limit: Int): Flow<List<ContinueItem>> = combine(
        progressDao.observeReading(),
        progressDao.observeAudio(),
        progressDao.observeVideo(),
        engagementDao.observeAll(),
        contentDao.observeAll(),
    ) { reading, audio, video, states, content ->
        val byId = content.associate { it.content.id to it.toDomain() }
        val completed = states.filter { it.completed }.map { it.contentId }.toSet()
        val candidates = buildList {
            reading.forEach { r ->
                byId[r.contentId]?.let { add(ContinueItem(it, ConsumptionMode.READ, r.fraction, (it.readingMinutes * 60_000L * (1 - r.fraction)).toLong(), r.updatedAt)) }
            }
            audio.forEach { a ->
                val p = a.toDomain()
                byId[a.contentId]?.let { add(ContinueItem(it, ConsumptionMode.LISTEN, p.fraction, p.remainingMs, a.updatedAt)) }
            }
            video.forEach { v ->
                val p = v.toDomain()
                byId[v.contentId]?.let { add(ContinueItem(it, ConsumptionMode.WATCH, p.fraction, p.remainingMs, v.updatedAt)) }
            }
        }
        candidates
            .filter { it.content.id !in completed && it.fraction < COMPLETE_THRESHOLD && it.fraction > 0f }
            .groupBy { it.content.id }
            .map { (_, items) -> items.maxBy { it.lastActivityAt } } // one entry per story: its latest mode
            .sortedByDescending { it.lastActivityAt }
            .take(limit)
    }

    override suspend fun saveReading(progress: ReadingProgress) = withContext(dispatchers.io) {
        progressDao.upsertReading(progress.toEntity())
        touchHistory(progress.contentId, HistoryKind.READ)
    }

    override suspend fun saveAudio(progress: MediaProgress) = withContext(dispatchers.io) {
        progressDao.upsertAudio(progress.toAudioEntity())
        touchHistory(progress.contentId, HistoryKind.LISTENED)
    }

    override suspend fun saveVideo(progress: MediaProgress) = withContext(dispatchers.io) {
        progressDao.upsertVideo(progress.toVideoEntity())
        touchHistory(progress.contentId, HistoryKind.WATCHED)
    }

    override suspend fun markCompleted(id: ContentId, mode: ConsumptionMode) = withContext(dispatchers.io) {
        db.withTransaction {
            val s = engagementDao.get(id) ?: UserContentStateEntity(contentId = id)
            if (!s.completed) {
                engagementDao.upsert(s.copy(completed = true, completedAt = clock.now(), dirty = true))
                contentDao.bumpCompletions(id)
            }
            engagementDao.deleteHistory(id, HistoryKind.COMPLETED.name)
            engagementDao.insertHistory(HistoryEntity(contentId = id, kind = HistoryKind.COMPLETED.name, at = clock.now()))
        }
    }

    override suspend fun reading(id: ContentId) = withContext(dispatchers.io) { progressDao.reading(id)?.toDomain() }
    override suspend fun audio(id: ContentId) = withContext(dispatchers.io) { progressDao.audio(id)?.toDomain() }
    override suspend fun video(id: ContentId) = withContext(dispatchers.io) { progressDao.video(id)?.toDomain() }

    private suspend fun touchHistory(id: ContentId, kind: HistoryKind) {
        // Throttle: history rows are per (content, kind); we replace rather than append.
        db.withTransaction {
            engagementDao.deleteHistory(id, kind.name)
            engagementDao.insertHistory(HistoryEntity(contentId = id, kind = kind.name, at = clock.now()))
        }
    }

    private companion object {
        const val COMPLETE_THRESHOLD = 0.985f
    }
}
