package com.olotpalot.feature.player

import android.content.Intent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ThumbUpOffAlt
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.olotpalot.core.common.TimeFormat
import com.olotpalot.core.design.components.ContentRow
import com.olotpalot.core.design.components.DisplayText
import com.olotpalot.core.design.components.EmptyState
import com.olotpalot.core.design.components.ErrorState
import com.olotpalot.core.design.components.IconAction
import com.olotpalot.core.design.components.IconButtonPlain
import com.olotpalot.core.design.components.MetaLine
import com.olotpalot.core.design.components.Rule
import com.olotpalot.core.design.components.SectionHeader
import com.olotpalot.core.design.theme.OPTheme

@Composable
fun WatchPlayerScreen(onBack: () -> Unit, onOpenVideo: (String) -> Unit, viewModel: WatchPlayerViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = OPTheme.colors
    val gutter = OPTheme.spacing.gutter
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.share.collect { (subject, text) ->
            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_SUBJECT, subject); putExtra(Intent.EXTRA_TEXT, text) }, "Share"))
        }
    }

    Column(Modifier.fillMaxSize().background(c.background)) {
        Box(Modifier.fillMaxWidth().background(Color.Black).windowInsetsPadding(WindowInsets.statusBars)) {
            if (state.ready) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = viewModel.player
                            useController = true
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            setShowNextButton(false)
                            setShowPreviousButton(false)
                            controllerShowTimeoutMs = 2_500
                        }
                    },
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                )
            } else {
                Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f))
            }
            IconButtonPlain(Icons.AutoMirrored.Outlined.ArrowBack, "Back", onBack, Modifier.padding(4.dp), tint = Color.White)
        }

        when {
            state.notFound -> Box(Modifier.padding(gutter)) { EmptyState("Nothing to watch", "This story has no video.", action = "Go back", onAction = onBack) }
            state.item == null -> Unit
            else -> {
                val item = state.item!!
                val user = state.user
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = gutter, vertical = 12.dp)) {
                    item("title") {
                        DisplayText(item.title, style = OPTheme.type.headline)
                        Spacer(Modifier.height(6.dp))
                        MetaLine(listOf(item.author.name, "${TimeFormat.compactCount(item.stats.views)} views", TimeFormat.duration(item.videoDurationMs), TimeFormat.relative(item.publishedAt)))
                    }
                    item("actions") {
                        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            IconAction(Icons.Outlined.ThumbUpOffAlt, TimeFormat.compactCount(item.stats.likes), viewModel::onLike, active = user?.isLiked == true, activeIcon = Icons.Filled.ThumbUp, contentDescription = "Like")
                            IconAction(Icons.Outlined.BookmarkBorder, "Later", viewModel::onSave, active = user?.isSavedForLater == true, activeIcon = Icons.Filled.Bookmark, contentDescription = "Watch later")
                            IconAction(Icons.Outlined.Share, "Share", viewModel::onShare)
                        }
                        Rule(Modifier.padding(vertical = 8.dp))
                    }
                    state.error?.let { msg -> item("error") { ErrorState(msg, onRetry = viewModel::retry, title = "Playback paused") } }
                    item("description") {
                        Text(item.description, style = OPTheme.type.body, color = c.ink, modifier = Modifier.padding(top = 4.dp))
                    }
                    if (state.related.isNotEmpty()) {
                        item("up-next") { SectionHeader("Up next", Modifier.padding(top = 24.dp)) }
                        items(state.related.size, key = { state.related[it].id }) { i ->
                            val rel = state.related[i]
                            ContentRow(rel, onClick = { onOpenVideo(rel.id) }, meta = "Watch · ${TimeFormat.duration(rel.videoDurationMs)}", showRule = i < state.related.lastIndex)
                        }
                    }
                }
            }
        }
    }
}
