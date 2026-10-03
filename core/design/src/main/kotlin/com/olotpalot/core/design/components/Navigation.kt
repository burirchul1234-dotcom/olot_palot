package com.olotpalot.core.design.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.olotpalot.core.design.theme.OPMotion
import com.olotpalot.core.design.theme.OPTheme

data class NavItem(val key: String, val label: String)

/**
 * The product's single navigation system: a 48dp typographic rail. Words, not
 * icons; the active section carries weight and a short accent rule. It sits
 * on every top-level screen and the content detail screen, identical in every theme.
 */
@Composable
fun TypographicNav(
    items: List<NavItem>,
    currentKey: String?,
    onSelect: (NavItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = OPTheme.colors
    Column(modifier.fillMaxWidth().background(c.background)) {
        Rule()
        Row(
            Modifier
                .fillMaxWidth()
                .height(OPTheme.spacing.navHeight)
                .padding(horizontal = OPTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                val selected = item.key == currentKey
                val color by animateColorAsState(if (selected) c.ink else c.inkMuted, tween(OPMotion.standard), label = "navColor")
                val ruleWidth by animateDpAsState(if (selected) 16.dp else 0.dp, tween(OPMotion.standard, easing = OPMotion.easeStandard), label = "navRule")
                val interaction = remember { MutableInteractionSource() }
                Column(
                    Modifier
                        .weight(1f)
                        .height(OPTheme.spacing.navHeight)
                        .clickable(interactionSource = interaction, indication = null, role = Role.Tab) { onSelect(item) }
                        .semantics { this.selected = selected },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                ) {
                    Text(
                        item.label,
                        style = OPTheme.type.label.copy(fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.W700 else androidx.compose.ui.text.font.FontWeight.W500),
                        color = color,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(Modifier.width(ruleWidth).height(2.dp).background(c.accent))
                }
            }
        }
        Spacer(Modifier.windowInsetsPadding(WindowInsets.navigationBars))
    }
}

/**
 * The persistent "continue" strip that sits above the nav: whatever you were in
 * the middle of — a listen, a read, a watch — one tap away from anywhere.
 */
@Composable
fun ContinueStrip(
    eyebrow: String,
    title: String,
    progress: Float,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    artworkUrl: String? = null,
) {
    val c = OPTheme.colors
    Column(modifier.fillMaxWidth().background(c.surface)) {
        Rule()
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onOpen)
                .padding(start = OPTheme.spacing.gutter, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Artwork(artworkUrl, null, Modifier.width(30.dp).height(40.dp), fallbackLetter = title)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Eyebrow(eyebrow, color = c.accent)
                Spacer(Modifier.height(2.dp))
                DisplayText(title, style = OPTheme.type.titleSmall, maxLines = 1)
            }
            Icon(Icons.Outlined.PlayArrow, null, tint = c.ink)
            IconButtonPlain(Icons.Outlined.Close, "Dismiss", onDismiss, tint = c.inkMuted)
        }
        ProgressHairline(progress, trackColor = c.rule)
    }
}
