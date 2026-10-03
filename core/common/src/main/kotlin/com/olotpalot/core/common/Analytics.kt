package com.olotpalot.core.common

/**
 * Vendor-agnostic analytics. Feature code emits [AnalyticsEvent]s; the bound
 * [AnalyticsTracker] decides where they go (logcat + Room in development).
 */
data class AnalyticsEvent(
    val name: String,
    val params: Map<String, Any?> = emptyMap(),
) {
    companion object {
        const val APP_OPENED = "AppOpened"
        const val CONTENT_IMPRESSION = "ContentImpression"
        const val CONTENT_OPENED = "ContentOpened"
        const val SEARCH_PERFORMED = "SearchPerformed"
        const val FILTER_APPLIED = "FilterApplied"
        const val LISTEN_STARTED = "ListenStarted"
        const val LISTEN_PROGRESS = "ListenProgress"
        const val LISTEN_COMPLETED = "ListenCompleted"
        const val READ_STARTED = "ReadStarted"
        const val READ_PROGRESS = "ReadProgress"
        const val READ_COMPLETED = "ReadCompleted"
        const val VIDEO_STARTED = "VideoStarted"
        const val VIDEO_PROGRESS = "VideoProgress"
        const val VIDEO_COMPLETED = "VideoCompleted"
        const val LIKE_ADDED = "LikeAdded"
        const val LIKE_REMOVED = "LikeRemoved"
        const val FAVOURITE_ADDED = "FavouriteAdded"
        const val FAVOURITE_REMOVED = "FavouriteRemoved"
        const val COMMENT_CREATED = "CommentCreated"
        const val RATING_SUBMITTED = "RatingSubmitted"
        const val WATCH_LATER_ADDED = "WatchLaterAdded"
        const val WATCH_LATER_REMOVED = "WatchLaterRemoved"
        const val BOOKMARK_ADDED = "BookmarkAdded"
        const val BOOKMARK_REMOVED = "BookmarkRemoved"
        const val DOWNLOAD_STARTED = "DownloadStarted"
        const val DOWNLOAD_COMPLETED = "DownloadCompleted"
        const val DOWNLOAD_FAILED = "DownloadFailed"
        const val SHARE_INITIATED = "ShareInitiated"
        const val THEME_CHANGED = "ThemeChanged"
        const val REWARD_STARTED = "RewardStarted"
        const val REWARD_GRANTED = "RewardGranted"
        const val REWARD_FAILED = "RewardFailed"
    }
}

interface AnalyticsTracker {
    fun track(event: AnalyticsEvent)
    fun track(name: String, vararg params: Pair<String, Any?>) = track(AnalyticsEvent(name, params.toMap()))
}
