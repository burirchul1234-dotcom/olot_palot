package com.olotpalot.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.olotpalot.core.common.TimeFormat
import com.olotpalot.core.design.components.Artwork
import com.olotpalot.core.design.components.ContentRow
import com.olotpalot.core.design.components.DisplayText
import com.olotpalot.core.design.components.ErrorState
import com.olotpalot.core.design.components.Eyebrow
import com.olotpalot.core.design.components.LoadingBlocks
import com.olotpalot.core.design.components.MetaLine
import com.olotpalot.core.design.components.ModeGlyphs
import com.olotpalot.core.design.components.ProgressRing
import com.olotpalot.core.design.components.SectionHeader
import com.olotpalot.core.design.components.coverScrim
import com.olotpalot.core.design.components.deepen
import com.olotpalot.core.design.components.enterRise
import com.olotpalot.core.design.components.icon
import com.olotpalot.core.design.components.pressScale
import com.olotpalot.core.design.components.rememberStoryTint
import com.olotpalot.core.design.components.soften
import com.olotpalot.core.design.theme.OPShapes
import com.olotpalot.core.design.theme.OPTheme
import com.olotpalot.domain.model.ConsumptionMode
import com.olotpalot.domain.model.ContentItem
import com.olotpalot.domain.model.ContinueItem
import com.olotpalot.domain.model.HomeFeed
import com.olotpalot.domain.model.SortOption
import com.olotpalot.domain.model.Taxon
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val paper = Color(0xFFFBF8F2)

data class HomeActions(
    val openContent: (String) -> Unit,
    val openMode: (String, ConsumptionMode) -> Unit,
    val openDiscover: (query: String?, taxonId: String?, focusSearch: Boolean, sort: SortOption?) -> Unit,
)

@Composable
fun HomeScreen(actions: HomeActions, viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val gutter = OPTheme.spacing.gutter
    LazyColumn(Modifier.fillMaxSize().background(OPTheme.colors.background), contentPadding = PaddingValues(bottom = 24.dp)) {
        when (val s = state) {
            HomeUiState.Loading -> {
                item("masthead") { Masthead(streak = 0, onSearch = { actions.openDiscover(null, null, true, null) }, onPaper = true) }
                item("loading") { LoadingBlocks(Modifier.padding(gutter), rows = 6) }
            }
            is HomeUiState.Error -> {
                item("masthead") { Masthead(streak = 0, onSearch = { actions.openDiscover(null, null, true, null) }, onPaper = true) }
                item("error") { Box(Modifier.padding(gutter)) { ErrorState(s.message, viewModel::retry) } }
            }
            is HomeUiState.Ready -> feedItems(s.feed, actions)
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.feedItems(feed: HomeFeed, actions: HomeActions) {
    var section = 0
    item("covers") {
        CoverCarousel(feed.covers, streak = feed.streak, onOpen = actions.openContent, onMode = actions.openMode,
            onSearch = { actions.openDiscover(null, null, true, null) })
    }
    if (feed.continueItems.isNotEmpty()) {
        item("continue") { ContinueCard(feed.continueItems, Modifier.enterRise(section++), onOpen = { actions.openMode(it.content.id, it.mode) }) }
    }
    feed.today?.let { today ->
        item("today") { TodayBlock(today, feed.streak, Modifier.enterRise(section++), onOpen = { actions.openContent(today.id) }) }
    }
    if (feed.trending.isNotEmpty()) {
        item("trending") { TrendingPosters(feed.trending, Modifier.enterRise(section++), onOpen = actions.openContent, onAll = { actions.openDiscover(null, null, false, SortOption.MOST_VIEWED) }) }
    }
    if (feed.moods.isNotEmpty()) {
        item("moods") { MoodGrid(feed.moods, Modifier.enterRise(section++), onOpen = { actions.openDiscover(null, it.id, false, null) }) }
    }
    if (feed.recommended.isNotEmpty()) {
        item("recommended") { RecommendedGrid(feed.recommended, feed.recommendedReason ?: "Picked for you", Modifier.enterRise(section++), onOpen = actions.openContent) }
    }
    if (feed.recentlyAdded.isNotEmpty()) {
        item("recent-header") {
            SectionHeader("Recently added", Modifier.padding(horizontal = OPTheme.spacing.gutter).padding(top = 32.dp), action = "All", onAction = { actions.openDiscover(null, null, false, SortOption.NEWEST) })
        }
        items(feed.recentlyAdded.size, key = { "recent-" + feed.recentlyAdded[it].id }) { i ->
            val item = feed.recentlyAdded[i]
            Box(Modifier.padding(horizontal = OPTheme.spacing.gutter)) {
                ContentRow(item, onClick = { actions.openContent(item.id) }, meta = TimeFormat.relative(item.publishedAt), showRule = i < feed.recentlyAdded.lastIndex)
            }
        }
    }
}

/** Wordmark, streak and search float over the cover; on paper when there's no cover yet. */
@Composable
private fun Masthead(streak: Int, onSearch: () -> Unit, onPaper: Boolean) {
    val c = OPTheme.colors
    val fg = if (onPaper) c.ink else paper
    Row(
        Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).padding(start = OPTheme.spacing.gutter, end = 10.dp, top = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("Olot Palot", style = OPTheme.type.wordmark, color = fg)
            Text("ওলট পালট", style = OPTheme.type.caption, color = fg.copy(alpha = 0.75f))
        }
        if (streak > 1) {
            Row(
                Modifier.clip(CircleShape).background(fg.copy(alpha = if (onPaper) 0.08f else 0.18f)).padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.LocalFireDepartment, "Day streak", Modifier.size(16.dp), tint = if (onPaper) c.accent else Color(0xFFFFB266))
                Spacer(Modifier.width(4.dp))
                Text("$streak", style = OPTheme.type.label, color = fg)
            }
            Spacer(Modifier.width(6.dp))
        }
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(fg.copy(alpha = if (onPaper) 0.08f else 0.18f)).clickable(role = Role.Button, onClickLabel = "Search", onClick = onSearch),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Outlined.Search, "Search", Modifier.size(20.dp), tint = fg) }
    }
}

/**
 * The front page: edge-to-edge covers you swipe through. Each one is a poster —
 * artwork, tinted scrim, big title, one obvious thing to do.
 */
@Composable
private fun CoverCarousel(covers: List<ContentItem>, streak: Int, onOpen: (String) -> Unit, onMode: (String, ConsumptionMode) -> Unit, onSearch: () -> Unit) {
    val c = OPTheme.colors
    val screenH = LocalConfiguration.current.screenHeightDp
    val height = (screenH * 0.72f).dp.coerceIn(480.dp, 700.dp)
    val pager = rememberPagerState { covers.size }
    Box(Modifier.fillMaxWidth().height(height)) {
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize(), beyondViewportPageCount = 1) { page ->
            CoverPage(covers[page], rank = page, onOpen = { onOpen(covers[page].id) }, onMode = { onMode(covers[page].id, it) })
        }
        Masthead(streak, onSearch, onPaper = false)
        // Segmented indicator: which cover you're on, without stealing attention.
        Row(Modifier.align(Alignment.BottomCenter).padding(horizontal = OPTheme.spacing.gutter, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(covers.size) { i ->
                Box(Modifier.weight(1f).height(2.dp).clip(CircleShape).background(paper.copy(alpha = if (i == pager.currentPage) 0.95f else 0.3f)))
            }
        }
        // Soft hand-off into the paper below.
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(28.dp).background(Brush.verticalGradient(0f to Color.Transparent, 1f to c.background)))
    }
}

@Composable
private fun CoverPage(item: ContentItem, rank: Int, onOpen: () -> Unit, onMode: (ConsumptionMode) -> Unit) {
    val tint by rememberStoryTint(item.heroUrl, item.accentArgb)
    val interaction = remember { MutableInteractionSource() }
    Box(Modifier.fillMaxSize().pressScale(interaction, 0.985f).clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onOpen)) {
        Artwork(item.heroUrl, item.title, Modifier.fillMaxSize(), shape = OPShapes.none, fallbackLetter = item.title)
        Box(Modifier.fillMaxSize().background(coverScrim(tint)))
        Box(Modifier.fillMaxWidth().height(140.dp).background(Brush.verticalGradient(0f to Color(0x66000000), 1f to Color.Transparent)))
        Column(Modifier.align(Alignment.BottomStart).padding(horizontal = OPTheme.spacing.gutter).padding(bottom = 36.dp)) {
            Eyebrow(
                if (item.isFeatured) "Featured  ·  ${item.genres.firstOrNull()?.name ?: item.category.name}" else "Trending  ·  №${rank}",
                color = paper.copy(alpha = 0.85f),
            )
            Spacer(Modifier.height(10.dp))
            DisplayText(item.title, style = OPTheme.type.display.copy(fontSize = 40.sp, lineHeight = 42.sp), color = paper, maxLines = 3)
            item.subtitle?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = OPTheme.type.body.copy(fontStyle = FontStyle.Italic), color = paper.copy(alpha = 0.85f), maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(8.dp))
            Text(item.author.name, style = OPTheme.type.caption, color = paper.copy(alpha = 0.7f))
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val primary = item.primaryMode
                if (primary != null) {
                    val label = when (primary) { ConsumptionMode.LISTEN -> "Listen now"; ConsumptionMode.READ -> "Read now"; ConsumptionMode.WATCH -> "Watch now" }
                    Row(
                        Modifier.height(46.dp).clip(CircleShape).background(paper).clickable(role = Role.Button) { onMode(primary) }.padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.PlayArrow, null, Modifier.size(18.dp), tint = tint.deepen(0.3f))
                        Spacer(Modifier.width(6.dp))
                        Text(label, style = OPTheme.type.label, color = Color(0xFF1B1915))
                        val d = item.durationFor(primary)
                        if (d > 0) Text("  ·  ${TimeFormat.duration(d)}", style = OPTheme.type.caption, color = Color(0xFF6B655B))
                    }
                }
                item.modes.drop(1).forEach { mode ->
                    Spacer(Modifier.width(8.dp))
                    Row(
                        Modifier.height(46.dp).clip(CircleShape).background(paper.copy(alpha = 0.16f)).clickable(role = Role.Button) { onMode(mode) }.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(mode.icon(), mode.label, Modifier.size(18.dp), tint = paper)
                    }
                }
            }
        }
    }
}

private fun ConsumptionMode.verb() = when (this) { ConsumptionMode.LISTEN -> "listening"; ConsumptionMode.READ -> "reading"; ConsumptionMode.WATCH -> "watching" }

@Composable
private fun ContinueCard(items: List<ContinueItem>, modifier: Modifier, onOpen: (ContinueItem) -> Unit) {
    val c = OPTheme.colors
    val first = items.first()
    val tint by rememberStoryTint(first.content.thumbnailUrl, first.content.accentArgb)
    val interaction = remember { MutableInteractionSource() }
    Column(modifier.padding(horizontal = OPTheme.spacing.gutter).padding(top = 18.dp)) {
        Row(
            Modifier.fillMaxWidth().pressScale(interaction).clip(OPShapes.tile).background(tint.soften(c)).clickable(interactionSource = interaction, indication = null, role = Role.Button) { onOpen(first) }.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Artwork(first.content.thumbnailUrl, null, Modifier.size(56.dp).clip(CircleShape), shape = CircleShape, fallbackLetter = first.content.title)
                ProgressRing(first.fraction, size = 66.dp, stroke = 3.dp, color = tint, track = tint.copy(alpha = 0.2f))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Eyebrow("${first.mode.verb().replaceFirstChar { it.uppercase() }}  ·  ${TimeFormat.duration(first.remainingMs)} left", color = tint.deepen(if (c.isDark) 0f else 0.2f))
                Spacer(Modifier.height(4.dp))
                DisplayText(first.content.title, style = OPTheme.type.title, maxLines = 2)
                Text(first.content.author.name, style = OPTheme.type.caption, color = c.inkMuted)
            }
            Spacer(Modifier.width(10.dp))
            Box(Modifier.size(44.dp).clip(CircleShape).background(c.ink), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.PlayArrow, "Resume", Modifier.size(22.dp), tint = c.background)
            }
        }
        if (items.size > 1) {
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items.size - 1, key = { items[it + 1].content.id }) { i ->
                    val ci = items[i + 1]
                    Row(
                        Modifier.clip(CircleShape).background(c.surface).clickable(role = Role.Button) { onOpen(ci) }.padding(start = 6.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Artwork(ci.content.thumbnailUrl, null, Modifier.size(26.dp).clip(CircleShape), shape = CircleShape, fallbackLetter = ci.content.title)
                            ProgressRing(ci.fraction, size = 32.dp, stroke = 2.dp)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(ci.content.title, style = OPTheme.type.caption, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(120.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TodayBlock(item: ContentItem, streak: Int, modifier: Modifier, onOpen: () -> Unit) {
    val c = OPTheme.colors
    val tint by rememberStoryTint(item.thumbnailUrl, item.accentArgb)
    val date = remember { LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.ENGLISH)) }
    val interaction = remember { MutableInteractionSource() }
    Column(modifier.padding(horizontal = OPTheme.spacing.gutter).padding(top = 28.dp)) {
        SectionHeader("Today's story  ·  $date")
        Row(
            Modifier.fillMaxWidth().pressScale(interaction).clip(OPShapes.tile)
                .background(Brush.horizontalGradient(listOf(tint.soften(c, 0.78f), tint.soften(c, 0.9f))))
                .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onOpen).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Artwork(item.thumbnailUrl, item.title, Modifier.width(88.dp).height(118.dp), shape = OPShapes.control, fallbackLetter = item.title)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                DisplayText(item.title, style = OPTheme.type.title, maxLines = 2)
                Spacer(Modifier.height(2.dp))
                Text(item.author.name, style = OPTheme.type.caption, color = c.inkMuted)
                Spacer(Modifier.height(8.dp))
                Text(item.description.lineSequence().first(), style = OPTheme.type.bodySmall, color = c.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ModeGlyphs(item, tint = tint.deepen(0.2f))
                    Spacer(Modifier.width(8.dp))
                    Text(if (streak > 1) "Day $streak — keep the thread going" else "Begin today's story", style = OPTheme.type.caption, color = tint.deepen(if (c.isDark) 0f else 0.25f))
                }
            }
        }
    }
}

@Composable
private fun TrendingPosters(items: List<ContentItem>, modifier: Modifier, onOpen: (String) -> Unit, onAll: () -> Unit) {
    val c = OPTheme.colors
    Column(modifier.padding(top = 32.dp)) {
        SectionHeader("Trending now", Modifier.padding(horizontal = OPTheme.spacing.gutter), action = "All", onAction = onAll)
        LazyRow(contentPadding = PaddingValues(horizontal = OPTheme.spacing.gutter), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(items.size, key = { items[it].id }) { i ->
                val item = items[i]
                val tint by rememberStoryTint(item.thumbnailUrl, item.accentArgb)
                val interaction = remember { MutableInteractionSource() }
                Column(Modifier.width(150.dp).pressScale(interaction).clickable(interactionSource = interaction, indication = null, role = Role.Button) { onOpen(item.id) }) {
                    Box {
                        Artwork(item.thumbnailUrl, item.title, Modifier.fillMaxWidth().aspectRatio(3f / 4f), shape = OPShapes.tile, fallbackLetter = item.title)
                        Box(Modifier.padding(8.dp).size(30.dp).clip(CircleShape).background(tint), contentAlignment = Alignment.Center) {
                            Text("${i + 1}", style = OPTheme.type.label, color = paper)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    DisplayText(item.title, style = OPTheme.type.titleSmall, maxLines = 2)
                    Spacer(Modifier.height(2.dp))
                    MetaLine(listOf(item.author.name, "${TimeFormat.compactCount(item.stats.views)} views"), color = c.inkFaint)
                }
            }
        }
    }
}

@Composable
private fun MoodGrid(moods: List<Taxon>, modifier: Modifier, onOpen: (Taxon) -> Unit) {
    Column(modifier.padding(horizontal = OPTheme.spacing.gutter).padding(top = 32.dp)) {
        SectionHeader("Moods")
        moods.chunked(2).forEachIndexed { r, pair ->
            if (r > 0) Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEach { mood -> MoodTile(mood.name, MoodColors.of(mood.id), selected = false, modifier = Modifier.weight(1f), height = 60.dp) { onOpen(mood) } }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun RecommendedGrid(items: List<ContentItem>, reason: String, modifier: Modifier, onOpen: (String) -> Unit) {
    val c = OPTheme.colors
    Column(modifier.padding(horizontal = OPTheme.spacing.gutter).padding(top = 32.dp)) {
        SectionHeader(reason)
        items.chunked(2).forEachIndexed { rowIndex, pair ->
            if (rowIndex > 0) Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                pair.forEach { item ->
                    val interaction = remember { MutableInteractionSource() }
                    Column(Modifier.weight(1f).pressScale(interaction).clickable(interactionSource = interaction, indication = null, role = Role.Button) { onOpen(item.id) }) {
                        Artwork(item.thumbnailUrl, item.title, Modifier.fillMaxWidth().aspectRatio(4f / 5f), shape = OPShapes.tile, fallbackLetter = item.title)
                        Spacer(Modifier.height(8.dp))
                        DisplayText(item.title, style = OPTheme.type.titleSmall, maxLines = 2)
                        Spacer(Modifier.height(2.dp))
                        Text(item.author.name, style = OPTheme.type.caption, color = c.inkMuted, maxLines = 1)
                        Spacer(Modifier.height(4.dp))
                        ModeGlyphs(item)
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Suppress("unused")
private val keepImports = listOf(Modifier.fillMaxHeight())
