package com.olotpalot.data.repository

import com.olotpalot.core.common.AppDispatchers
import com.olotpalot.core.common.AppError
import com.olotpalot.core.common.AppResult
import com.olotpalot.core.common.Clock
import com.olotpalot.core.database.dao.BookmarkDao
import com.olotpalot.core.database.dao.CommentDao
import com.olotpalot.core.database.dao.ContentDao
import com.olotpalot.core.database.entity.BookmarkEntity
import com.olotpalot.core.database.entity.CommentEntity
import com.olotpalot.data.mapper.toDomain
import com.olotpalot.domain.model.Bookmark
import com.olotpalot.domain.model.Comment
import com.olotpalot.domain.model.CommentStatus
import com.olotpalot.domain.model.ContentId
import com.olotpalot.domain.model.ContentItem
import com.olotpalot.domain.repository.BookmarkRepository
import com.olotpalot.domain.repository.CommentRepository
import com.olotpalot.domain.repository.PreferencesRepository
import com.olotpalot.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookmarkRepositoryImpl @Inject constructor(
    private val dao: BookmarkDao,
    private val contentDao: ContentDao,
    private val dispatchers: AppDispatchers,
    private val clock: Clock,
) : BookmarkRepository {
    override fun observeForContent(id: ContentId): Flow<List<Bookmark>> = dao.observeForContent(id).map { it.map { b -> b.toDomain() } }

    override fun observeAll(): Flow<List<Pair<Bookmark, ContentItem>>> =
        combine(dao.observeAll(), contentDao.observeAll()) { marks, content ->
            val byId = content.associate { it.content.id to it.toDomain() }
            marks.mapNotNull { m -> byId[m.contentId]?.let { m.toDomain() to it } }
        }

    override suspend fun add(id: ContentId, chapterIndex: Int, paragraphIndex: Int, excerpt: String, note: String?): Bookmark =
        withContext(dispatchers.io) {
            val entity = BookmarkEntity(contentId = id, chapterIndex = chapterIndex, paragraphIndex = paragraphIndex, excerpt = excerpt.take(160), note = note, createdAt = clock.now())
            val rowId = dao.insert(entity)
            entity.copy(id = rowId).toDomain()
        }

    override suspend fun remove(bookmarkId: Long) = withContext(dispatchers.io) { dao.delete(bookmarkId) }
}

/**
 * Local-first comments. Created comments are stored with pendingSync=true so
 * a remote implementation can push them; moderation state is modelled now
 * (status) even though only VISIBLE/DELETED are produced locally.
 */
@Singleton
class CommentRepositoryImpl @Inject constructor(
    private val dao: CommentDao,
    private val contentDao: ContentDao,
    private val users: UserRepository,
    private val preferences: PreferencesRepository,
    private val dispatchers: AppDispatchers,
    private val clock: Clock,
) : CommentRepository {

    override fun observeComments(id: ContentId): Flow<List<Comment>> =
        combine(dao.observeForContent(id), users.user) { rows, user -> rows.map { it.toDomain(user.id) } }

    override suspend fun loadPage(id: ContentId, offset: Int, limit: Int): AppResult<List<Comment>> = withContext(dispatchers.io) {
        val user = users.current()
        AppResult.Success(dao.page(id, offset, limit).map { it.toDomain(user.id) })
    }

    override suspend fun create(id: ContentId, body: String): AppResult<Comment> = withContext(dispatchers.io) {
        val clean = sanitize(body)
        if (clean.length < 2) return@withContext AppResult.Failure(AppError.Message("Say a little more."))
        if (clean.length > MAX_LEN) return@withContext AppResult.Failure(AppError.Message("Keep it under $MAX_LEN characters."))
        val user = users.current()
        val name = preferences.preferences.first().displayName
        val entity = CommentEntity(
            id = "local-" + UUID.randomUUID(), contentId = id, userId = user.id, userName = name, body = clean,
            createdAt = clock.now(), editedAt = null, status = CommentStatus.VISIBLE.name, parentId = null, pendingSync = true,
        )
        dao.upsert(entity)
        contentDao.bumpComments(id, 1)
        AppResult.Success(entity.toDomain(user.id))
    }

    override suspend fun edit(commentId: String, body: String): AppResult<Comment> = withContext(dispatchers.io) {
        val existing = dao.get(commentId) ?: return@withContext AppResult.Failure(AppError.NotFound("comment"))
        val user = users.current()
        if (existing.userId != user.id) return@withContext AppResult.Failure(AppError.Message("You can only edit your own comments."))
        val clean = sanitize(body)
        if (clean.length < 2) return@withContext AppResult.Failure(AppError.Message("Say a little more."))
        val updated = existing.copy(body = clean, editedAt = clock.now(), pendingSync = true)
        dao.upsert(updated)
        AppResult.Success(updated.toDomain(user.id))
    }

    override suspend fun delete(commentId: String): AppResult<Unit> = withContext(dispatchers.io) {
        val existing = dao.get(commentId) ?: return@withContext AppResult.Failure(AppError.NotFound("comment"))
        val user = users.current()
        if (existing.userId != user.id) return@withContext AppResult.Failure(AppError.Message("You can only delete your own comments."))
        dao.upsert(existing.copy(status = CommentStatus.DELETED.name, pendingSync = true))
        contentDao.bumpComments(existing.contentId, -1)
        AppResult.Success(Unit)
    }

    /** Client-side hygiene only; the backend re-validates. Strips control chars and collapses whitespace. */
    private fun sanitize(s: String) = s.replace(Regex("[\\p{Cntrl}&&[^\n]]"), "").replace(Regex("\n{3,}"), "\n\n").trim()

    private companion object {
        const val MAX_LEN = 1000
    }
}
