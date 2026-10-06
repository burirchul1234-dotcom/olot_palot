package com.olotpalot.core.design.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.olotpalot.core.design.theme.LocalReducedMotion
import com.olotpalot.core.design.theme.OPMotion
import com.olotpalot.core.design.theme.OPTheme
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Cards and covers dip slightly under the finger. Pair with the same interaction source as the clickable. */
fun Modifier.pressScale(interaction: MutableInteractionSource, pressed: Float = 0.97f): Modifier = composed {
    val isPressed by interaction.collectIsPressedAsState()
    val reduced = LocalReducedMotion.current
    val scale by animateFloatAsState(if (isPressed && !reduced) pressed else 1f, spring(dampingRatio = 0.7f, stiffness = 900f), label = "press")
    scale(scale)
}

/**
 * Sections rise into place once, staggered by [index], the first time they appear.
 * Respects reduced motion (no movement, just instant).
 */
fun Modifier.enterRise(index: Int, stepMs: Int = 60): Modifier = composed {
    val reduced = LocalReducedMotion.current
    val alpha = remember { Animatable(if (reduced) 1f else 0f) }
    val offset = remember { Animatable(if (reduced) 0f else 24f) }
    LaunchedEffect(Unit) {
        if (!reduced) {
            delay((index * stepMs).toLong())
            coroutineScope {
                launch { alpha.animateTo(1f, tween(OPMotion.slow, easing = OPMotion.easeEnter)) }
                launch { offset.animateTo(0f, tween(OPMotion.slow, easing = OPMotion.easeEnter)) }
            }
        }
    }
    graphicsLayer { this.alpha = alpha.value; translationY = offset.value * density }
}

/** Circular progress around artwork — the product's shape for "you're part-way through". */
@Composable
fun ProgressRing(
    fraction: Float,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    stroke: Dp = 3.dp,
    color: Color = OPTheme.colors.accent,
    track: Color = OPTheme.colors.rule,
) {
    val animated by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(OPMotion.slow), label = "ring")
    Canvas(modifier.size(size)) {
        val sw = stroke.toPx()
        val inset = sw / 2
        val arcSize = Size(this.size.width - sw, this.size.height - sw)
        drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(sw))
        drawArc(color, -90f, 360f * animated, false, Offset(inset, inset), arcSize, style = Stroke(sw, cap = StrokeCap.Round))
    }
}
