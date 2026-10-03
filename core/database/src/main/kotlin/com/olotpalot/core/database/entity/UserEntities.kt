package com.olotpalot.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Local projection of the user's explicit relationships with one content
 * item. Remotely these are separate rows (likes, favourites, saved_items,
 * ratings); locally we keep one row per content with per-relationship
 * timestamps and a dirty flag so a sync worker can replay changes in order.
 */
@Entity(tableName = "user_content_state")
data class UserContentStateEntity(
    @PrimaryKey val contentId: String,
    val liked: Boolean = false,
    val likedAt: Long? = null,
    val favourite: Boolean = false,
    val favouritedAt: Long? = null,
    val savedForLater: Boolean = false,
    val savedAt: Long? = null,
    val rating: Int? = null,
    val ratedAt: Long? = null,
    val completed: Boolean = false,
    val completedAt: Long? = null,
    val downloaded: Boolean = false,
    val lastOpenedAt: Long? = null,
    val dirty: Boolean = true,
)

@Entity(tableName = "reading_progress")
data class ReadingProgressEntity(
    @PrimaryKey val contentId: String,
    val chapterIndex: Int,
    val paragraphIndex: Int,
    val fraction: Float,
    val updatedAt: Long,
)

@Entity(tableName = "audio_progress")
data class AudioProgressEntity(
    @PrimaryKey val contentId: String,
    val assetId: String,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAt: Long,
)

@Entity(tableName = "video_progress")
data class VideoProgressEntity(
    @PrimaryKey val contentId: String,
    val assetId: String,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAt: Long,
)

@Entity(tableName = "bookmarks", indices = [Index("contentId")])
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contentId: String,
    val chapterIndex: Int,
    val paragraphIndex: Int,
    val excerpt: String,
    val note: String?,
    val createdAt: Long,
)

@Entity(tableName = "comments", indices = [Index("contentId"), Index("createdAt")])
data class CommentEntity(
    @PrimaryKey val id: String,
    val contentId: String,
    val userId: String,
    val userName: String,
    val body: String,
    val createdAt: Long,
    val editedAt: Long?,
    val status: String,
    val parentId: String?,
    val pendingSync: Boolean = false,
)

/** Raw engagement events: OPEN / START / COMPLETE / SHARE. Views are derived, never incremented blindly. */
@Entity(tableName = "engagement_events", indices = [Index(value = ["contentId", "kind", "at"])])
data class EngagementEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contentId: String,
    val kind: String,
    val at: Long,
)

@Entity(tableName = "history", indices = [Index("at"), Index("contentId")])
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contentId: String,
    val kind: String,
    val at: Long,
)

@Entity(tableName = "analytics_events", indices = [Index("at")])
data class AnalyticsEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val params: String,
    val at: Long,
)

@Entity(tableName = "reward_unlocks", indices = [Index("contentId")])
data class RewardUnlockEntity(
    @PrimaryKey val id: String,
    val contentId: String,
    val userId: String,
    val status: String,
    val providerReference: String?,
    val createdAt: Long,
    val expiresAt: Long?,
)

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val assetId: String,
    val contentId: String,
    val status: String,
    val localPath: String?,
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val errorMessage: String?,
    val updatedAt: Long,
    val workRequestId: String?,
)
