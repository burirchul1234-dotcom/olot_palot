package com.olotpalot.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.olotpalot.core.design.theme.OPFonts
import com.olotpalot.core.design.theme.OPTheme

/** Small caps section label. The product's primary way of saying "this is a section". */
@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier, color: Color = OPTheme.colors.inkMuted) {
    Text(text.uppercase(), style = OPTheme.type.eyebrow, color = color, modifier = modifier, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
fun Rule(modifier: Modifier = Modifier, color: Color = OPTheme.colors.rule) {
    Box(modifier.fillMaxWidth().height(OPTheme.spacing.hairline).background(color))
}

@Composable
fun VerticalRule(modifier: Modifier = Modifier, color: Color = OPTheme.colors.rule) {
    Box(modifier.width(OPTheme.spacing.hairline).background(color))
}

/** Eyebrow + hairline (+ optional trailing text action). Never a big heading. */
@Composable
fun SectionHeader(
    eyebrow: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(modifier.fillMaxWidth().padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Eyebrow(eyebrow)
        Spacer(Modifier.width(10.dp))
        Rule(Modifier.weight(1f))
        if (action != null && onAction != null) {
            Spacer(Modifier.width(10.dp))
            TextAction(action, onClick = onAction)
        }
    }
}

/** Title text that automatically switches to the Bengali serif when needed. */
@Composable
fun DisplayText(
    text: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = OPTheme.type.title,
    color: Color = OPTheme.colors.ink,
    maxLines: Int = Int.MAX_VALUE,
) {
    val family = if (OPFonts.isBengali(text)) OPFonts.Bengali else style.fontFamily
    Text(
        text,
        modifier = modifier,
        style = style.copy(fontFamily = family),
        color = color,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

/** "Author · 24 min · 1.2k views" — metadata separated by middle dots. */
@Composable
fun MetaLine(parts: List<String>, modifier: Modifier = Modifier, color: Color = OPTheme.colors.inkMuted, maxLines: Int = 1) {
    Text(
        parts.filter { it.isNotBlank() }.joinToString("  ·  "),
        style = OPTheme.type.caption,
        color = color,
        modifier = modifier,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}
