package com.olotpalot.feature.watch

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.olotpalot.core.common.Clock
import com.olotpalot.core.common.TimeFormat
import com.olotpalot.core.design.components.Artwork
import com.olotpalot.core.design.components.ContentRow
import com.olotpalot.core.design.components.DisplayText
import com.olotpalot.core.design.components.EmptyState
import com.olotpalot.core.design.components.Eyebrow
import com.olotpalot.core.design.components.LoadingBlocks
import com.olotpalot.core.design.components.MetaLine
import com.olotpalot.core.design.components.ProgressHairline
import com.olotpalot.core.design.components.SectionHeader
import com.olotpalot.core.design.theme.OPShapes
import com.olotpalot.core.design.theme.OPTheme
import com.olotpalot.domain.model.ConsumptionMode
import com.olotpalot.domain.model.ContentItem
import com.olotpalot.domain.model.ContinueItem
import com.olotpalot.domain.ranking.RankingEngine
import com.olotpalot.domain.repository.ContentRepository
import com.olotpalot.domain.repository.ProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class WatchUiState(
    val continueWatching: List<ContinueItem> = emptyList(),
    val featured: ContentItem? = null,
    val videos: List<ContentItem> = emptyList(),
    val loaded: Boolean = false,
)

@HiltViewModel
class WatchViewModel @Inject constructor(
    content: ContentRepository,
    progress: ProgressRepository,
    ranking: RankingEngine,
    clock: Clock,
) : ViewModel() {
    val state: StateFlow<WatchUiState> = combine(content.observeAll(), progress.observeContinue(20)) { all, cont ->
        val videos = all.filter { it.isPublished && it.supports(ConsumptionMode.WATCH) }.sortedByDescending { ranking.trendingScore(it, clock.now()) }
        WatchUiState(
            continueWatching = cont.filter { it.mode == ConsumptionMode.WATCH }.take(3),
            featured = videos.firstOrNull(),
            videos = videos.drop(1),
            loaded = all.isNotEmpty(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WatchUiState())
}

@Composable
fun WatchScreen(onOpenContent: (String) -> Unit, onPlay: (String) -> Unit, viewModel: WatchViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = OPTheme.colors
    val gutter = OPTheme.spacing.gutter

    LazyColumn(
        Modifier.fillMaxSize().background(c.background),
        contentPadding = PaddingValues(start = gutter, end = gutter, bottom = 24.dp),
    ) {
        item("head") {
            Column(Modifier.windowInsetsPadding(WindowInsets.statusBars).padding(top = 14.dp, bottom = 12.dp)) {
                DisplayText("Watch", style = OPTheme.type.headline)
                Text("Short films and stories with pictures", style = OPTheme.type.caption, color = c.inkMuted)
            }
        }
        if (!state.loaded) {
            item("loading") { LoadingBlocks() }
            return@LazyColumn
        }
        if (state.continueWatching.isNotEmpty()) {
            item("cont-header") { SectionHeader("Continue watching") }
            items(state.continueWatching.size, key = { "cont-" + state.continueWatching[it].content.id }) { i ->
                val ci = state.continueWatching[i]
                ContentRow(ci.content, onClick = { onPlay(ci.content.id) }, meta = "${TimeFormat.duration(ci.remainingMs)} left", progress = ci.fraction, showRule = i < state.continueWatching.lastIndex)
            }
            item("cont-gap") { Spacer(Modifier.height(20.dp)) }
        }
        state.featured?.let { f ->
            item("featured") { VideoFrame(f, large = true, onPlay = { onPlay(f.id) }, onOpen = { onOpenContent(f.id) }) }
        }
        if (state.videos.isEmpty() && state.featured == null) {
            item("empty") { EmptyState("Nothing to watch yet", "Videos will appear here as they are published.") }
        }
        items(state.videos.size, key = { state.videos[it].id }) { i ->
            val v = state.videos[i]
            VideoFrame(v, large = false, onPlay = { onPlay(v.id) }, onOpen = { onOpenContent(v.id) }, modifier = Modifier.padding(top = 24.dp))
        }
    }
}

@Composable
private fun VideoFrame(item: ContentItem, large: Boolean, onPlay: () -> Unit, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val c = OPTheme.colors
    val paper = Color(0xFFFBF8F2)
    Column(modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(OPShapes.thumb).clickable(role = Role.Button, onClickLabel = "Play", onClick = onPlay)) {
            Artwork(item.videoAsset?.thumbnailUrl ?: item.heroUrl, item.title, Modifier.matchParentSize(), shape = OPShapes.none, fallbackLetter = item.title)
            Box(Modifier.matchParentSize().background(Brush.verticalGradient(0.5f to Color.Transparent, 1f to Color(0xB314110D))))
            Box(Modifier.align(Alignment.Center).size(if (large) 56.dp else 44.dp).clip(CircleShape).background(paper.copy(alpha = 0.92f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.PlayArrow, null, Modifier.size(if (large) 30.dp else 24.dp), tint = Color(0xFF1B1915))
            }
            Text(
                TimeFormat.duration(item.videoDurationMs),
                style = OPTheme.type.caption, color = paper,
                modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp),
            )
            if (large) Eyebrow("Featured", Modifier.align(Alignment.TopStart).padding(12.dp), color = paper.copy(alpha = 0.85f))
        }
        Column(Modifier.fillMaxWidth().clickable(role = Role.Button, onClickLabel = "Open story", onClick = onOpen).padding(top = 10.dp)) {
            DisplayText(item.title, style = if (large) OPTheme.type.headline else OPTheme.type.title, maxLines = 2)
            Spacer(Modifier.height(4.dp))
            MetaLine(listOf(item.author.name, "${TimeFormat.compactCount(item.stats.views)} views", TimeFormat.relative(item.publishedAt)))
        }
    }
}
