package com.olotpalot.domain.model

typealias ContentId = String

enum class Language(val code: String, val label: String, val nativeLabel: String) {
    ENGLISH("en", "English", "English"),
    BENGALI("bn", "Bengali", "বাংলা");

    companion object {
        fun fromCode(code: String?): Language = entries.firstOrNull { it.code == code } ?: ENGLISH
    }
}

enum class MediaType { AUDIO, VIDEO, DOCUMENT, IMAGE }

/**
 * The three user-facing ways to consume content. Order matters: it is the
 * product's priority order (Listen first, then Read Story, then Watch).
 */
enum class ConsumptionMode(val label: String) {
    LISTEN("Listen"),
    READ("Read Story"),
    WATCH("Watch");
}

enum class AccessPolicy { FREE, REWARDED, PREMIUM }

data class Author(
    val id: String,
    val name: String,
    val bio: String? = null,
    val avatarUrl: String? = null,
)

/** Category / genre / mood share one shape; they differ in how discovery uses them. */
data class Taxon(val id: String, val name: String, val kind: TaxonKind)

enum class TaxonKind { CATEGORY, GENRE, MOOD }

data class MediaAsset(
    val id: String,
    val contentId: ContentId,
    val type: MediaType,
    val url: String,
    val durationMs: Long = 0,
    val pageCount: Int = 0,
    val mimeType: String? = null,
    val sizeBytes: Long = 0,
    val thumbnailUrl: String? = null,
    val downloadable: Boolean = true,
    /** 1:1 mapping to a chapter when audio is narrated per chapter; null = whole-content asset. */
    val chapterIndex: Int? = null,
)

data class Chapter(
    val id: String,
    val contentId: ContentId,
    val index: Int,
    val title: String,
    /** Plain paragraphs separated by blank lines. The reader renders these natively. */
    val body: String,
    val wordCount: Int,
)

data class ContentStats(
    val views: Long = 0,
    val likes: Long = 0,
    val comments: Long = 0,
    val shares: Long = 0,
    val ratingAverage: Float = 0f,
    val ratingCount: Int = 0,
    val completions: Long = 0,
)

data class ContentItem(
    val id: ContentId,
    val title: String,
    val subtitle: String? = null,
    val description: String,
    val author: Author,
    val thumbnailUrl: String,
    val heroUrl: String,
    val language: Language,
    val category: Taxon,
    val genres: List<Taxon> = emptyList(),
    val moods: List<Taxon> = emptyList(),
    val tags: List<String> = emptyList(),
    val createdAt: Long,
    val publishedAt: Long,
    val updatedAt: Long,
    val isPublished: Boolean = true,
    val isFeatured: Boolean = false,
    val isTrending: Boolean = false,
    val readingMinutes: Int = 0,
    val audioDurationMs: Long = 0,
    val videoDurationMs: Long = 0,
    val pageCount: Int = 0,
    val access: AccessPolicy = AccessPolicy.FREE,
    val media: List<MediaAsset> = emptyList(),
    val chapterCount: Int = 0,
    val stats: ContentStats = ContentStats(),
) {
    val modes: List<ConsumptionMode>
        get() = buildList {
            if (media.any { it.type == MediaType.AUDIO }) add(ConsumptionMode.LISTEN)
            if (chapterCount > 0 || media.any { it.type == MediaType.DOCUMENT }) add(ConsumptionMode.READ)
            if (media.any { it.type == MediaType.VIDEO }) add(ConsumptionMode.WATCH)
        }

    val primaryMode: ConsumptionMode? get() = modes.firstOrNull()

    fun supports(mode: ConsumptionMode) = mode in modes

    fun durationFor(mode: ConsumptionMode): Long = when (mode) {
        ConsumptionMode.LISTEN -> audioDurationMs
        ConsumptionMode.READ -> readingMinutes * 60_000L
        ConsumptionMode.WATCH -> videoDurationMs
    }

    val videoAsset: MediaAsset? get() = media.firstOrNull { it.type == MediaType.VIDEO }
    val audioAssets: List<MediaAsset> get() = media.filter { it.type == MediaType.AUDIO }.sortedBy { it.chapterIndex ?: 0 }
}
