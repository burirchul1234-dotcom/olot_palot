package com.olotpalot.data.repository

import com.olotpalot.core.common.AppDispatchers
import com.olotpalot.core.common.AppResult
import com.olotpalot.core.common.Clock
import com.olotpalot.core.common.runCatchingApp
import com.olotpalot.core.database.OlotPalotDatabase
import com.olotpalot.core.database.dao.CommentDao
import com.olotpalot.core.database.dao.ContentDao
import com.olotpalot.core.database.entity.CommentEntity
import com.olotpalot.data.mapper.toDomain
import com.olotpalot.data.mapper.toEntity
import com.olotpalot.data.remote.RemoteContentDataSource
import com.olotpalot.domain.model.Chapter
import com.olotpalot.domain.model.Collection
import com.olotpalot.domain.model.ContentId
import com.olotpalot.domain.model.ContentItem
import com.olotpalot.domain.model.SearchQuery
import com.olotpalot.domain.model.Taxon
import com.olotpalot.domain.model.TaxonKind
import com.olotpalot.domain.ranking.SearchEvaluator
import com.olotpalot.domain.repository.ContentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import androidx.room.withTransaction
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContentRepositoryImpl @Inject constructor(
    private val db: OlotPalotDatabase,
    private val contentDao: ContentDao,
    private val commentDao: CommentDao,
    private val remote: RemoteContentDataSource,
    private val dispatchers: AppDispatchers,
    private val clock: Clock,
) : ContentRepository {

    private val seedLock = Mutex()

    override fun observeAll(): Flow<List<ContentItem>> = contentDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observeContent(id: ContentId): Flow<ContentItem?> = contentDao.observeById(id).map { it?.toDomain() }

    override fun observeByIds(ids: List<ContentId>): Flow<List<ContentItem>> =
        if (ids.isEmpty()) flowOf(emptyList())
        else contentDao.observeByIds(ids).map { rows ->
            val byId = rows.associateBy { it.content.id }
            ids.mapNotNull { byId[it]?.toDomain() } // preserve caller order
        }

    override suspend fun getContent(id: ContentId): ContentItem? = withContext(dispatchers.io) { contentDao.getById(id)?.toDomain() }

    override suspend fun getChapters(id: ContentId): List<Chapter> = withContext(dispatchers.io) { contentDao.chapters(id).map { it.toDomain() } }

    override suspend fun search(query: SearchQuery): List<ContentItem> = withContext(dispatchers.default) {
        val all = withContext(dispatchers.io) { contentDao.getAll().map { it.toDomain() } }
        SearchEvaluator.evaluate(all, query, clock.now())
    }

    override fun observeTaxa(kind: TaxonKind): Flow<List<Taxon>> = contentDao.observeTaxa(kind.name).map { rows -> rows.map { it.toDomain() } }

    override suspend fun collections(): List<Collection> = withContext(dispatchers.io) { contentDao.collections().map { it.toDomain() } }

    override suspend fun refresh(): AppResult<Unit> = runCatchingApp {
        val snapshot = remote.fetchCatalog()
        withContext(dispatchers.io) {
            db.withTransaction {
                contentDao.upsertContent(snapshot.content.map { it.toEntity() })
                contentDao.upsertMedia(snapshot.content.flatMap { it.media }.map { it.toEntity() })
                contentDao.upsertChapters(snapshot.chapters.map { it.toEntity() })
                contentDao.upsertTaxa(snapshot.taxa.mapIndexed { i, t -> t.toEntity(i) })
                contentDao.upsertCollections(snapshot.collections.mapIndexed { i, c -> c.toEntity(i) })
                // Remote comments are authoritative for their own ids; local pending ones are left alone.
                commentDao.upsertAll(
                    snapshot.comments.map {
                        CommentEntity(it.id, it.contentId, it.userId, it.userName, it.body, it.createdAt, it.editedAt, it.status.name, it.parentId)
                    },
                )
            }
        }
    }

    override suspend fun ensureSeeded() {
        seedLock.withLock {
            if (withContext(dispatchers.io) { contentDao.count() } == 0) refresh()
        }
    }
}
