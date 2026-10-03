package com.olotpalot.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import com.olotpalot.core.common.TimeFormat
import com.olotpalot.core.design.components.ContinueStrip
import com.olotpalot.core.design.components.NavItem
import com.olotpalot.core.design.components.TypographicNav
import com.olotpalot.core.design.theme.OPMotion
import com.olotpalot.core.design.theme.OPTheme
import com.olotpalot.core.navigation.CommentsRoute
import com.olotpalot.core.navigation.DeepLinks
import com.olotpalot.core.navigation.DiscoverRoute
import com.olotpalot.core.navigation.HomeRoute
import com.olotpalot.core.navigation.LibraryRoute
import com.olotpalot.core.navigation.LibrarySectionRoute
import com.olotpalot.core.navigation.ListenRoute
import com.olotpalot.core.navigation.PlayerRoute
import com.olotpalot.core.navigation.ProfileRoute
import com.olotpalot.core.navigation.ReaderRoute
import com.olotpalot.core.navigation.StoryRoute
import com.olotpalot.core.navigation.TopLevel
import com.olotpalot.core.navigation.WatchRoute
import com.olotpalot.domain.model.ConsumptionMode
import com.olotpalot.feature.discover.DiscoverScreen
import com.olotpalot.feature.home.HomeActions
import com.olotpalot.feature.home.HomeScreen
import com.olotpalot.feature.library.LibraryActions
import com.olotpalot.feature.library.LibraryScreen
import com.olotpalot.feature.library.LibrarySectionScreen
import com.olotpalot.feature.player.ListenScreen
import com.olotpalot.feature.player.WatchPlayerScreen
import com.olotpalot.feature.profile.ProfileScreen
import com.olotpalot.feature.reader.ReaderScreen
import com.olotpalot.feature.story.CommentsScreen
import com.olotpalot.feature.story.StoryActions
import com.olotpalot.feature.story.StoryScreen
import com.olotpalot.feature.watch.WatchScreen

private val navItems = TopLevel.entries.map { NavItem(it.key, it.label) }

@Composable
fun AppShell(mainViewModel: MainViewModel) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val destination = backStack?.destination
    val continueItem by mainViewModel.continueItem.collectAsStateWithLifecycle()

    val currentTop = TopLevel.entries.firstOrNull { top ->
        destination?.hierarchy?.any { d ->
            when (top) {
                TopLevel.HOME -> d.hasRoute<HomeRoute>()
                TopLevel.DISCOVER -> d.hasRoute<DiscoverRoute>()
                TopLevel.WATCH -> d.hasRoute<WatchRoute>()
                TopLevel.LIBRARY -> d.hasRoute<LibraryRoute>()
                TopLevel.PROFILE -> d.hasRoute<ProfileRoute>()
            }
        } == true
    }
    // Chrome hides only inside the three immersive consumption screens. Everywhere else it is identical.
    val immersive = destination?.let { it.hasRoute<ReaderRoute>() || it.hasRoute<ListenRoute>() || it.hasRoute<PlayerRoute>() } ?: false
    val onConsumptionOfSame = immersive

    Column(Modifier.fillMaxSize().background(OPTheme.colors.background)) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            AppNavHost(navController)
        }
        AnimatedVisibility(
            visible = !immersive,
            enter = fadeIn(tween(OPMotion.standard)) + expandVertically(tween(OPMotion.standard)),
            exit = fadeOut(tween(OPMotion.fast)) + shrinkVertically(tween(OPMotion.fast)),
        ) {
            Column {
                val ci = continueItem
                AnimatedVisibility(
                    visible = ci != null && !onConsumptionOfSame,
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut() + slideOutVertically { it / 2 },
                ) {
                    val item = ci ?: return@AnimatedVisibility
                    val verb = when (item.mode) { ConsumptionMode.LISTEN -> "Listening"; ConsumptionMode.READ -> "Reading"; ConsumptionMode.WATCH -> "Watching" }
                    ContinueStrip(
                        eyebrow = "$verb  ·  ${TimeFormat.duration(item.remainingMs)} left",
                        title = item.content.title,
                        progress = item.fraction,
                        artworkUrl = item.content.thumbnailUrl,
                        onOpen = { navController.openMode(item.content.id, item.mode) },
                        onDismiss = { mainViewModel.dismissContinue(item) },
                    )
                }
                TypographicNav(
                    items = navItems,
                    currentKey = currentTop?.key,
                    onSelect = { item ->
                        val route: Any = when (TopLevel.entries.first { it.key == item.key }) {
                            TopLevel.HOME -> HomeRoute
                            TopLevel.DISCOVER -> DiscoverRoute()
                            TopLevel.WATCH -> WatchRoute
                            TopLevel.LIBRARY -> LibraryRoute
                            TopLevel.PROFILE -> ProfileRoute
                        }
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        }
    }
}

private fun androidx.navigation.NavController.openMode(id: String, mode: ConsumptionMode) {
    when (mode) {
        ConsumptionMode.LISTEN -> navigate(ListenRoute(id))
        ConsumptionMode.READ -> navigate(ReaderRoute(id))
        ConsumptionMode.WATCH -> navigate(PlayerRoute(id))
    }
}

@Composable
private fun AppNavHost(navController: androidx.navigation.NavHostController) {
    val reduced = OPTheme.reducedMotion
    val enterSpec = tween<Float>(if (reduced) 0 else OPMotion.standard, easing = OPMotion.easeEnter)
    val exitSpec = tween<Float>(if (reduced) 0 else OPMotion.fast, easing = OPMotion.easeExit)

    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        // One transition for the whole app: a quiet fade with a short vertical settle.
        enterTransition = { fadeIn(enterSpec) + slideInVertically(tween(if (reduced) 0 else OPMotion.standard, easing = OPMotion.easeEnter)) { it / 24 } },
        exitTransition = { fadeOut(exitSpec) },
        popEnterTransition = { fadeIn(enterSpec) },
        popExitTransition = { fadeOut(exitSpec) + slideOutVertically(tween(if (reduced) 0 else OPMotion.fast, easing = OPMotion.easeExit)) { it / 24 } },
    ) {
        composable<HomeRoute> {
            HomeScreen(
                HomeActions(
                    openContent = { navController.navigate(StoryRoute(it)) },
                    openMode = { id, mode -> navController.openMode(id, mode) },
                    openDiscover = { query, taxonId, focus, sort ->
                        navController.navigate(DiscoverRoute(query = query, taxonId = taxonId, focusSearch = focus)) { launchSingleTop = true }
                    },
                ),
            )
        }
        composable<DiscoverRoute> { DiscoverScreen(onOpenContent = { navController.navigate(StoryRoute(it)) }) }
        composable<WatchRoute> {
            WatchScreen(onOpenContent = { navController.navigate(StoryRoute(it)) }, onPlay = { navController.navigate(PlayerRoute(it)) })
        }
        composable<LibraryRoute> { LibraryScreen(libraryActions(navController)) }
        composable<LibrarySectionRoute> { LibrarySectionScreen(onBack = { navController.popBackStack() }, actions = libraryActions(navController)) }
        composable<ProfileRoute> { ProfileScreen(onOpenSection = { navController.navigate(LibrarySectionRoute(it.name)) }) }

        composable<StoryRoute>(
            deepLinks = listOf(
                navDeepLink<StoryRoute>(basePath = "${DeepLinks.SCHEME}://story"),
                navDeepLink<StoryRoute>(basePath = "https://${DeepLinks.HOST}/story"),
            ),
        ) {
            StoryScreen(
                StoryActions(
                    back = { navController.popBackStack() },
                    openMode = { id, mode -> navController.openMode(id, mode) },
                    openComments = { navController.navigate(CommentsRoute(it)) },
                    openContent = { navController.navigate(StoryRoute(it)) },
                ),
            )
        }
        composable<CommentsRoute> { CommentsScreen(onBack = { navController.popBackStack() }) }
        composable<ReaderRoute> { ReaderScreen(onBack = { navController.popBackStack() }) }
        composable<ListenRoute> { ListenScreen(onBack = { navController.popBackStack() }) }
        composable<PlayerRoute> {
            WatchPlayerScreen(
                onBack = { navController.popBackStack() },
                onOpenVideo = { navController.navigate(PlayerRoute(it)) { popUpTo<PlayerRoute> { inclusive = true } } },
            )
        }
    }
}

private fun libraryActions(navController: androidx.navigation.NavHostController) = LibraryActions(
    openContent = { navController.navigate(StoryRoute(it)) },
    openMode = { id, mode -> navController.openMode(id, mode) },
    openReaderAt = { navController.navigate(ReaderRoute(it)) },
    openSection = { navController.navigate(LibrarySectionRoute(it.name)) },
)
