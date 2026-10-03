package com.olotpalot.core.design.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.olotpalot.core.common.TimeFormat
import com.olotpalot.core.design.theme.OPTheme
import com.olotpalot.domain.model.ConsumptionMode
import com.olotpalot.domain.model.ContentItem

fun ConsumptionMode.icon(): ImageVector = when (this) {
    ConsumptionMode.LISTEN -> Icons.Outlined.Headphones
    ConsumptionMode.READ -> Icons.Outlined.MenuBook
    ConsumptionMode.WATCH -> Icons.Outlined.PlayCircleOutline
}

/** Tiny glyph row showing which modes a story offers. Information, not buttons. */
@Composable
fun ModeGlyphs(item: ContentItem, modifier: Modifier = Modifier, tint: androidx.compose.ui.graphics.Color = OPTheme.colors.inkMuted) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        item.modes.forEachIndexed { i, mode ->
            if (i > 0) Spacer(Modifier.width(6.dp))
            Icon(mode.icon(), mode.label, Modifier.size(13.dp), tint = tint)
        }
    }
}

fun ContentItem.primaryMeta(): String {
    val mode = primaryMode ?: return author.name
    val d = durationFor(mode)
    return if (d > 0) "${mode.label} · ${TimeFormat.duration(d)}" else mode.label
}

/**
 * The workhorse list row: small portrait artwork, title, author, mode glyphs.
 * Hairline-separated, 72dp of artwork — dense enough that six fit on a phone.
 */
@Composable
fun ContentRow(
    item: ContentItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    meta: String = item.primaryMeta(),
    progress: Float = 0f,
    showRule: Boolean = true,
) {
    val c = OPTheme.colors
    Column(modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (leading != null) {
                leading()
                Spacer(Modifier.width(12.dp))
            }
            Artwork(item.thumbnailUrl, null, Modifier.width(52.dp).height(68.dp), fallbackLetter = item.title)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                DisplayText(item.title, style = OPTheme.type.titleSmall, maxLines = 2)
                Spacer(Modifier.height(3.dp))
                Text(item.author.name, style = OPTheme.type.caption, color = c.inkMuted, maxLines = 1)
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ModeGlyphs(item)
                    Spacer(Modifier.width(8.dp))
                    Text(meta, style = OPTheme.type.caption, color = c.inkFaint, maxLines = 1)
                }
                if (progress > 0f) {
                    Spacer(Modifier.height(8.dp))
                    ProgressHairline(progress, Modifier.fillMaxWidth(0.6f))
                }
            }
            if (trailing != null) {
                Spacer(Modifier.width(8.dp))
                trailing()
            }
        }
        if (showRule) Rule()
    }
}
