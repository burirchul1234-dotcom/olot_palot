package com.olotpalot.core.navigation

import kotlinx.serialization.Serializable

/**
 * Every destination in the app, shared by all feature modules so that no
 * feature depends on another. Type-safe Navigation Compose routes.
 */
@Serializable object HomeRoute
@Serializable data class DiscoverRoute(val query: String? = null, val taxonId: String? = null, val focusSearch: Boolean = false)
@Serializable object WatchRoute
@Serializable object LibraryRoute
@Serializable data class LibrarySectionRoute(val section: String)
@Serializable object ProfileRoute
@Serializable object AppearanceRoute

@Serializable data class StoryRoute(val contentId: String)
@Serializable data class CommentsRoute(val contentId: String)
@Serializable data class ReaderRoute(val contentId: String)
@Serializable data class ListenRoute(val contentId: String)
@Serializable data class PlayerRoute(val contentId: String)

/** Top-level sections, in display order. One list, used by one nav component. */
enum class TopLevel(val key: String, val label: String) {
    HOME("home", "Home"),
    DISCOVER("discover", "Discover"),
    WATCH("watch", "Watch"),
    LIBRARY("library", "Library"),
    PROFILE("profile", "Profile"),
}

object DeepLinks {
    const val SCHEME = "olotpalot"
    const val HOST = "olotpalot.app"
    val storyPatterns = listOf("$SCHEME://story/{contentId}", "https://$HOST/story/{contentId}")
}
