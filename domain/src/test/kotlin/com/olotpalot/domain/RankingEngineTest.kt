package com.olotpalot.domain

import com.google.common.truth.Truth.assertThat
import com.olotpalot.domain.model.Author
import com.olotpalot.domain.model.ContentItem
import com.olotpalot.domain.model.ContentStats
import com.olotpalot.domain.model.Language
import com.olotpalot.domain.model.Taxon
import com.olotpalot.domain.model.TaxonKind
import com.olotpalot.domain.model.UserContentState
import com.olotpalot.domain.ranking.DeterministicRankingEngine
import com.olotpalot.domain.ranking.UserInterest
import org.junit.Test

class RankingEngineTest {
    private val now = 1_000_000_000_000L
    private val day = 86_400_000L
    private val engine = DeterministicRankingEngine()
    private val cat = Taxon("c", "C", TaxonKind.CATEGORY)
    private val calm = Taxon("calm", "Calm", TaxonKind.MOOD)

    private fun item(id: String, daysAgo: Int, views: Long, likes: Long = 0, moods: List<Taxon> = emptyList()) = ContentItem(
        id = id, title = id, description = "", author = Author("a", "A"), thumbnailUrl = "", heroUrl = "",
        language = Language.ENGLISH, category = cat, moods = moods,
        createdAt = now - daysAgo * day, publishedAt = now - daysAgo * day, updatedAt = now,
        stats = ContentStats(views = views, likes = likes),
    )

    @Test
    fun `fresh rising content beats stale giant for trending`() {
        val fresh = item("fresh", daysAgo = 1, views = 2_000, likes = 400)
        val stale = item("stale", daysAgo = 200, views = 50_000, likes = 500)
        assertThat(engine.trendingScore(fresh, now)).isGreaterThan(engine.trendingScore(stale, now))
    }

    @Test
    fun `interest lifts matching content and completion demotes it`() {
        val calmItem = item("calm", 10, 100, moods = listOf(calm))
        val other = item("other", 10, 100)
        val interest = UserInterest(taxonWeights = mapOf("calm" to 2f))
        assertThat(engine.recommendationScore(calmItem, interest, null, now))
            .isGreaterThan(engine.recommendationScore(other, interest, null, now))
        val done = UserContentState("calm", isCompleted = true)
        assertThat(engine.recommendationScore(calmItem, interest, done, now))
            .isLessThan(engine.recommendationScore(calmItem, interest, null, now))
    }
}
