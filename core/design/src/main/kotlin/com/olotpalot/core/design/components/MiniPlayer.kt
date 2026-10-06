package com.olotpalot.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.olotpalot.core.design.theme.OPShapes
import com.olotpalot.core.design.theme.OPTheme

/**
 * One strip above the nav for whatever you're in the middle of. For audio it is a
 * real mini-player (play/pause, live progress); for reading/watching it resumes.
 */
@Composable
fun MiniPlayer(
    eyebrow: String,
    title: String,
    progress: Float,
    artworkUrl: String?,
    tint: Color,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    isPlaying: Boolean? = null,
    onTogglePlay: (() -> Unit)? = null,
) {
    val c = OPTheme.colors
    Column(modifier.fillMaxWidth().background(c.surface)) {
        Rule()
        Row(
            Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onOpen).padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Artwork(artworkUrl, null, Modifier.size(40.dp).clip(CircleShape), shape = CircleShape, fallbackLetter = title)
                ProgressRing(progress, size = 48.dp, stroke = 2.5.dp, color = tint, track = c.rule)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Eyebrow(eyebrow, color = tint)
                Spacer(Modifier.height(2.dp))
                DisplayText(title, style = OPTheme.type.titleSmall, maxLines = 1)
            }
            if (onTogglePlay != null && isPlaying != null) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(c.ink).clickable(role = Role.Button, onClick = onTogglePlay),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, if (isPlaying) "Pause" else "Play", Modifier.size(22.dp), tint = c.background)
                }
                Spacer(Modifier.width(4.dp))
            } else {
                Icon(Icons.Filled.PlayArrow, null, tint = c.ink, modifier = Modifier.padding(end = 4.dp))
            }
            IconButtonPlain(Icons.Outlined.Close, "Dismiss", onDismiss, tint = c.inkMuted)
        }
    }
}

@Suppress("unused")
private val unusedShape = OPShapes.control
