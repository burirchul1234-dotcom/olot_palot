package com.olotpalot.domain.usecase

import com.olotpalot.core.common.AnalyticsEvent
import com.olotpalot.core.common.AnalyticsTracker
import com.olotpalot.domain.model.ConsumptionMode
import com.olotpalot.domain.model.ContentId
import com.olotpalot.domain.model.ContentItem
import com.olotpalot.domain.model.HistoryKind
import com.olotpalot.domain.model.UserContentState
import com.olotpalot.domain.repository.ContentRepository
import com.olotpalot.domain.repository.EngagementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

data class ContentDetail(val item: ContentItem, val state: UserContentState)

class ObserveContentDetailUseCase @Inject constructor(
    private val content: ContentRepository,
    private val engagement: EngagementRepository,
) {
    operator fun invoke(id: ContentId): Flow<ContentDetail?> =
        combine(content.observeContent(id), engagement.observeState(id)) { item, state ->
            item?.let { ContentDetail(it, state) }
        }
}

/** Opening detail counts as a view at most once per session window; the repository enforces the window. */
class RecordContentOpenedUseCase @Inject constructor(
    private val engagement: EngagementRepository,
    private val analytics: AnalyticsTracker,
) {
    suspend operator fun invoke(id: ContentId, source: String) {
        val counted = engagement.recordOpen(id)
        engagement.recordHistory(id, HistoryKind.OPENED)
        analytics.track(AnalyticsEvent.CONTENT_OPENED, "contentId" to id, "source" to source, "countedView" to counted)
    }
}

class ToggleLikeUseCase @Inject constructor(
    private val engagement: EngagementRepository,
    private val analytics: AnalyticsTracker,
) {
    suspend operator fun invoke(id: ContentId, current: Boolean) {
        engagement.setLiked(id, !current)
        analytics.track(if (current) AnalyticsEvent.LIKE_REMOVED else AnalyticsEvent.LIKE_ADDED, "contentId" to id)
    }
}

class ToggleFavouriteUseCase @Inject constructor(
    private val engagement: EngagementRepository,
    private val analytics: AnalyticsTracker,
) {
    suspend operator fun invoke(id: ContentId, current: Boolean) {
        engagement.setFavourite(id, !current)
        analytics.track(if (current) AnalyticsEvent.FAVOURITE_REMOVED else AnalyticsEvent.FAVOURITE_ADDED, "contentId" to id)
    }
}

class ToggleSavedForLaterUseCase @Inject constructor(
    private val engagement: EngagementRepository,
    private val analytics: AnalyticsTracker,
) {
    suspend operator fun invoke(id: ContentId, current: Boolean) {
        engagement.setSavedForLater(id, !current)
        analytics.track(if (current) AnalyticsEvent.WATCH_LATER_REMOVED else AnalyticsEvent.WATCH_LATER_ADDED, "contentId" to id)
    }
}

class RateContentUseCase @Inject constructor(
    private val engagement: EngagementRepository,
    private val analytics: AnalyticsTracker,
) {
    suspend operator fun invoke(id: ContentId, stars: Int) {
        require(stars in 1..5)
        engagement.rate(id, stars)
        analytics.track(AnalyticsEvent.RATING_SUBMITTED, "contentId" to id, "stars" to stars)
    }
}

class RecordShareUseCase @Inject constructor(
    private val engagement: EngagementRepository,
    private val analytics: AnalyticsTracker,
) {
    suspend operator fun invoke(id: ContentId) {
        engagement.recordShare(id)
        analytics.track(AnalyticsEvent.SHARE_INITIATED, "contentId" to id)
    }
}

/** Stable identity for sharing; the host is reserved for the HTTPS app link. */
object ContentLinks {
    const val SCHEME = "olotpalot"
    const val WEB_HOST = "olotpalot.app"
    fun deepLink(id: ContentId) = "$SCHEME://story/$id"
    fun webLink(id: ContentId) = "https://$WEB_HOST/story/$id"
    fun modeLink(id: ContentId, mode: ConsumptionMode) = "$SCHEME://story/$id/${mode.name.lowercase()}"
}
