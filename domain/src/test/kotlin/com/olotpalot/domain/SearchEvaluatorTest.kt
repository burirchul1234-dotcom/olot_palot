package com.olotpalot.domain

import com.google.common.truth.Truth.assertThat
import com.olotpalot.domain.model.AccessPolicy
import com.olotpalot.domain.model.Author
import com.olotpalot.domain.model.ContentItem
import com.olotpalot.domain.model.ContentStats
import com.olotpalot.domain.model.Language
import com.olotpalot.domain.model.MediaAsset
import com.olotpalot.domain.model.MediaType
import com.olotpalot.domain.model.MinRating
import com.olotpalot.domain.model.MinViews
import com.olotpalot.domain.model.SearchFilters
import com.olotpalot.domain.model.SearchQuery
import com.olotpalot.domain.model.SortOption
import com.olotpalot.domain.model.Taxon
import com.olotpalot.domain.model.TaxonKind
import com.olotpalot.domain.model.UploadWindow
import com.olotpalot.domain.model.VideoLength
import com.olotpalot.domain.ranking.SearchEvaluator
import org.junit.Test

class SearchEvaluatorTest {
    private val now = 1_000_000_000_000L
    private val day = 86_400_000L
    private val fiction = Taxon("fiction", "Fiction", TaxonKind.CATEGORY)

    private fun item(
        id: String,
        title: String,
        author: String = "Someone",
        publishedDaysAgo: Int = 1,
        video: Long = 0,
        rating: Float = 0f,
        views: Long = 0,
        tags: List<String> = emptyList(),
    ) = ContentItem(
        id = id, title = title, description = "", author = Author(author.lowercase(), author),
        thumbnailUrl = "", heroUrl = "", language = Language.ENGLISH, category = fiction,
        createdAt = now - publishedDaysAgo * day, publishedAt = now - publishedDaysAgo * day, updatedAt = now,
        videoDurationMs = video, tags = tags, access = AccessPolicy.FREE,
        media = buildList { if (video > 0) add(MediaAsset("v$id", id, MediaType.VIDEO, "", video)) },
        chapterCount = 3,
        stats = ContentStats(views = views, ratingAverage = rating, ratingCount = if (rating > 0) 10 else 0),
    )

    private val catalog = listOf(
        item("1", "Echoes of the Starlit Sea", "Mira Sen", publishedDaysAgo = 2, video = 4 * 60_000, rating = 4.7f, views = 12_000),
        item("2", "Midnight at Neon Harbor", "Mira Sen", publishedDaysAgo = 40, video = 25 * 60_000, rating = 3.9f, views = 900),
        item("3", "Coffee, Rain, and Second Chances", "Jonah Pike", publishedDaysAgo = 400, rating = 4.2f, views = 50_000, tags = listOf("romance", "rain")),
    )

    @Test
    fun `title match outranks tag match`() {
        val r = SearchEvaluator.evaluate(catalog, SearchQuery("rain"), now)
        assertThat(r.map { it.id }).containsExactly("3")
    }

    @Test
    fun `author search works`() {
        val r = SearchEvaluator.evaluate(catalog, SearchQuery("mira"), now)
        assertThat(r.map { it.id }).containsExactly("1", "2").inOrder()
    }

    @Test
    fun `all terms must match`() {
        assertThat(SearchEvaluator.evaluate(catalog, SearchQuery("neon sea"), now)).isEmpty()
    }

    @Test
    fun `upload window filter`() {
        val r = SearchEvaluator.evaluate(catalog, SearchQuery(filters = SearchFilters(uploaded = UploadWindow.WEEK)), now)
        assertThat(r.map { it.id }).containsExactly("1")
    }

    @Test
    fun `video length filter excludes non-video`() {
        val r = SearchEvaluator.evaluate(catalog, SearchQuery(filters = SearchFilters(videoLength = VideoLength.LONG)), now)
        assertThat(r.map { it.id }).containsExactly("2")
    }

    @Test
    fun `rating and views filters`() {
        assertThat(SearchEvaluator.evaluate(catalog, SearchQuery(filters = SearchFilters(minRating = MinRating.FOUR)), now).map { it.id })
            .containsExactly("1", "3")
        assertThat(SearchEvaluator.evaluate(catalog, SearchQuery(filters = SearchFilters(minViews = MinViews.K10)), now).map { it.id })
            .containsExactly("1", "3")
    }

    @Test
    fun `sort options`() {
        assertThat(SearchEvaluator.evaluate(catalog, SearchQuery(sort = SortOption.NEWEST), now).map { it.id }).containsExactly("1", "2", "3").inOrder()
        assertThat(SearchEvaluator.evaluate(catalog, SearchQuery(sort = SortOption.OLDEST), now).map { it.id }).containsExactly("3", "2", "1").inOrder()
        assertThat(SearchEvaluator.evaluate(catalog, SearchQuery(sort = SortOption.MOST_VIEWED), now).map { it.id }).containsExactly("3", "1", "2").inOrder()
        assertThat(SearchEvaluator.evaluate(catalog, SearchQuery(sort = SortOption.HIGHEST_RATED), now).map { it.id }).containsExactly("1", "3", "2").inOrder()
        assertThat(SearchEvaluator.evaluate(catalog, SearchQuery(sort = SortOption.SHORTEST_VIDEO), now).map { it.id }).containsExactly("1", "2").inOrder()
    }
}
