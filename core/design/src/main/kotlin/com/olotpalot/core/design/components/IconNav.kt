package com.olotpalot.core.design.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.olotpalot.core.design.theme.OPMotion
import com.olotpalot.core.design.theme.OPTheme

data class NavIconItem(val key: String, val label: String, val icon: ImageVector, val selectedIcon: ImageVector)

object NavIcons {
    fun forKey(key: String): Pair<ImageVector, ImageVector> = when (key) {
        "home" -> Icons.Outlined.Home to Icons.Filled.Home
        "discover" -> Icons.Outlined.Explore to Icons.Filled.Explore
        "watch" -> Icons.Outlined.PlayCircle to Icons.Filled.PlayCircle
        "library" -> Icons.Outlined.CollectionsBookmark to Icons.Filled.CollectionsBookmark
        else -> Icons.Outlined.Person to Icons.Filled.Person
    }
}

/**
 * The product's single navigation: icon + label, 56dp, with the active item
 * lifted by a soft accent disc. Identical on every screen and in every theme.
 */
@Composable
fun IconNav(
    items: List<NavIconItem>,
    currentKey: String?,
    onSelect: (NavIconItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = OPTheme.colors
    Column(modifier.fillMaxWidth().background(c.background)) {
        Rule()
        Row(
            Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            items.forEach { item ->
                val selected = item.key == currentKey
                val tint by animateColorAsState(if (selected) c.accent else c.inkMuted, tween(OPMotion.standard), label = "navTint")
                val disc by animateFloatAsState(if (selected) 1f else 0f, spring(dampingRatio = 0.6f, stiffness = 500f), label = "navDisc")
                val interaction = remember { MutableInteractionSource() }
                Column(
                    Modifier
                        .weight(1f)
                        .height(58.dp)
                        .clickable(interactionSource = interaction, indication = null, role = Role.Tab) { onSelect(item) }
                        .semantics { this.selected = selected },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Box(Modifier.size(34.dp).scale(disc).clip(CircleShape).background(c.accentSoft))
                        Icon(if (selected) item.selectedIcon else item.icon, null, Modifier.size(22.dp), tint = tint)
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        item.label,
                        style = OPTheme.type.caption.copy(fontSize = 10.5.sp, fontWeight = if (selected) FontWeight.W700 else FontWeight.W500),
                        color = tint,
                        maxLines = 1,
                    )
                }
            }
        }
        Spacer(Modifier.windowInsetsPadding(WindowInsets.navigationBars))
    }
}
