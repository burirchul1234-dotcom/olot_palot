package com.olotpalot.domain.model

/** Editorial collection: a curated, named list (seeded now, CMS-driven later). */
data class Collection(
    val id: String,
    val title: String,
    val blurb: String,
    val contentIds: List<ContentId>,
)

data class HomeFeed(
    val continueItems: List<ContinueItem>,
    val featured: ContentItem?,
    val trending: List<ContentItem>,
    val recommended: List<ContentItem>,
    val recommendedReason: String?,
    val recentlyAdded: List<ContentItem>,
    val moods: List<Taxon>,
)

data class LibrarySnapshot(
    val continueItems: List<ContinueItem>,
    val savedForLater: List<ContentItem>,
    val favourites: List<ContentItem>,
    val bookmarks: List<Pair<Bookmark, ContentItem>>,
    val history: List<HistoryEntry>,
    val completed: List<ContentItem>,
    val downloaded: List<ContentItem>,
)

enum class LibrarySection(val title: String, val emptyTitle: String, val emptyBody: String) {
    CONTINUE("Continue", "Nothing in progress", "Start something and it will wait for you here."),
    SAVED("Saved for later", "Save something for later.", "Stories you set aside will appear here."),
    FAVOURITES("Favourites", "No favourites yet", "Keep the stories you love in one place."),
    BOOKMARKS("Bookmarks", "No bookmarks", "Mark a passage while reading to find it again."),
    HISTORY("History", "No history yet", "Everything you open shows up here."),
    COMPLETED("Completed", "Nothing finished yet", "Finished stories gather here."),
    DOWNLOADS("Downloads", "Nothing downloaded", "Downloaded stories stay available offline."),
}
