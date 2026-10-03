package com.olotpalot.core.design.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.olotpalot.core.design.theme.OPShapes
import com.olotpalot.core.design.theme.OPTheme

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val c = OPTheme.colors
    Row(
        modifier
            .defaultMinSize(minHeight = OPTheme.spacing.touchTarget)
            .clip(OPShapes.control)
            .background(if (enabled) c.ink else c.rule)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(18.dp), tint = c.background)
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = OPTheme.type.label, color = c.background, maxLines = 1)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val c = OPTheme.colors
    Row(
        modifier
            .defaultMinSize(minHeight = OPTheme.spacing.touchTarget)
            .clip(OPShapes.control)
            .border(1.dp, c.ink.copy(alpha = 0.5f), OPShapes.control)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(18.dp), tint = c.ink)
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = OPTheme.type.label, color = c.ink, maxLines = 1)
    }
}

/** Underlined text action, used in section headers and inline. */
@Composable
fun TextAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = OPTheme.colors.ink) {
    Column(
        modifier
            .clip(OPShapes.control)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp),
    ) {
        Text(text, style = OPTheme.type.label, color = color, maxLines = 1)
        Box(Modifier.fillMaxWidth().height(1.dp).background(color.copy(alpha = 0.6f)))
    }
}

/**
 * A consumption-mode control: label on top, meta beneath (duration / progress).
 * The first available mode is rendered filled; the rest outlined. This is how
 * the Listen → Read Story → Watch hierarchy is expressed.
 */
@Composable
fun ModeButton(
    label: String,
    meta: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    icon: ImageVector? = null,
    progress: Float = 0f,
) {
    val c = OPTheme.colors
    val bg = if (primary) c.ink else Color.Transparent
    val fg = if (primary) c.background else c.ink
    Column(
        modifier
            .clip(OPShapes.control)
            .background(bg)
            .then(if (primary) Modifier else Modifier.border(1.dp, c.ink.copy(alpha = 0.35f), OPShapes.control))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, null, Modifier.size(16.dp), tint = fg)
                Spacer(Modifier.width(7.dp))
            }
            Text(label, style = OPTheme.type.label, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(3.dp))
        Text(meta, style = OPTheme.type.caption, color = fg.copy(alpha = 0.72f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (progress > 0f) {
            Spacer(Modifier.height(7.dp))
            ProgressHairline(progress, trackColor = fg.copy(alpha = 0.2f), color = if (primary) c.accentAlt else c.accent)
        }
    }
}

/** Icon + tiny label; the secondary-action vocabulary (like, favourite, save, download, share). */
@Composable
fun IconAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    activeIcon: ImageVector = icon,
    contentDescription: String = label,
) {
    val c = OPTheme.colors
    val tint by animateColorAsState(if (active) c.accent else c.ink, label = "tint")
    val scale by animateFloatAsState(if (active) 1.08f else 1f, spring(dampingRatio = 0.5f, stiffness = 700f), label = "scale")
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier
            .defaultMinSize(minWidth = 52.dp, minHeight = OPTheme.spacing.touchTarget)
            .clip(OPShapes.control)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription }
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(if (active) activeIcon else icon, null, Modifier.size(22.dp).scale(scale), tint = tint)
        Spacer(Modifier.height(4.dp))
        Text(label, style = OPTheme.type.caption, color = if (active) c.accent else c.inkMuted, maxLines = 1)
    }
}

/** Plain-text toggle (filters, moods). Selected = ink + underline, never a filled pill. */
@Composable
fun TextToggle(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = OPTheme.colors
    Column(
        modifier
            .clip(OPShapes.control)
            .clickable(role = Role.Checkbox, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
        Text(text, style = OPTheme.type.label, color = if (selected) c.ink else c.inkMuted, maxLines = 1)
        Spacer(Modifier.height(3.dp))
        Box(Modifier.fillMaxWidth().height(2.dp).background(if (selected) c.accent else Color.Transparent))
    }
}

@Composable
fun IconButtonPlain(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = OPTheme.colors.ink,
    background: Color = Color.Transparent,
) {
    Box(
        modifier
            .size(OPTheme.spacing.touchTarget)
            .clip(OPShapes.control)
            .background(background)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, Modifier.size(22.dp), tint = tint)
    }
}
