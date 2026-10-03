package com.olotpalot.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.olotpalot.core.design.theme.OPShapes
import com.olotpalot.core.design.theme.OPTheme

/**
 * The one search control in the product: 40dp, single line, hairline border,
 * filter beside it. It never grows to two lines; long queries scroll inside.
 */
@Composable
fun CompactSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit,
    onFilter: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search stories, authors, moods",
    filterCount: Int = 0,
    focusRequester: FocusRequester? = null,
    onFocusChanged: ((Boolean) -> Unit)? = null,
) {
    val c = OPTheme.colors
    Row(modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .height(40.dp)
                .clip(OPShapes.control)
                .border(1.dp, c.rule, OPShapes.control)
                .background(c.surface)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Search, null, Modifier.size(18.dp), tint = c.inkMuted)
            Spacer(Modifier.width(8.dp))
            Box(Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(placeholder, style = OPTheme.type.bodySmall, color = c.inkFaint, maxLines = 1)
                }
                BasicTextField(
                    value = value,
                    onValueChange = { onValueChange(it.replace('\n', ' ')) },
                    singleLine = true,
                    textStyle = OPTheme.type.bodySmall.copy(color = c.ink),
                    cursorBrush = SolidColor(c.accent),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                        .then(if (onFocusChanged != null) Modifier.onFocusChanged { onFocusChanged(it.isFocused) } else Modifier),
                )
            }
            if (value.isNotEmpty()) {
                Icon(
                    Icons.Outlined.Close, "Clear search",
                    Modifier.size(18.dp).clip(OPShapes.control).clickable(role = Role.Button) { onValueChange("") },
                    tint = c.inkMuted,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier
                .size(40.dp)
                .clip(OPShapes.control)
                .border(1.dp, if (filterCount > 0) c.ink else c.rule, OPShapes.control)
                .background(if (filterCount > 0) c.ink else c.surface)
                .clickable(role = Role.Button, onClick = onFilter),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Tune,
                if (filterCount > 0) "Filters, $filterCount active" else "Filters",
                Modifier.size(18.dp),
                tint = if (filterCount > 0) c.background else c.ink,
            )
            if (filterCount > 0) {
                Text(
                    filterCount.toString(),
                    style = OPTheme.type.eyebrow,
                    color = c.background,
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 3.dp, end = 5.dp),
                )
            }
        }
    }
}
