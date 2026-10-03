package com.olotpalot.domain.model

enum class SortOption(val label: String) {
    RELEVANCE("Relevance"),
    NEWEST("Newest"),
    OLDEST("Oldest"),
    HIGHEST_RATED("Highest rated"),
    MOST_VIEWED("Most viewed"),
    SHORTEST_VIDEO("Shortest video"),
    LONGEST_VIDEO("Longest video"),
}

enum class UploadWindow(val label: String, val millis: Long?) {
    ANY("Any time", null),
    TODAY("Today", 24 * 3_600_000L),
    WEEK("This week", 7 * 24 * 3_600_000L),
    MONTH("This month", 30 * 24 * 3_600_000L),
    YEAR("This year", 365 * 24 * 3_600_000L),
}

enum class VideoLength(val label: String, val range: LongRange?) {
    ANY("Any length", null),
    SHORT("Under 5 min", 0L until 5 * 60_000L),
    MEDIUM("5–20 min", 5 * 60_000L until 20 * 60_000L),
    LONG("Over 20 min", 20 * 60_000L..Long.MAX_VALUE),
}

enum class MinRating(val label: String, val value: Float) {
    ANY("Any rating", 0f),
    THREE("3+", 3f),
    FOUR("4+", 4f),
    FOUR_HALF("4.5+", 4.5f),
}

enum class MinViews(val label: String, val value: Long) {
    ANY("Any", 0),
    K1("1k+", 1_000),
    K10("10k+", 10_000),
    K100("100k+", 100_000),
}

data class SearchFilters(
    val uploaded: UploadWindow = UploadWindow.ANY,
    val videoLength: VideoLength = VideoLength.ANY,
    val minRating: MinRating = MinRating.ANY,
    val minViews: MinViews = MinViews.ANY,
    val modes: Set<ConsumptionMode> = emptySet(),
    val language: Language? = null,
    val taxonIds: Set<String> = emptySet(),
) {
    val activeCount: Int
        get() = listOf(
            uploaded != UploadWindow.ANY,
            videoLength != VideoLength.ANY,
            minRating != MinRating.ANY,
            minViews != MinViews.ANY,
            modes.isNotEmpty(),
            language != null,
            taxonIds.isNotEmpty(),
        ).count { it }

    val isEmpty: Boolean get() = activeCount == 0

    companion object {
        val NONE = SearchFilters()
    }
}

data class SearchQuery(
    val text: String = "",
    val sort: SortOption = SortOption.RELEVANCE,
    val filters: SearchFilters = SearchFilters.NONE,
) {
    val isBlank: Boolean get() = text.isBlank() && filters.isEmpty
}
