package com.olotpalot.core.design.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.olotpalot.core.design.theme.OPMotion
import com.olotpalot.core.design.theme.OPShapes
import com.olotpalot.core.design.theme.OPTheme

/**
 * Artwork with a paper-coloured placeholder (no spinner, no grey box) and a
 * one-letter fallback so a failed image still reads as "a story", not a bug.
 */
@Composable
fun Artwork(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    shape: Shape = OPShapes.thumb,
    fallbackLetter: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val c = OPTheme.colors
    Box(modifier.clip(shape).background(c.surfaceRaised), contentAlignment = Alignment.Center) {
        if (fallbackLetter != null) {
            Text(fallbackLetter.take(1), style = OPTheme.type.numeral, color = c.rule)
        }
        if (!url.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(url).crossfade(OPMotion.standard).build(),
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = Modifier.fillMaxWidth().fillMaxHeight(),
            )
        }
    }
}

/** 2dp progress line. Used everywhere progress is shown so the eye learns one shape. */
@Composable
fun ProgressHairline(
    fraction: Float,
    modifier: Modifier = Modifier,
    color: Color = OPTheme.colors.accent,
    trackColor: Color = OPTheme.colors.rule,
    thickness: androidx.compose.ui.unit.Dp = 2.dp,
) {
    val animated by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(OPMotion.standard), label = "progress")
    Box(modifier.fillMaxWidth().height(thickness).background(trackColor)) {
        Box(Modifier.fillMaxWidth(animated).fillMaxHeight().background(color))
    }
}
