package com.olotpalot.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.olotpalot.core.common.TimeFormat
import com.olotpalot.core.design.components.Artwork
import com.olotpalot.core.design.components.CompactSearchBar
import com.olotpalot.core.design.components.ContentRow
import com.olotpalot.core.design.components.DisplayText
import com.olotpalot.core.design.components.ErrorState
import com.olotpalot.core.design.components.Eyebrow
import com.olotpalot.core.design.components.LoadingBlocks
import com.olotpalot.core.design.components.MetaLine
import com.olotpalot.core.design.components.ModeGlyphs
import com.olotpalot.core.design.components.ProgressHairline
import com.olotpalot.core.design.components.Rule
import com.olotpalot.core.design.components.SectionHeader
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

/** Callbacks are the only way out of Home; it knows nothing about routes. */
data class HomeActions(
    val openContent: (String) -> Unit,
    val openMode: (String, ConsumptionMode) -> Unit,
    val openDiscover: (query: String?, taxonId: String?, focusSearch: Boolean, sort: SortOption?) -> Unit,
)

@Composable
fun HomeScreen(actions: HomeActions, viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeContent(state, actions, onRetry = viewModel::retry)
}

@Composable
private fun HomeContent(state: HomeUiState, actions: HomeActions, onRetry: () -> Unit) {
    val gutter = OPTheme.spacing.gutter
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(OPTheme.colors.background),
        contentPadding = PaddingValues(start = gutter, end = gutter, bottom = 24.dp),
    ) {
        item(key = "masthead") { Masthead() }
        item(key = "search") { SearchEntry(onOpen = { actions.openDiscover(null, null, true, null) }) }

        when (state) {
            HomeUiState.Loading -> item(key = "loading") { LoadingBlocks(Modifier.padding(top = 24.dp), rows = 6) }
            is HomeUiState.Error -> item(key = "error") { ErrorState(state.message, onRetry) }
            is HomeUiState.Ready -> feedItems(state.feed, actions)
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.feedItems(feed: HomeFeed, actions: HomeActions) {
    val stageIsContinue = feed.continueItems.isNotEmpty()

    if (stageIsContinue) {
        item(key = "continue") {
            ContinueStage(
                items = feed.continueItems,
                onOpen = { actions.openMode(it.content.id, it.mode) },
                onOpenContent = actions.openContent,
            )
        }
    }

    feed.featured?.let { featured ->
        item(key = "featured") {
            FeaturedStage(featured, compact = stageIsContinue, onOpen = { actions.openContent(featured.id) })
        }
    }

    if (feed.trending.isNotEmpty()) {
        item(key = "trending") {
            TrendingBlock(feed.trending, onOpen = actions.openContent, onAll = { actions.openDiscover(null, null, false, SortOption.MOST_VIEWED) })
        }
    }

    if (feed.moods.isNotEmpty()) {
        item(key = "moods") { MoodIndex(feed.moods, onOpen = { actions.openDiscover(null, it.id, false, null) }) }
    }

    if (feed.recommended.isNotEmpty()) {
        item(key = "recommended") {
            RecommendedGrid(feed.recommended, feed.recommendedReason ?: "Recommended", onOpen = actions.openContent)
        }
    }

    if (feed.recentlyAdded.isNotEmpty()) {
        item(key = "recent-header") {
            SectionHeader("Recently added", Modifier.padding(top = 32.dp), action = "All", onAction = { actions.openDiscover(null, null, false, SortOption.NEWEST) })
        }
        items(feed.recentlyAdded.size, key = { "recent-" + feed.recentlyAdded[it].id }) { i ->
            val item = feed.recentlyAdded[i]
            ContentRow(item, onClick = { actions.openContent(item.id) }, meta = TimeFormat.relative(item.publishedAt), showRule = i < feed.recentlyAdded.lastIndex)
        }
    }
}

@Composable
private fun Masthead() {
    val c = OPTheme.colors
    val date = remember { LocalDate.now().format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH)) }
    Row(
        Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).padding(top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column {
            Text("Olot Palot", style = OPTheme.type.wordmark, color = c.ink)
            Text("ওলট পালট  ·  listen, read, watch", style = OPTheme.type.caption, color = c.inkMuted)
        }
        Spacer(Modifier.weight(1f))
        Eyebrow(date, color = c.inkFaint)
    }
}

/** Looks like the real search bar, acts as a doorway into Discover (one search engine, one place). */
@Composable
private fun SearchEntry(onOpen: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
        CompactSearchBar(value = "", onValueChange = {}, onSearch = {}, onFilter = onOpen)
        Box(Modifier.matchParentSize().clip(OPShapes.control).clickable(role = Role.Button, onClickLabel = "Search", onClick = onOpen))
    }
}

private fun ConsumptionMode.verb() = when (this) {
    ConsumptionMode.LISTEN -> "listening"
    ConsumptionMode.READ -> "reading"
    ConsumptionMode.WATCH -> "watching"
}

@Composable
private fun ContinueStage(items: List<ContinueItem>, onOpen: (ContinueItem) -> Unit, onOpenContent: (String) -> Unit) {
    val c = OPTheme.colors
    val first = items.first()
    Column(Modifier.fillMaxWidth().padding(top = 20.dp)) {
        Row(
            Modifier.fillMaxWidth().clip(OPShapes.tile).clickable(role = Role.Button) { onOpen(first) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Artwork(first.content.thumbnailUrl, null, Modifier.width(92.dp).height(124.dp), fallbackLetter = first.content.title)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Eyebrow("Continue ${first.mode.verb()}  ·  ${TimeFormat.duration(first.remainingMs)} left", color = c.accent)
                Spacer(Modifier.height(6.dp))
                DisplayText(first.content.title, style = OPTheme.type.headline, maxLines = 3)
                Spacer(Modifier.height(4.dp))
                Text(first.content.author.name, style = OPTheme.type.caption, color = c.inkMuted)
                Spacer(Modifier.height(12.dp))
                ProgressHairline(first.fraction)
            }
        }
        if (items.size > 1) {
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items.drop(1).take(3).forEach { item ->
                    Row(
                        Modifier.weight(1f).clip(OPShapes.control).clickable(role = Role.Button) { onOpen(item) }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Artwork(item.content.thumbnailUrl, null, Modifier.width(30.dp).height(40.dp), fallbackLetter = item.content.title)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            DisplayText(item.content.title, style = OPTheme.type.titleSmall.copy(fontSize = 13.sp, lineHeight = 15.sp), maxLines = 2)
                            Spacer(Modifier.height(4.dp))
                            ProgressHairline(item.fraction, Modifier.width(44.dp))
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Rule()
    }
}

@Composable
private fun FeaturedStage(item: ContentItem, compact: Boolean, onOpen: () -> Unit) {
    val c = OPTheme.colors
    Column(Modifier.fillMaxWidth().padding(top = if (compact) 24.dp else 16.dp).clickable(role = Role.Button, onClick = onOpen)) {
        Artwork(item.heroUrl, item.title, Modifier.fillMaxWidth().aspectRatio(if (compact) 16f / 9f else 4f / 3f), shape = OPShapes.thumb, fallbackLetter = item.title)
        Spacer(Modifier.height(14.dp))
        Eyebrow("Featured  ·  ${item.genres.firstOrNull()?.name ?: item.category.name}", color = c.accent)
        Spacer(Modifier.height(8.dp))
        DisplayText(item.title, style = if (compact) OPTheme.type.headline else OPTheme.type.display)
        item.subtitle?.let {
            Spacer(Modifier.height(6.dp))
            Text(it, style = OPTheme.type.body.copy(fontStyle = FontStyle.Italic), color = c.inkMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(10.dp))
        MetaLine(
            listOf(item.author.name) + item.modes.map { m ->
                val d = item.durationFor(m); if (d > 0) "${m.label} ${TimeFormat.duration(d)}" else m.label
            },
            maxLines = 2,
        )
    }
}

@Composable
private fun TrendingBlock(items: List<ContentItem>, onOpen: (String) -> Unit, onAll: () -> Unit) {
    val c = OPTheme.colors
    Column(Modifier.fillMaxWidth().padding(top = 32.dp)) {
        SectionHeader("Trending now", action = "All", onAction = onAll)
        val lead = items[0]
        val side = items.drop(1).take(2)
        Row(Modifier.fillMaxWidth().height(236.dp)) {
            MosaicTile(lead, rank = 1, modifier = Modifier.weight(1.5f).fillMaxHeight(), large = true, onOpen = { onOpen(lead.id) })
            if (side.isNotEmpty()) {
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    side.forEachIndexed { i, item ->
                        MosaicTile(item, rank = i + 2, modifier = Modifier.weight(1f).fillMaxWidth(), large = false, onOpen = { onOpen(item.id) })
                    }
                }
            }
        }
        val rest = items.drop(3)
        if (rest.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            rest.forEachIndexed { i, item ->
                Row(
                    Modifier.fillMaxWidth().clickable(role = Role.Button) { onOpen(item.id) }.padding(vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        (i + 4).toString().padStart(2, '0'),
                        style = OPTheme.type.numeral.copy(fontSize = 22.sp, lineHeight = 22.sp),
                        color = c.inkFaint,
                        modifier = Modifier.width(40.dp),
                    )
                    Column(Modifier.weight(1f)) {
                        DisplayText(item.title, style = OPTheme.type.titleSmall, maxLines = 1)
                        Spacer(Modifier.height(2.dp))
                        MetaLine(listOf(item.author.name, "${TimeFormat.compactCount(item.stats.views)} views"))
                    }
                    Spacer(Modifier.width(10.dp))
                    ModeGlyphs(item)
                }
                if (i < rest.lastIndex) Rule()
            }
        }
    }
}

@Composable
private fun MosaicTile(item: ContentItem, rank: Int, modifier: Modifier, large: Boolean, onOpen: () -> Unit) {
    Box(modifier.clip(OPShapes.thumb).clickable(role = Role.Button, onClick = onOpen)) {
        Artwork(if (large) item.heroUrl else item.thumbnailUrl, item.title, Modifier.matchParentSize(), shape = OPShapes.none, fallbackLetter = item.title)
        Box(
            Modifier.matchParentSize().background(
                Brush.verticalGradient(0.35f to Color.Transparent, 1f to Color(0xCC14110D)),
            ),
        )
        Column(Modifier.align(Alignment.BottomStart).padding(if (large) 14.dp else 10.dp)) {
            Text(
                rank.toString().padStart(2, '0'),
                style = OPTheme.type.numeral.copy(fontSize = if (large) 30.sp else 18.sp, lineHeight = if (large) 30.sp else 18.sp),
                color = Color(0xFFF3EEE4).copy(alpha = 0.8f),
            )
            Spacer(Modifier.height(4.dp))
            DisplayText(
                item.title,
                style = if (large) OPTheme.type.title else OPTheme.type.titleSmall.copy(fontSize = 14.sp, lineHeight = 17.sp),
                color = Color(0xFFFBF8F2),
                maxLines = 2,
            )
        }
    }
}

@Composable
private fun MoodIndex(moods: List<Taxon>, onOpen: (Taxon) -> Unit) {
    val c = OPTheme.colors
    Column(Modifier.fillMaxWidth().padding(top = 32.dp)) {
        SectionHeader("Browse by mood")
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(0.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            moods.forEachIndexed { i, mood ->
                if (i > 0) {
                    Text("  /  ", style = OPTheme.type.title.copy(fontSize = 22.sp, lineHeight = 30.sp), color = c.inkFaint)
                }
                Text(
                    mood.name,
                    style = OPTheme.type.title.copy(fontSize = 22.sp, lineHeight = 30.sp),
                    color = c.ink,
                    modifier = Modifier.clip(OPShapes.control).clickable(role = Role.Button) { onOpen(mood) }.padding(horizontal = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun RecommendedGrid(items: List<ContentItem>, reason: String, onOpen: (String) -> Unit) {
    val c = OPTheme.colors
    Column(Modifier.fillMaxWidth().padding(top = 32.dp)) {
        SectionHeader(reason)
        items.chunked(2).forEachIndexed { rowIndex, pair ->
            if (rowIndex > 0) Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                pair.forEach { item ->
                    Column(Modifier.weight(1f).clip(OPShapes.control).clickable(role = Role.Button) { onOpen(item.id) }) {
                        Artwork(item.thumbnailUrl, item.title, Modifier.fillMaxWidth().aspectRatio(4f / 5f), fallbackLetter = item.title)
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
