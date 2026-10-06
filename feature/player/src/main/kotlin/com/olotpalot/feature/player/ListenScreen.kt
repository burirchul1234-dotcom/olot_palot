package com.olotpalot.feature.player

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Forward30
import androidx.compose.material.icons.outlined.Replay10
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.olotpalot.core.common.TimeFormat
import com.olotpalot.core.design.components.Artwork
import com.olotpalot.core.design.components.DisplayText
import com.olotpalot.core.design.components.EmptyState
import com.olotpalot.core.design.components.ErrorState
import com.olotpalot.core.design.components.Eyebrow
import com.olotpalot.core.design.components.IconButtonPlain
import com.olotpalot.core.design.components.LoadingBlocks
import com.olotpalot.core.design.components.Rule
import com.olotpalot.core.design.components.SectionHeader
import com.olotpalot.core.design.components.TextToggle
import com.olotpalot.core.design.components.deepen
import com.olotpalot.core.design.components.rememberStoryTint
import com.olotpalot.core.design.theme.OPShapes
import com.olotpalot.core.design.theme.OPTheme

/**
 * Listening lives inside the story's own colour: the top of the screen is the
 * artwork's tint, fading into paper where the controls and chapters sit.
 */
@Composable
fun ListenScreen(onBack: () -> Unit, viewModel: ListenViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = OPTheme.colors
    val gutter = OPTheme.spacing.gutter
    val paper = Color(0xFFFBF8F2)
    val item = state.item
    val tint by rememberStoryTint(item?.heroUrl, item?.accentArgb)
    val top = tint.deepen(if (c.isDark) 0.55f else 0.15f)

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to top, 0.48f to top.copy(alpha = 0.55f), 0.62f to c.background, 1f to c.background))) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)) {
            Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButtonPlain(Icons.AutoMirrored.Outlined.ArrowBack, "Back", onBack, tint = paper)
                Eyebrow("Listening", Modifier.weight(1f), color = paper.copy(alpha = 0.85f))
                SleepTimerButton(state.sleepRemainingMs, onSet = viewModel::setSleepTimer, tint = paper)
            }
            when {
                state.error != null && item == null -> Box(Modifier.padding(gutter)) { EmptyState("Nothing to listen to", state.error!!, action = "Go back", onAction = onBack) }
                item == null -> Column(Modifier.padding(gutter)) { LoadingBlocks() }
                else -> PlayerBody(state, viewModel, tint)
            }
        }
    }
}

@Composable
private fun PlayerBody(state: PlaybackState, vm: ListenViewModel, tint: Color) {
    val c = OPTheme.colors
    val gutter = OPTheme.spacing.gutter
    val paper = Color(0xFFFBF8F2)
    val item = state.item ?: return
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = gutter, vertical = 8.dp)) {
        item("art") {
            Box(Modifier.fillMaxWidth().padding(top = 12.dp), contentAlignment = Alignment.Center) {
                Artwork(
                    item.heroUrl, item.title,
                    Modifier.fillMaxWidth(0.82f).widthIn(max = 320.dp).aspectRatio(1f).shadow(28.dp, OPShapes.tile, ambientColor = tint.deepen(0.4f), spotColor = tint.deepen(0.6f)),
                    shape = OPShapes.tile, fallbackLetter = item.title,
                )
            }
        }
        item("title") {
            Column(Modifier.padding(top = 26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (state.hasChapters) Eyebrow("Chapter ${state.currentIndex + 1} of ${state.assets.size}  ·  ${state.currentTitle}", color = tint.deepen(if (c.isDark) 0f else 0.2f))
                else if (state.currentTitle.isNotBlank()) Eyebrow(state.currentTitle, color = tint.deepen(if (c.isDark) 0f else 0.2f))
                Spacer(Modifier.height(8.dp))
                DisplayText(item.title, style = OPTheme.type.headline, maxLines = 3, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(4.dp))
                Text(item.author.name, style = OPTheme.type.bodySmall, color = c.inkMuted)
            }
        }
        item("scrub") {
            Column(Modifier.padding(top = 18.dp)) {
                var dragging by remember { mutableStateOf<Float?>(null) }
                val fraction = dragging ?: if (state.durationMs > 0) state.positionMs.toFloat() / state.durationMs else 0f
                Slider(
                    value = fraction.coerceIn(0f, 1f),
                    onValueChange = { dragging = it },
                    onValueChangeFinished = { dragging?.let(vm::seekTo); dragging = null },
                    modifier = Modifier.fillMaxWidth().height(28.dp),
                    colors = SliderDefaults.colors(thumbColor = tint, activeTrackColor = tint, inactiveTrackColor = c.rule),
                )
                Row(Modifier.fillMaxWidth()) {
                    Text(TimeFormat.clock((fraction * state.durationMs).toLong()), style = OPTheme.type.caption, color = c.inkMuted)
                    Spacer(Modifier.weight(1f))
                    Text(if (state.buffering) "Buffering…" else "-" + TimeFormat.clock(state.durationMs - (fraction * state.durationMs).toLong()), style = OPTheme.type.caption, color = c.inkMuted)
                }
            }
        }
        item("controls") {
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                IconButtonPlain(Icons.Outlined.SkipPrevious, "Previous chapter", vm::previous, tint = if (state.hasChapters) c.ink else c.inkFaint)
                IconButtonPlain(Icons.Outlined.Replay10, "Back 10 seconds", { vm.skip(-10_000) })
                Box(
                    Modifier.size(72.dp).shadow(14.dp, CircleShape, ambientColor = tint, spotColor = tint).clip(CircleShape).background(tint).clickable(role = Role.Button, onClick = vm::togglePlay),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, if (state.isPlaying) "Pause" else "Play", Modifier.size(34.dp), tint = paper)
                }
                IconButtonPlain(Icons.Outlined.Forward30, "Forward 30 seconds", { vm.skip(30_000) })
                IconButtonPlain(Icons.Outlined.SkipNext, "Next chapter", vm::next, tint = if (state.hasChapters) c.ink else c.inkFaint)
            }
        }
        item("speed") {
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Eyebrow("Speed")
                Spacer(Modifier.width(8.dp))
                listOf(0.8f, 1f, 1.25f, 1.5f, 2f).forEach { s ->
                    TextToggle(if (s == 1f) "1×" else "${s}×".replace(".0×", "×"), selected = state.speed == s, onClick = { vm.setSpeed(s) })
                }
            }
        }
        state.error?.let { msg -> item("error") { ErrorState(msg, onRetry = vm::retry, title = "Playback paused") } }
        if (state.hasChapters) {
            item("chapters-header") { SectionHeader("Chapters", Modifier.padding(top = 24.dp)) }
            items(state.assets.size, key = { "asset-" + state.assets[it].id }) { i ->
                val asset = state.assets[i]
                val title = state.chapters.getOrNull(asset.chapterIndex ?: -1)?.title ?: "Part ${i + 1}"
                val current = i == state.currentIndex
                Row(Modifier.fillMaxWidth().clickable(role = Role.Button) { vm.goTo(i) }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text((i + 1).toString().padStart(2, '0'), style = OPTheme.type.numeral.copy(fontSize = 20.sp, lineHeight = 20.sp), color = if (current) tint else c.inkFaint, modifier = Modifier.width(40.dp))
                    DisplayText(title, style = OPTheme.type.titleSmall, color = if (current) tint.deepen(if (c.isDark) 0f else 0.2f) else c.ink, modifier = Modifier.weight(1f), maxLines = 1)
                    Text(TimeFormat.duration(asset.durationMs), style = OPTheme.type.caption, color = c.inkMuted)
                }
                if (i < state.assets.lastIndex) Rule()
            }
        }
    }
}

@Composable
private fun SleepTimerButton(remainingMs: Long?, onSet: (Int?) -> Unit, tint: Color) {
    val c = OPTheme.colors
    var open by remember { mutableStateOf(false) }
    Box {
        Row(Modifier.clip(OPShapes.control).clickable(role = Role.Button) { open = true }.padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Bedtime, "Sleep timer", Modifier.size(20.dp), tint = tint)
            if (remainingMs != null) { Spacer(Modifier.width(6.dp)); Text(TimeFormat.clock(remainingMs), style = OPTheme.type.caption, color = tint) }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = c.surface) {
            listOf(null to "Off", 15 to "15 minutes", 30 to "30 minutes", 45 to "45 minutes", 60 to "1 hour").forEach { (m, label) ->
                DropdownMenuItem(text = { Text(label, style = OPTheme.type.bodySmall, color = c.ink) }, onClick = { onSet(m); open = false })
            }
        }
    }
}
