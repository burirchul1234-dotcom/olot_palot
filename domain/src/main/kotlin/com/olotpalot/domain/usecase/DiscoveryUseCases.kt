package com.olotpalot.domain.usecase

import com.olotpalot.core.common.Clock
import com.olotpalot.domain.model.ContentItem
import com.olotpalot.domain.model.HomeFeed
import com.olotpalot.domain.model.SearchQuery
import com.olotpalot.domain.model.TaxonKind
import com.olotpalot.domain.ranking.InterestProfiler
import com.olotpalot.domain.ranking.RankingEngine
import com.olotpalot.domain.repository.ContentRepository
import com.olotpalot.domain.repository.EngagementRepository
import com.olotpalot.domain.repository.PreferencesRepository
import com.olotpalot.domain.repository.ProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class ObserveHomeFeedUseCase @Inject constructor(
    private val content: ContentRepository,
    private val engagement: EngagementRepository,
    private val progress: ProgressRepository,
    private val preferences: PreferencesRepository,
    private val ranking: RankingEngine,
    private val clock: Clock,
) {
    operator fun invoke(): Flow<HomeFeed> = combine(
        content.observeAll(),
        engagement.observeStates(),
        progress.observeContinue(limit = 5),
        content.observeTaxa(TaxonKind.MOOD),
        preferences.preferences,
    ) { all, states, continueItems, moods, prefs ->
        val now = clock.now()
        val published = all.filter { it.isPublished }
        val continueIds = continueItems.map { it.content.id }.toSet()

        val featured = published.filter { it.isFeatured }.maxByOrNull { it.publishedAt }
            ?: published.maxByOrNull { ranking.trendingScore(it, now) }

        val trending = published
            .filter { it.id != featured?.id }
            .sortedByDescending { ranking.trendingScore(it, now) }
            .take(7)

        val covers = listOfNotNull(featured) + trending.take(4)

        // Daily pick: stable for a calendar day, rotates through the catalog, avoids the cover.
        val dayIndex = (now / 86_400_000L).toInt()
        val pool = published.filter { it.id != featured?.id }.sortedBy { it.id }
        val today = pool.getOrNull(if (pool.isEmpty()) 0 else dayIndex % pool.size)

        val interest = InterestProfiler.from(published, states, prefs.favouriteMoodIds)
        val hasSignal = interest.taxonWeights.isNotEmpty()
        val recommended = published
            .filter { it.id != featured?.id && it.id !in continueIds }
            .sortedByDescending { ranking.recommendationScore(it, interest, states[it.id], now) }
            .take(4)
        val reason = if (hasSignal) {
            val topTaxon = interest.taxonWeights.maxByOrNull { it.value }?.key
            val name = published.flatMap { it.genres + it.moods + it.category }.firstOrNull { it.id == topTaxon }?.name
            name?.let { "Because you’re drawn to $it" }
        } else null

        val recent = published.sortedByDescending { it.publishedAt }.take(6)

        HomeFeed(
            covers = covers,
            today = today,
            streak = prefs.streakCount,
            continueItems = continueItems,
            featured = featured,
            trending = trending,
            recommended = recommended,
            recommendedReason = reason,
            recentlyAdded = recent,
            moods = moods,
        )
    }
}

class SearchContentUseCase @Inject constructor(
    private val content: ContentRepository,
    private val preferences: PreferencesRepository,
) {
    suspend operator fun invoke(query: SearchQuery, remember: Boolean = true): List<ContentItem> {
        val results = content.search(query)
        if (remember && query.text.isNotBlank()) preferences.addRecentSearch(query.text.trim())
        return results
    }
}

class ObserveRelatedUseCase @Inject constructor(
    private val content: ContentRepository,
    private val ranking: RankingEngine,
    private val clock: Clock,
) {
    operator fun invoke(item: ContentItem, limit: Int = 4): Flow<List<ContentItem>> =
        combine(content.observeAll()) { (all) ->
            val wanted = (item.genres + item.moods).map { it.id }.toSet()
            all.filter { it.id != item.id && it.isPublished }
                .sortedByDescending { other ->
                    val overlap = (other.genres + other.moods).count { it.id in wanted }
                    val sameAuthor = if (other.author.id == item.author.id) 2 else 0
                    overlap * 3 + sameAuthor + ranking.trendingScore(other, clock.now())
                }
                .take(limit)
        }
}
