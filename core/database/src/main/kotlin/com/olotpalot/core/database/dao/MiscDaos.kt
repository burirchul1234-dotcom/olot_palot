package com.olotpalot.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import com.olotpalot.core.database.entity.AnalyticsEventEntity
import com.olotpalot.core.database.entity.BookmarkEntity
import com.olotpalot.core.database.entity.CommentEntity
import com.olotpalot.core.database.entity.DownloadEntity
import com.olotpalot.core.database.entity.RewardUnlockEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Insert suspend fun insert(b: BookmarkEntity): Long
    @Query("DELETE FROM bookmarks WHERE id = :id") suspend fun delete(id: Long)
    @Query("SELECT * FROM bookmarks WHERE contentId = :id ORDER BY chapterIndex, paragraphIndex") fun observeForContent(id: String): Flow<List<BookmarkEntity>>
    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC") fun observeAll(): Flow<List<BookmarkEntity>>
    @Query("SELECT * FROM bookmarks WHERE id = :id") suspend fun get(id: Long): BookmarkEntity?
}

@Dao
interface CommentDao {
    @Query("SELECT * FROM comments WHERE contentId = :id AND status != 'DELETED' ORDER BY createdAt DESC")
    fun observeForContent(id: String): Flow<List<CommentEntity>>

    @Query("SELECT * FROM comments WHERE contentId = :id AND status != 'DELETED' ORDER BY createdAt DESC LIMIT :limit OFFSET :offset")
    suspend fun page(id: String, offset: Int, limit: Int): List<CommentEntity>

    @Query("SELECT * FROM comments WHERE id = :id") suspend fun get(id: String): CommentEntity?
    @Upsert suspend fun upsert(c: CommentEntity)
    @Upsert suspend fun upsertAll(c: List<CommentEntity>)
    @Query("SELECT COUNT(*) FROM comments WHERE contentId = :id AND status != 'DELETED'") suspend fun count(id: String): Int
}

@Dao
interface AnalyticsDao {
    @Insert suspend fun insert(e: AnalyticsEventEntity)
    @Query("DELETE FROM analytics_events WHERE at < :before") suspend fun prune(before: Long)
    @Query("SELECT * FROM analytics_events ORDER BY at DESC LIMIT :limit") suspend fun recent(limit: Int): List<AnalyticsEventEntity>
}

@Dao
interface RewardDao {
    @Upsert suspend fun upsert(r: RewardUnlockEntity)
    @Query("SELECT * FROM reward_unlocks WHERE contentId = :id AND status = 'GRANTED' ORDER BY createdAt DESC LIMIT 1")
    suspend fun latestGrant(id: String): RewardUnlockEntity?
    @Query("SELECT * FROM reward_unlocks WHERE contentId = :id AND status = 'GRANTED'")
    fun observeGrants(id: String): Flow<List<RewardUnlockEntity>>
}

@Dao
interface DownloadDao {
    @Upsert suspend fun upsert(d: DownloadEntity)
    @Query("SELECT * FROM downloads WHERE contentId = :id") fun observeForContent(id: String): Flow<List<DownloadEntity>>
    @Query("SELECT * FROM downloads") fun observeAll(): Flow<List<DownloadEntity>>
    @Query("SELECT * FROM downloads WHERE assetId = :assetId") suspend fun get(assetId: String): DownloadEntity?
    @Query("DELETE FROM downloads WHERE assetId = :assetId") suspend fun delete(assetId: String)
}
