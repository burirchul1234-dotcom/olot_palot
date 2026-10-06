package com.olotpalot.feature.watch

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ThumbUpOffAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.olotpalot.core.common.Clock
import com.olotpalot.core.common.TimeFormat
import com.olotpalot.core.design.components.Artwork
import com.olotpalot.core.design.components.DisplayText
import com.olotpalot.core.design.components.EmptyState
import com.olotpalot.core.design.components.Eyebrow
import com.olotpalot.core.design.components.LoadingBlocks
import com.olotpalot.core.design.components.ProgressRing
import com.olotpalot.core.design.components.coverScrim
import com.olotpalot.core.design.components.rememberStoryTint
import com.olotpalot.core.design.theme.OPShapes
import com.olotpalot.core.design.theme.OPTheme
import com.olotpalot.domain.model.ConsumptionMode
import com.olotpalot.domain.model.ContentItem
import com.olotpalot.domain.model.UserContentState
import com.olotpalot.domain.ranking.RankingEngine
import com.olotpalot.domain.repository.ContentRepository
import com.olotpalot.domain.repository.EngagementRepository
import com.olotpalot.domain.usecase.ContentLinks
import com.olotpalot.domain.usecase.RecordShareUseCase
import com.olotpalot.domain.usecase.ToggleLikeUseCase
import com.olotpalot.domain.usecase.ToggleSavedForLaterUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WatchUiState(
    val videos: List<ContentItem> = emptyList(),
    val states: Map<String, UserContentState> = emptyMap(),
    val loaded: Boolean = false,
)

@HiltViewModel
class WatchViewModel @Inject constructor(
    content: ContentRepository,
    engagement: EngagementRepository,
    ranking: RankingEngine,
    clock: Clock,
    private val toggleLike: ToggleLikeUseCase,
    private val toggleSaved: ToggleSavedForLaterUseCase,
    private val recordShare: RecordShareUseCase,
) : ViewModel() {
    private val _share = Channel<Pair<String, String>>(Channel.BUFFERED)
    val share = _share.receiveAsFlow()

    val state: StateFlow<WatchUiState> = combine(content.observeAll(), engagement.observeStates()) { all, states ->
        // In-progress videos first, then by trending — the feed opens on what you'd most want.
        val videos = all.filter { it.isPublished && it.supports(ConsumptionMode.WATCH) }
            .sortedWith(compareByDescending<ContentItem> { (states[it.id]?.video?.fraction ?: 0f) in 0.02f..0.985f }.thenByDescending { ranking.trendingScore(it, clock.now()) })
        WatchUiState(videos = videos, states = states, loaded = all.isNotEmpty())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WatchUiState())

    fun like(item: ContentItem) { val s = state.value.states[item.id]; viewModelScope.launch { toggleLike(item.id, s?.isLiked == true) } }
    fun save(item: ContentItem) { val s = state.value.states[item.id]; viewModelScope.launch { toggleSaved(item.id, s?.isSavedForLater == true) } }
    fun share(item: ContentItem) = viewModelScope.launch { recordShare(item.id); _share.send(item.title to "${item.title} by ${item.author.name}\n${ContentLinks.webLink(item.id)}") }
}

/**
 * Watch is a vertical, full-screen feed: one film per page, swipe up for the next.
 * Same type system as the rest of the product — posters, not a social timeline.
 */
@Composable
fun WatchScreen(onOpenContent: (String) -> Unit, onPlay: (String) -> Unit, viewModel: WatchViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = OPTheme.colors
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.share.collect { (subject, text) ->
            context.startActivity(android.content.Intent.createChooser(android.content.Intent(android.content.Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(android.content.Intent.EXTRA_SUBJECT, subject); putExtra(android.content.Intent.EXTRA_TEXT, text) }, "Share"))
        }
    }

    when {
        !state.loaded -> Column(Modifier.fillMaxSize().background(c.background).windowInsetsPadding(WindowInsets.statusBars).padding(OPTheme.spacing.gutter)) { LoadingBlocks() }
        state.videos.isEmpty() -> Box(Modifier.fillMaxSize().background(c.background).padding(OPTheme.spacing.gutter)) { EmptyState("Nothing to watch yet", "Films will appear here as they are published.") }
        else -> {
            val pager = rememberPagerState { state.videos.size }
            Box(Modifier.fillMaxSize().background(Color(0xFF14110D))) {
                VerticalPager(state = pager, modifier = Modifier.fillMaxSize(), beyondViewportPageCount = 1) { page ->
                    val item = state.videos[page]
                    WatchPage(
                        item = item,
                        user = state.states[item.id],
                        index = page, count = state.videos.size,
                        onPlay = { onPlay(item.id) }, onOpen = { onOpenContent(item.id) },
                        onLike = { viewModel.like(item) }, onSave = { viewModel.save(item) }, onShare = { viewModel.share(item) },
                    )
                }
            }
        }
    }
}

@Composable
private fun WatchPage(
    item: ContentItem, user: UserContentState?, index: Int, count: Int,
    onPlay: () -> Unit, onOpen: () -> Unit, onLike: () -> Unit, onSave: () -> Unit, onShare: () -> Unit,
) {
    val paper = Color(0xFFFBF8F2)
    val tint by rememberStoryTint(item.videoAsset?.thumbnailUrl ?: item.heroUrl, item.accentArgb)
    val haptics = LocalHapticFeedback.current
    val fraction = user?.video?.fraction ?: 0f
    val inProgress = fraction in 0.02f..0.985f

    Box(Modifier.fillMaxSize()) {
        Artwork(item.videoAsset?.thumbnailUrl ?: item.heroUrl, item.title, Modifier.fillMaxSize(), shape = OPShapes.none, fallbackLetter = item.title)
        Box(Modifier.fillMaxSize().background(coverScrim(tint)))
        Box(Modifier.fillMaxWidth().height(160.dp).background(Brush.verticalGradient(0f to Color(0x80000000), 1f to Color.Transparent)))

        Row(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).padding(horizontal = OPTheme.spacing.gutter, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            DisplayText("Watch", style = OPTheme.type.title, color = paper)
            Spacer(Modifier.weight(1f))
            Text("${index + 1} / $count", style = OPTheme.type.caption, color = paper.copy(alpha = 0.7f))
        }

        // Centre: one big play. The frame *is* the button.
        Box(
            Modifier.align(Alignment.Center).size(76.dp).clip(CircleShape).background(paper.copy(alpha = 0.92f)).clickable(role = Role.Button, onClickLabel = "Play", onClick = onPlay),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.PlayArrow, null, Modifier.size(38.dp), tint = Color(0xFF1B1915))
            if (inProgress) ProgressRing(fraction, size = 76.dp, stroke = 3.dp, color = tint, track = Color.Transparent)
        }

        // Right rail: engagement, vertical, with counts.
        Column(Modifier.align(Alignment.BottomEnd).padding(end = 10.dp, bottom = 150.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            RailAction(if (user?.isLiked == true) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUpOffAlt, TimeFormat.compactCount(item.stats.likes), active = user?.isLiked == true, tint = tint) { haptics.performHapticFeedback(HapticFeedbackType.LongPress); onLike() }
            RailAction(if (user?.isSavedForLater == true) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder, "Later", active = user?.isSavedForLater == true, tint = tint) { haptics.performHapticFeedback(HapticFeedbackType.LongPress); onSave() }
            RailAction(Icons.Outlined.Share, "Share", active = false, tint = tint, onClick = onShare)
        }

        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(start = OPTheme.spacing.gutter, end = 76.dp, bottom = 26.dp)) {
            Eyebrow("${item.category.name}  ·  ${TimeFormat.duration(item.videoDurationMs)}${if (inProgress) "  ·  ${TimeFormat.duration(user?.video?.remainingMs ?: 0)} left" else ""}", color = paper.copy(alpha = 0.85f))
            Spacer(Modifier.height(8.dp))
            DisplayText(item.title, style = OPTheme.type.display.copy(fontSize = 34.sp, lineHeight = 36.sp), color = paper, maxLines = 3, modifier = Modifier.clickable(role = Role.Button, onClickLabel = "Open story", onClick = onOpen))
            item.subtitle?.let {
                Spacer(Modifier.height(6.dp))
                Text(it, style = OPTheme.type.body.copy(fontStyle = FontStyle.Italic), color = paper.copy(alpha = 0.8f), maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(6.dp))
            Text("${item.author.name}  ·  ${TimeFormat.compactCount(item.stats.views)} views", style = OPTheme.type.caption, color = paper.copy(alpha = 0.7f))
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    Modifier.height(44.dp).clip(CircleShape).background(paper).clickable(role = Role.Button, onClick = onPlay).padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.PlayArrow, null, Modifier.size(18.dp), tint = Color(0xFF1B1915))
                    Spacer(Modifier.width(6.dp))
                    Text(if (inProgress) "Continue" else "Watch now", style = OPTheme.type.label, color = Color(0xFF1B1915))
                }
                Spacer(Modifier.width(10.dp))
                Text("Story", style = OPTheme.type.label, color = paper, modifier = Modifier.clip(CircleShape).background(paper.copy(alpha = 0.16f)).clickable(role = Role.Button, onClick = onOpen).padding(horizontal = 16.dp, vertical = 12.dp))
            }
            if (index < count - 1) {
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.KeyboardArrowUp, null, Modifier.size(16.dp), tint = paper.copy(alpha = 0.6f))
                    Text("Swipe up for the next film", style = OPTheme.type.caption, color = paper.copy(alpha = 0.6f))
                }
            }
        }
    }
}

@Composable
private fun RailAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, active: Boolean, tint: Color, onClick: () -> Unit) {
    val paper = Color(0xFFFBF8F2)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(46.dp).clip(CircleShape).background(if (active) paper else Color(0x33FFFFFF)).clickable(role = Role.Button, onClickLabel = label, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, Modifier.size(22.dp), tint = if (active) tint else paper) }
        Spacer(Modifier.height(4.dp))
        Text(label, style = OPTheme.type.caption, color = paper.copy(alpha = 0.85f))
    }
}
