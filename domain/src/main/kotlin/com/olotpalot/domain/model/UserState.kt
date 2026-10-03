package com.olotpalot.domain.model

/**
 * The user's relationship with one piece of content, folded into a single
 * read model so screens don't join six tables. Each flag is backed by its own
 * explicit relationship on the write side (Like, Favourite, SavedItem, Rating...).
 */
data class UserContentState(
    val contentId: ContentId,
    val isLiked: Boolean = false,
    val isFavourite: Boolean = false,
    val isSavedForLater: Boolean = false,
    val userRating: Int? = null,
    val isDownloaded: Boolean = false,
    val isCompleted: Boolean = false,
    val reading: ReadingProgress? = null,
    val audio: MediaProgress? = null,
    val video: MediaProgress? = null,
    val lastOpenedAt: Long? = null,
) {
    companion object {
        fun empty(id: ContentId) = UserContentState(id)
    }

    /** The mode the user most recently made progress in, if any. */
    val activeMode: ConsumptionMode?
        get() = listOfNotNull(
            audio?.let { ConsumptionMode.LISTEN to it.updatedAt },
            reading?.let { ConsumptionMode.READ to it.updatedAt },
            video?.let { ConsumptionMode.WATCH to it.updatedAt },
        ).maxByOrNull { it.second }?.first

    fun fraction(mode: ConsumptionMode): Float = when (mode) {
        ConsumptionMode.LISTEN -> audio?.fraction ?: 0f
        ConsumptionMode.READ -> reading?.fraction ?: 0f
        ConsumptionMode.WATCH -> video?.fraction ?: 0f
    }
}

data class ReadingProgress(
    val contentId: ContentId,
    val chapterIndex: Int,
    val paragraphIndex: Int,
    val fraction: Float,
    val updatedAt: Long,
)

data class MediaProgress(
    val contentId: ContentId,
    val assetId: String,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAt: Long,
) {
    val fraction: Float get() = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val remainingMs: Long get() = (durationMs - positionMs).coerceAtLeast(0)
}

data class Bookmark(
    val id: Long,
    val contentId: ContentId,
    val chapterIndex: Int,
    val paragraphIndex: Int,
    val excerpt: String,
    val note: String? = null,
    val createdAt: Long,
)

enum class CommentStatus { VISIBLE, HIDDEN, DELETED, PENDING }

data class Comment(
    val id: String,
    val contentId: ContentId,
    val userId: String,
    val userName: String,
    val body: String,
    val createdAt: Long,
    val editedAt: Long? = null,
    val status: CommentStatus = CommentStatus.VISIBLE,
    /** Reserved for threaded replies; null means top-level. */
    val parentId: String? = null,
    val isMine: Boolean = false,
)

data class ContinueItem(
    val content: ContentItem,
    val mode: ConsumptionMode,
    val fraction: Float,
    val remainingMs: Long,
    val lastActivityAt: Long,
)

enum class HistoryKind { OPENED, LISTENED, READ, WATCHED, COMPLETED }

data class HistoryEntry(
    val content: ContentItem,
    val kind: HistoryKind,
    val at: Long,
)

data class UserStats(
    val storiesCompleted: Int = 0,
    val minutesListened: Long = 0,
    val minutesRead: Long = 0,
    val videosWatched: Int = 0,
    val saved: Int = 0,
)

data class LocalUser(
    val id: String,
    val displayName: String,
    val isSignedIn: Boolean = false,
)
