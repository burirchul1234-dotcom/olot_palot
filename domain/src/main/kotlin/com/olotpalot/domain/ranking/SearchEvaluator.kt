package com.olotpalot.domain.ranking

import com.olotpalot.domain.model.ConsumptionMode
import com.olotpalot.domain.model.ContentItem
import com.olotpalot.domain.model.MinRating
import com.olotpalot.domain.model.MinViews
import com.olotpalot.domain.model.SearchFilters
import com.olotpalot.domain.model.SearchQuery
import com.olotpalot.domain.model.SortOption
import com.olotpalot.domain.model.UploadWindow
import com.olotpalot.domain.model.VideoLength

/**
 * In-memory search over the cached catalog. Pure and testable; the same
 * predicates map directly onto SQL/PostgREST filters when the remote grows.
 */
object SearchEvaluator {

    fun evaluate(items: List<ContentItem>, query: SearchQuery, now: Long): List<ContentItem> {
        val terms = query.text.trim().lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
        val scored = items.asSequence()
            .filter { it.isPublished }
            .filter { matchesFilters(it, query.filters, now) }
            .mapNotNull { item ->
                val score = if (terms.isEmpty()) 1.0 else relevance(item, terms)
                if (score > 0) item to score else null
            }
            .toList()
        return sort(scored, query.sort)
    }

    fun matchesFilters(item: ContentItem, f: SearchFilters, now: Long): Boolean {
        if (f.uploaded != UploadWindow.ANY) {
            val window = f.uploaded.millis ?: Long.MAX_VALUE
            if (now - item.publishedAt > window) return false
        }
        if (f.videoLength != VideoLength.ANY) {
            val range = f.videoLength.range ?: return false
            if (item.videoDurationMs <= 0 || item.videoDurationMs !in range) return false
        }
        if (f.minRating != MinRating.ANY && item.stats.ratingAverage < f.minRating.value) return false
        if (f.minViews != MinViews.ANY && item.stats.views < f.minViews.value) return false
        if (f.modes.isNotEmpty() && f.modes.none { item.supports(it) }) return false
        if (f.language != null && item.language != f.language) return false
        if (f.taxonIds.isNotEmpty()) {
            val ids = (item.genres + item.moods + item.category).map { it.id }.toSet()
            if (f.taxonIds.none { it in ids }) return false
        }
        return true
    }

    /** Weighted field matching: title > author > tags/taxa > subtitle/description. */
    fun relevance(item: ContentItem, terms: List<String>): Double {
        val title = item.title.lowercase()
        val author = item.author.name.lowercase()
        val subtitle = item.subtitle?.lowercase().orEmpty()
        val description = item.description.lowercase()
        val taxa = (item.genres + item.moods + item.category).joinToString(" ") { it.name.lowercase() }
        val tags = item.tags.joinToString(" ") { it.lowercase() }
        var score = 0.0
        for (t in terms) {
            var hit = 0.0
            if (title.contains(t)) hit += if (title.startsWith(t)) 6.0 else 4.0
            if (author.contains(t)) hit += 3.0
            if (tags.contains(t) || taxa.contains(t)) hit += 2.5
            if (subtitle.contains(t)) hit += 1.5
            if (description.contains(t)) hit += 1.0
            if (hit == 0.0) return 0.0 // every term must match something
            score += hit
        }
        return score
    }

    private fun sort(scored: List<Pair<ContentItem, Double>>, sort: SortOption): List<ContentItem> = when (sort) {
        SortOption.RELEVANCE -> scored.sortedWith(compareByDescending<Pair<ContentItem, Double>> { it.second }.thenByDescending { it.first.stats.views })
        SortOption.NEWEST -> scored.sortedByDescending { it.first.publishedAt }
        SortOption.OLDEST -> scored.sortedBy { it.first.publishedAt }
        SortOption.HIGHEST_RATED -> scored.sortedWith(compareByDescending<Pair<ContentItem, Double>> { it.first.stats.ratingAverage }.thenByDescending { it.first.stats.ratingCount })
        SortOption.MOST_VIEWED -> scored.sortedByDescending { it.first.stats.views }
        SortOption.SHORTEST_VIDEO -> scored.filter { it.first.supports(ConsumptionMode.WATCH) }.sortedBy { it.first.videoDurationMs }
        SortOption.LONGEST_VIDEO -> scored.filter { it.first.supports(ConsumptionMode.WATCH) }.sortedByDescending { it.first.videoDurationMs }
    }.map { it.first }
}
