package com.olotpalot.domain.ranking

import com.olotpalot.domain.model.ContentItem
import com.olotpalot.domain.model.Taxon
import com.olotpalot.domain.model.UserContentState
import kotlin.math.ln
import kotlin.math.max

/**
 * Signals the ranker may use. Kept as plain data so a server-side model can
 * later produce the same shape without touching callers.
 */
data class UserInterest(
    val taxonWeights: Map<String, Float> = emptyMap(),
    val authorWeights: Map<String, Float> = emptyMap(),
    val languageWeights: Map<String, Float> = emptyMap(),
)

interface RankingEngine {
    fun trendingScore(item: ContentItem, now: Long): Double
    fun recommendationScore(item: ContentItem, interest: UserInterest, state: UserContentState?, now: Long): Double
}

/**
 * Deterministic first version of the scoring described in the spec:
 *   score = recency + popularity + engagement + user interest + completion relevance
 * Each term is isolated so weights can be tuned (or replaced) independently.
 */
class DeterministicRankingEngine(
    private val weights: Weights = Weights(),
) : RankingEngine {

    data class Weights(
        val recency: Double = 1.0,
        val popularity: Double = 1.0,
        val engagement: Double = 1.2,
        val interest: Double = 1.5,
        val completion: Double = 0.6,
        val halfLifeDays: Double = 14.0,
    )

    private fun recency(item: ContentItem, now: Long): Double {
        val ageDays = max(0.0, (now - item.publishedAt) / 86_400_000.0)
        return Math.pow(0.5, ageDays / weights.halfLifeDays)
    }

    private fun popularity(item: ContentItem): Double = ln(1.0 + item.stats.views) / 12.0

    private fun engagement(item: ContentItem): Double {
        val s = item.stats
        val views = max(1L, s.views).toDouble()
        val likeRate = s.likes / views
        val commentRate = s.comments / views
        val completionRate = s.completions / views
        val rating = if (s.ratingCount > 0) s.ratingAverage / 5.0 else 0.5
        return (likeRate * 4 + commentRate * 6 + completionRate * 3 + rating).coerceAtMost(3.0)
    }

    override fun trendingScore(item: ContentItem, now: Long): Double {
        // Velocity proxy: popularity discounted by age, so new-and-rising beats old-and-big.
        return (popularity(item) * weights.popularity + engagement(item) * weights.engagement) *
            (0.35 + recency(item, now)) + (if (item.isTrending) 0.5 else 0.0)
    }

    override fun recommendationScore(
        item: ContentItem,
        interest: UserInterest,
        state: UserContentState?,
        now: Long,
    ): Double {
        val taxa: List<Taxon> = item.genres + item.moods + item.category
        val interestScore = taxa.sumOf { (interest.taxonWeights[it.id] ?: 0f).toDouble() } +
            (interest.authorWeights[item.author.id] ?: 0f) +
            (interest.languageWeights[item.language.code] ?: 0f)
        val completion = when {
            state?.isCompleted == true -> -1.0 // already finished: deprioritise
            state != null && (state.reading != null || state.audio != null || state.video != null) -> -0.5 // in Continue already
            else -> 0.0
        }
        return recency(item, now) * weights.recency +
            popularity(item) * weights.popularity +
            engagement(item) * weights.engagement +
            interestScore * weights.interest +
            completion * weights.completion
    }
}

/** Derives a lightweight interest profile from what the user has engaged with. */
object InterestProfiler {
    fun from(items: List<ContentItem>, states: Map<String, UserContentState>): UserInterest {
        val taxa = HashMap<String, Float>()
        val authors = HashMap<String, Float>()
        val langs = HashMap<String, Float>()
        for (item in items) {
            val st = states[item.id] ?: continue
            var w = 0f
            if (st.isLiked) w += 1f
            if (st.isFavourite) w += 1.5f
            if (st.isSavedForLater) w += 0.5f
            if (st.isCompleted) w += 1f
            if (st.reading != null || st.audio != null || st.video != null) w += 0.75f
            st.userRating?.let { w += (it - 3) * 0.5f }
            if (w == 0f) continue
            (item.genres + item.moods + item.category).forEach { taxa.merge(it.id, w) { a, b -> a + b } }
            authors.merge(item.author.id, w) { a, b -> a + b }
            langs.merge(item.language.code, w * 0.3f) { a, b -> a + b }
        }
        return UserInterest(taxa, authors, langs)
    }
}
