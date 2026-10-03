package com.olotpalot.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.olotpalot.core.database.entity.ContentWithMedia
import com.olotpalot.core.database.entity.EngagementEventEntity
import com.olotpalot.core.database.entity.HistoryEntity
import com.olotpalot.core.database.entity.UserContentStateEntity
import kotlinx.coroutines.flow.Flow

data class HistoryRow(val kind: String, val at: Long, val contentId: String)

@Dao
interface EngagementDao {
    @Query("SELECT * FROM user_content_state WHERE contentId = :id")
    fun observeState(id: String): Flow<UserContentStateEntity?>

    @Query("SELECT * FROM user_content_state")
    fun observeAll(): Flow<List<UserContentStateEntity>>

    @Query("SELECT * FROM user_content_state WHERE contentId = :id")
    suspend fun get(id: String): UserContentStateEntity?

    @Upsert suspend fun upsert(state: UserContentStateEntity)

    @Insert suspend fun insertEvent(event: EngagementEventEntity)

    @Query("SELECT MAX(at) FROM engagement_events WHERE contentId = :id AND kind = :kind")
    suspend fun lastEventAt(id: String, kind: String): Long?

    @Insert suspend fun insertHistory(entry: HistoryEntity)

    @Query("DELETE FROM history WHERE contentId = :id AND kind = :kind")
    suspend fun deleteHistory(id: String, kind: String)

    @Query("SELECT kind, at, contentId FROM history ORDER BY at DESC LIMIT :limit")
    fun observeHistory(limit: Int): Flow<List<HistoryRow>>

    @Transaction
    @Query("SELECT c.* FROM content c INNER JOIN user_content_state s ON s.contentId = c.id WHERE s.favourite = 1 ORDER BY s.favouritedAt DESC")
    fun observeFavourites(): Flow<List<ContentWithMedia>>

    @Transaction
    @Query("SELECT c.* FROM content c INNER JOIN user_content_state s ON s.contentId = c.id WHERE s.savedForLater = 1 ORDER BY s.savedAt DESC")
    fun observeSavedForLater(): Flow<List<ContentWithMedia>>

    @Transaction
    @Query("SELECT c.* FROM content c INNER JOIN user_content_state s ON s.contentId = c.id WHERE s.completed = 1 ORDER BY s.completedAt DESC")
    fun observeCompleted(): Flow<List<ContentWithMedia>>

    @Query("SELECT COUNT(*) FROM user_content_state WHERE completed = 1")
    fun observeCompletedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM user_content_state WHERE savedForLater = 1 OR favourite = 1")
    fun observeSavedCount(): Flow<Int>

    @Query("SELECT COUNT(DISTINCT contentId) FROM history WHERE kind = 'WATCHED'")
    fun observeVideosWatched(): Flow<Int>
}
