package com.olotpalot.feature.story

import android.content.Intent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ThumbUpOffAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.olotpalot.core.common.TimeFormat
import com.olotpalot.core.common.dropFirstGrapheme
import com.olotpalot.core.common.firstGrapheme
import com.olotpalot.core.design.components.Artwork
import com.olotpalot.core.design.components.ContentRow
import com.olotpalot.core.design.components.DisplayText
import com.olotpalot.core.design.components.EmptyState
import com.olotpalot.core.design.components.Eyebrow
import com.olotpalot.core.design.components.IconAction
import com.olotpalot.core.design.components.IconButtonPlain
import com.olotpalot.core.design.components.LoadingBlocks
import com.olotpalot.core.design.components.MetaLine
import com.olotpalot.core.design.components.ModeButton
import com.olotpalot.core.design.components.PrimaryButton
import com.olotpalot.core.design.components.ProgressHairline
import com.olotpalot.core.design.components.RatingStars
import com.olotpalot.core.design.components.Rule
import com.olotpalot.core.design.components.SectionHeader
import com.olotpalot.core.design.components.TextAction
import com.olotpalot.core.design.components.icon
import com.olotpalot.core.design.theme.OPFonts
import com.olotpalot.core.design.theme.OPShapes
import com.olotpalot.core.design.theme.OPTheme
import com.olotpalot.domain.model.AccessDecision
import com.olotpalot.domain.model.Comment
import com.olotpalot.domain.model.ConsumptionMode
import com.olotpalot.domain.model.ContentItem
import com.olotpalot.domain.model.UserContentState

data class StoryActions(
    val back: () -> Unit,
    val openMode: (String, ConsumptionMode) -> Unit,
    val openComments: (String) -> Unit,
    val openContent: (String) -> Unit,
)

@Composable
fun StoryScreen(actions: StoryActions, viewModel: StoryViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { e ->
            when (e) {
                is StoryEvent.Message -> snackbar.showSnackbar(e.text)
                is StoryEvent.Unlocked -> actions.openMode(viewModel.contentId, e.mode)
                is StoryEvent.Share -> {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, e.subject)
                        putExtra(Intent.EXTRA_TEXT, e.text)
                    }
                    context.startActivity(Intent.createChooser(send, "Share story"))
                }
            }
        }
    }

    Box(Modifier.fillMaxSize().background(OPTheme.colors.background)) {
        when {
            state.notFound -> NotFound(actions.back)
            state.detail == null -> Column(Modifier.padding(OPTheme.spacing.gutter).windowInsetsPadding(WindowInsets.statusBars)) { LoadingBlocks() }
            else -> StoryContent(state, viewModel, actions)
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }

    state.rewardSheetMode?.let { mode ->
        RewardSheet(mode, inProgress = state.rewardInProgress, onWatch = viewModel::watchRewardedAd, onDismiss = viewModel::dismissReward)
    }
}

@Composable
private fun NotFound(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars).padding(OPTheme.spacing.gutter)) {
        IconButtonPlain(Icons.AutoMirrored.Outlined.ArrowBack, "Back", onBack)
        EmptyState("This story isn't here", "The link may be old, or the story was taken down.", action = "Go back", onAction = onBack)
    }
}

@Composable
private fun StoryContent(state: StoryUiState, vm: StoryViewModel, actions: StoryActions) {
    val detail = state.detail ?: return
    val item = detail.item
    val user = detail.state
    val gutter = OPTheme.spacing.gutter
    val c = OPTheme.colors

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
        item("hero") { Hero(item, state.access, actions.back) }

        item("meta") {
            Column(Modifier.padding(horizontal = gutter).padding(top = 16.dp)) {
                MetaLine(
                    listOf(item.author.name, item.language.nativeLabel, TimeFormat.relative(item.publishedAt), "${TimeFormat.compactCount(item.stats.views)} views"),
                )
                Spacer(Modifier.height(8.dp))
                RatingStars(item.stats.ratingAverage, count = item.stats.ratingCount)
            }
        }

        item("modes") {
            ModeRow(item, user, state.access, onMode = { mode -> vm.requestMode(mode) { actions.openMode(item.id, it) } })
        }

        item("actions") {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = gutter - 4.dp).padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconAction(Icons.Outlined.ThumbUpOffAlt, TimeFormat.compactCount(item.stats.likes), vm::onLike, active = user.isLiked, activeIcon = Icons.Filled.ThumbUp, contentDescription = if (user.isLiked) "Unlike" else "Like")
                IconAction(Icons.Outlined.FavoriteBorder, "Favourite", vm::onFavourite, active = user.isFavourite, activeIcon = Icons.Filled.Favorite)
                IconAction(Icons.Outlined.BookmarkBorder, "Later", vm::onSaveForLater, active = user.isSavedForLater, activeIcon = Icons.Filled.Bookmark, contentDescription = "Save for later")
                IconAction(Icons.Outlined.Share, "Share", vm::onShare)
            }
            Rule(Modifier.padding(horizontal = gutter, vertical = 8.dp))
        }

        item("description") {
            Column(Modifier.padding(horizontal = gutter).padding(top = 8.dp)) {
                DropCapParagraphs(item.description)
                if (item.tags.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text(item.tags.joinToString("   ") { "#$it" }, style = OPTheme.type.caption, color = c.inkFaint)
                }
            }
        }

        if (state.chapters.isNotEmpty()) {
            item("chapters-header") { SectionHeader("Contents", Modifier.padding(horizontal = gutter).padding(top = 28.dp)) }
            items(state.chapters.size, key = { "ch-" + state.chapters[it].id }) { i ->
                val ch = state.chapters[i]
                val audio = item.audioAssets.firstOrNull { it.chapterIndex == ch.index }
                Row(
                    Modifier.fillMaxWidth()
                        .clickable(role = Role.Button) { vm.requestMode(ConsumptionMode.READ) { actions.openMode(item.id, it) } }
                        .padding(horizontal = gutter, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text((i + 1).toString().padStart(2, '0'), style = OPTheme.type.numeral.copy(fontSize = 20.sp, lineHeight = 20.sp), color = c.inkFaint, modifier = Modifier.width(40.dp))
                    DisplayText(ch.title, style = OPTheme.type.titleSmall, modifier = Modifier.weight(1f), maxLines = 1)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        if (audio != null && audio.durationMs > 0) TimeFormat.duration(audio.durationMs) else "${maxOf(1, ch.wordCount / 190)} min read",
                        style = OPTheme.type.caption, color = c.inkMuted,
                    )
                }
                if (i < state.chapters.lastIndex) Rule(Modifier.padding(horizontal = gutter))
            }
        }

        item("rate") {
            Column(Modifier.padding(horizontal = gutter).padding(top = 28.dp)) {
                SectionHeader(if (user.userRating != null) "Your rating" else "Rate this story")
                RatingStars(item.stats.ratingAverage, onRate = vm::onRate, userValue = user.userRating)
            }
        }

        item("comments") {
            Column(Modifier.padding(horizontal = gutter).padding(top = 24.dp)) {
                SectionHeader(
                    if (state.commentCount > 0) "Comments  ·  ${state.commentCount}" else "Comments",
                    action = if (state.commentCount > 2) "All" else null,
                    onAction = { actions.openComments(item.id) },
                )
                if (state.comments.isEmpty()) {
                    Text("Be the first to say something.", style = OPTheme.type.bodySmall, color = c.inkMuted)
                } else {
                    state.comments.forEachIndexed { i, comment ->
                        CommentPreview(comment)
                        if (i < state.comments.lastIndex) Rule(Modifier.padding(vertical = 10.dp))
                    }
                }
                Spacer(Modifier.height(12.dp))
                TextAction("Add a comment", onClick = { actions.openComments(item.id) })
            }
        }

        if (state.related.isNotEmpty()) {
            item("related-header") { SectionHeader("More like this", Modifier.padding(horizontal = gutter).padding(top = 28.dp)) }
            items(state.related.size, key = { "rel-" + state.related[it].id }) { i ->
                val rel = state.related[i]
                Box(Modifier.padding(horizontal = gutter)) {
                    ContentRow(rel, onClick = { actions.openContent(rel.id) }, showRule = i < state.related.lastIndex)
                }
            }
        }
    }
}

@Composable
private fun Hero(item: ContentItem, access: AccessDecision, onBack: () -> Unit) {
    val paper = Color(0xFFFBF8F2)
    Box(Modifier.fillMaxWidth().aspectRatio(4f / 5f).heightIn(max = 560.dp)) {
        Artwork(item.heroUrl, item.title, Modifier.matchParentSize(), shape = OPShapes.none, fallbackLetter = item.title)
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(0.45f to Color.Transparent, 1f to Color(0xE614110D))))
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(0f to Color(0x6614110D), 0.25f to Color.Transparent)))

        Row(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).padding(8.dp)) {
            IconButtonPlain(Icons.AutoMirrored.Outlined.ArrowBack, "Back", onBack, tint = paper)
        }

        Column(Modifier.align(Alignment.BottomStart).padding(OPTheme.spacing.gutter)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Eyebrow("${item.category.name}  ·  ${item.genres.firstOrNull()?.name ?: item.moods.firstOrNull()?.name.orEmpty()}", color = paper.copy(alpha = 0.8f))
                if (access !is AccessDecision.Granted) {
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Outlined.Lock, "Locked", Modifier.height(12.dp), tint = paper.copy(alpha = 0.8f))
                }
            }
            Spacer(Modifier.height(8.dp))
            DisplayText(item.title, style = OPTheme.type.display, color = paper, maxLines = 4)
            item.subtitle?.let {
                Spacer(Modifier.height(6.dp))
                Text(it, style = OPTheme.type.body.copy(fontStyle = FontStyle.Italic, fontFamily = OPFonts.serifFor(it)), color = paper.copy(alpha = 0.85f), maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun ModeRow(item: ContentItem, user: UserContentState, access: AccessDecision, onMode: (ConsumptionMode) -> Unit) {
    val modes = item.modes
    if (modes.isEmpty()) return
    val locked = access !is AccessDecision.Granted
    Row(
        Modifier.fillMaxWidth().padding(horizontal = OPTheme.spacing.gutter).padding(top = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        modes.forEachIndexed { i, mode ->
            val fraction = user.fraction(mode)
            val duration = item.durationFor(mode)
            val meta = when {
                locked -> "Unlock to ${mode.label.lowercase().substringBefore(' ')}"
                fraction > 0.02f && fraction < 0.985f -> {
                    val remaining = (duration * (1 - fraction)).toLong()
                    if (remaining > 0) "Continue · ${TimeFormat.duration(remaining)} left" else "Continue"
                }
                user.isCompleted && fraction >= 0.985f -> "Finished · again?"
                duration > 0 -> TimeFormat.duration(duration)
                else -> ""
            }
            ModeButton(
                label = mode.label,
                meta = meta,
                onClick = { onMode(mode) },
                modifier = Modifier.weight(1f),
                primary = i == 0,
                icon = if (locked) Icons.Outlined.Lock else mode.icon(),
                progress = if (fraction in 0.02f..0.985f) fraction else 0f,
                compact = modes.size >= 3,
            )
        }
    }
}

/** A raised initial on the first paragraph: the one typographic flourish the detail page allows itself. */
@Composable
private fun DropCapParagraphs(text: String) {
    val c = OPTheme.colors
    val paragraphs = text.split(Regex("\\n\\s*\\n")).map { it.trim() }.filter { it.isNotEmpty() }
    paragraphs.forEachIndexed { i, p ->
        if (i == 0 && p.length > 1) {
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(fontFamily = OPFonts.displayFor(p), fontSize = 34.sp, baselineShift = BaselineShift(-0.08f), color = c.accent)) { append(p.firstGrapheme()) }
                    append(p.dropFirstGrapheme())
                },
                style = OPTheme.type.body, color = c.ink,
            )
        } else {
            Text(p, style = OPTheme.type.body, color = c.ink)
        }
        if (i < paragraphs.lastIndex) Spacer(Modifier.height(10.dp))
    }
}

@Composable
internal fun CommentPreview(comment: Comment) {
    val c = OPTheme.colors
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(comment.userName, style = OPTheme.type.label, color = c.ink)
            Spacer(Modifier.width(8.dp))
            Text(TimeFormat.relative(comment.createdAt), style = OPTheme.type.caption, color = c.inkFaint)
            if (comment.editedAt != null) Text("  ·  edited", style = OPTheme.type.caption, color = c.inkFaint)
        }
        Spacer(Modifier.height(4.dp))
        Text(comment.body, style = OPTheme.type.bodySmall, color = c.ink)
    }
}

@Composable
private fun RewardSheet(mode: ConsumptionMode, inProgress: Boolean, onWatch: () -> Unit, onDismiss: () -> Unit) {
    val c = OPTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val progress by animateFloatAsState(if (inProgress) 1f else 0f, tween(if (inProgress) 4_000 else 0, easing = LinearEasing), label = "ad")
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = c.surface, contentColor = c.ink, shape = OPShapes.sheet, dragHandle = null) {
        Column(Modifier.padding(OPTheme.spacing.gutter).padding(bottom = 8.dp).windowInsetsPadding(WindowInsets.navigationBars)) {
            Eyebrow("Unlocked by a short ad", color = c.accent)
            Spacer(Modifier.height(8.dp))
            DisplayText("Watch a short ad to ${mode.label.lowercase()} this story", style = OPTheme.type.title)
            Spacer(Modifier.height(6.dp))
            Text("You'll have access for seven days. No account needed.", style = OPTheme.type.bodySmall, color = c.inkMuted)
            Spacer(Modifier.height(20.dp))
            if (inProgress) {
                Text("Ad playing…", style = OPTheme.type.caption, color = c.inkMuted)
                Spacer(Modifier.height(8.dp))
                ProgressHairline(progress, thickness = 3.dp)
            } else {
                PrimaryButton("Watch ad", onWatch, Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                TextAction("Not now", onDismiss, color = c.inkMuted)
            }
        }
    }
}
