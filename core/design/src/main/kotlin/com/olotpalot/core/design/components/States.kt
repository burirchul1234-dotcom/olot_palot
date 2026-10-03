package com.olotpalot.core.design.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import com.olotpalot.core.design.theme.OPShapes
import com.olotpalot.core.design.theme.OPTheme

/** Quiet loading: paper-coloured blocks that breathe. No spinner. */
@Composable
fun LoadingBlocks(modifier: Modifier = Modifier, rows: Int = 4) {
    val c = OPTheme.colors
    val reduced = OPTheme.reducedMotion
    val transition = rememberInfiniteTransition(label = "breathe")
    val alpha by transition.animateFloat(
        initialValue = 0.55f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "alpha",
    )
    Column(modifier.fillMaxWidth().alpha(if (reduced) 0.8f else alpha).semantics { contentDescription = "Loading" }) {
        repeat(rows) { i ->
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(width = 56.dp, height = 72.dp).clip(OPShapes.thumb).background(c.surfaceRaised))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Box(Modifier.fillMaxWidth(if (i % 2 == 0) 0.7f else 0.55f).height(14.dp).clip(OPShapes.thumb).background(c.surfaceRaised))
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.fillMaxWidth(0.35f).height(10.dp).clip(OPShapes.thumb).background(c.surfaceRaised))
                }
            }
        }
    }
}

@Composable
fun EmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null,
    compact: Boolean = false,
) {
    val c = OPTheme.colors
    Column(
        modifier.fillMaxWidth().padding(vertical = if (compact) 16.dp else 40.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Box(Modifier.width(28.dp).height(2.dp).background(c.accent))
        Spacer(Modifier.height(14.dp))
        DisplayText(title, style = if (compact) OPTheme.type.titleSmall else OPTheme.type.title)
        Spacer(Modifier.height(6.dp))
        Text(body, style = OPTheme.type.bodySmall, color = c.inkMuted)
        if (action != null && onAction != null) {
            Spacer(Modifier.height(16.dp))
            SecondaryButton(action, onAction)
        }
    }
}

@Composable
fun ErrorState(
    message: String,
    onRetry: (() -> Unit)?,
    modifier: Modifier = Modifier,
    title: String = "Something went wrong",
) {
    EmptyState(title = title, body = message, modifier = modifier, action = if (onRetry != null) "Try again" else null, onAction = onRetry)
}

/** Stars read as information by default; pass [onRate] to make them a control. */
@Composable
fun RatingStars(
    value: Float,
    modifier: Modifier = Modifier,
    count: Int? = null,
    onRate: ((Int) -> Unit)? = null,
    size: Dp = 14.dp,
    userValue: Int? = null,
) {
    val c = OPTheme.colors
    val shown = userValue?.toFloat() ?: value
    Row(
        modifier.semantics { contentDescription = if (count != null) "Rated %.1f by $count".format(value) else "Rated %.1f".format(value) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (i in 1..5) {
            val filled = shown >= i - 0.25f
            val tint = if (userValue != null) c.accent else if (filled) c.ink else c.rule
            Icon(
                if (filled) Icons.Filled.Star else Icons.Outlined.StarOutline,
                null,
                Modifier
                    .size(if (onRate != null) 28.dp else size)
                    .then(if (onRate != null) Modifier.clip(OPShapes.control).clickable(role = Role.Button) { onRate(i) }.padding(4.dp) else Modifier),
                tint = tint,
            )
        }
        if (count != null) {
            Spacer(Modifier.width(6.dp))
            Text(
                if (count > 0) "%.1f (%d)".format(value, count) else "No ratings yet",
                style = OPTheme.type.caption, color = c.inkMuted,
            )
        }
    }
}

@Composable
fun InlineNotice(text: String, modifier: Modifier = Modifier) {
    val c = OPTheme.colors
    Row(
        modifier.fillMaxWidth().clip(OPShapes.control).background(c.surfaceRaised).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(3.dp).height(18.dp).background(c.accent))
        Spacer(Modifier.width(12.dp))
        Text(text, style = OPTheme.type.bodySmall, color = c.ink, textAlign = TextAlign.Start)
    }
}
