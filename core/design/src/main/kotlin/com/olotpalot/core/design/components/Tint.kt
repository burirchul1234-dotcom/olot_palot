package com.olotpalot.core.design.components

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.olotpalot.core.design.theme.OPColors
import com.olotpalot.core.design.theme.OPTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Collections

/**
 * Story tint: starts from the editorial accent, then refines to the artwork's
 * own dominant colour once the image is available. Results are cached per URL
 * so lists don't re-extract on every scroll.
 */
object ArtworkTints {
    private val cache = Collections.synchronizedMap(HashMap<String, Int>())

    suspend fun extract(context: android.content.Context, url: String): Int? {
        cache[url]?.let { return it }
        val loader = SingletonImageLoader.get(context)
        val result = loader.execute(ImageRequest.Builder(context).data(url).allowHardware(false).size(160).build())
        val bitmap: Bitmap = (result as? SuccessResult)?.image?.toBitmap() ?: return null
        val palette = withContext(Dispatchers.Default) { Palette.from(bitmap).maximumColorCount(16).generate() }
        // Prefer a colour with real saturation; a grey sea or a sepia street keeps the editorial accent instead.
        val swatch = listOfNotNull(palette.vibrantSwatch, palette.darkVibrantSwatch, palette.lightVibrantSwatch, palette.mutedSwatch, palette.dominantSwatch)
            .firstOrNull { it.hsl[1] >= 0.28f && it.hsl[2] in 0.18f..0.72f } ?: return null
        return swatch.rgb.also { cache[url] = it }
    }
}

@Composable
fun rememberStoryTint(url: String?, fallbackArgb: Int?): State<Color> {
    val c = OPTheme.colors
    val context = LocalContext.current
    val initial = fallbackArgb?.let { Color(it) } ?: c.accent
    val state = remember(url, fallbackArgb) { mutableStateOf(initial) }
    LaunchedEffect(url) {
        if (!url.isNullOrBlank()) runCatching { ArtworkTints.extract(context, url) }.getOrNull()?.let { state.value = Color(it) }
    }
    return state
}

/** Keeps a tint legible as a background in the current theme: deepened on paper, lifted on ink. */
fun Color.asSurfaceTint(colors: OPColors): Color =
    if (colors.isDark) blend(this, Color.Black, 0.55f) else blend(this, Color.White, 0.0f)

fun Color.deepen(amount: Float = 0.45f): Color = blend(this, Color.Black, amount)
fun Color.soften(colors: OPColors, amount: Float = 0.82f): Color = blend(this, colors.background, amount)

private fun blend(a: Color, b: Color, t: Float) = Color(
    red = a.red + (b.red - a.red) * t,
    green = a.green + (b.green - a.green) * t,
    blue = a.blue + (b.blue - a.blue) * t,
    alpha = 1f,
)

fun coverScrim(tint: Color): Brush = Brush.verticalGradient(
    0f to Color.Transparent,
    0.42f to tint.deepen(0.6f).copy(alpha = 0.15f),
    1f to tint.deepen(0.78f).copy(alpha = 0.96f),
)
