package com.olotpalot.core.design.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class OPSpacing(
    val xxs: Dp = 4.dp,
    val xs: Dp = 8.dp,
    val sm: Dp = 12.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 20.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
    val xxxl: Dp = 44.dp,
    /** Page gutter: the one number every screen aligns to. */
    val gutter: Dp = 20.dp,
    val touchTarget: Dp = 44.dp,
    val navHeight: Dp = 48.dp,
    val hairline: Dp = 1.dp,
)

/** Restrained radii. Editorial surfaces are nearly square; only controls soften. */
object OPShapes {
    val none = RoundedCornerShape(0.dp)
    val thumb = RoundedCornerShape(4.dp)
    val control = RoundedCornerShape(6.dp)
    val sheet = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
    val tile = RoundedCornerShape(8.dp)
}

/** One motion language for the whole product. */
object OPMotion {
    const val fast = 120
    const val standard = 220
    const val slow = 320
    val easeStandard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val easeEnter: Easing = CubicBezierEasing(0f, 0f, 0f, 1f)
    val easeExit: Easing = CubicBezierEasing(0.3f, 0f, 1f, 1f)
}

val LocalOPSpacing = staticCompositionLocalOf { OPSpacing() }
val LocalReducedMotion = staticCompositionLocalOf { false }
