package com.olotpalot.core.design.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.olotpalot.core.design.R

/**
 * Two families carry the whole product: Fraunces (editorial serif, variable)
 * for anything that reads as a headline or a passage, Manrope (humanist sans)
 * for interface text and metadata. Bengali titles swap to Noto Serif Bengali.
 */
@OptIn(ExperimentalTextApi::class)
object OPFonts {
    private fun fraunces(weight: Int, opsz: Float, soft: Float = 30f, italic: Boolean = false) = Font(
        resId = if (italic) R.font.fraunces_italic else R.font.fraunces,
        weight = FontWeight(weight),
        style = if (italic) FontStyle.Italic else FontStyle.Normal,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(weight),
            FontVariation.Setting("opsz", opsz),
            FontVariation.Setting("SOFT", soft),
            FontVariation.Setting("WONK", 0f),
        ),
    )

    private fun manrope(weight: Int) = Font(
        resId = R.font.manrope,
        weight = FontWeight(weight),
        variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
    )

    private fun bengali(weight: Int) = Font(
        resId = R.font.noto_serif_bengali,
        weight = FontWeight(weight),
        variationSettings = FontVariation.Settings(FontVariation.weight(weight), FontVariation.Setting("wdth", 100f)),
    )

    /** Large optical size: tight, high-contrast letterforms for titles. */
    val Display = FontFamily(
        fraunces(300, 96f), fraunces(400, 96f), fraunces(500, 96f), fraunces(600, 96f),
        fraunces(400, 96f, italic = true), fraunces(500, 96f, italic = true),
    )

    /** Text optical size: sturdier forms for long passages in the reader. */
    val Serif = FontFamily(
        fraunces(400, 14f, soft = 50f), fraunces(500, 14f, soft = 50f), fraunces(600, 14f, soft = 50f),
        fraunces(400, 14f, soft = 50f, italic = true),
    )

    val Sans = FontFamily(manrope(400), manrope(500), manrope(600), manrope(700), manrope(800))

    val Bengali = FontFamily(bengali(400), bengali(500), bengali(600), bengali(700))

    private val bengaliRange = 'ঀ'..'৿'
    fun isBengali(text: String): Boolean = text.any { it in bengaliRange }
    fun displayFor(text: String): FontFamily = if (isBengali(text)) Bengali else Display
    fun serifFor(text: String): FontFamily = if (isBengali(text)) Bengali else Serif
}

@Immutable
data class OPTypography(
    val display: TextStyle,
    val headline: TextStyle,
    val title: TextStyle,
    val titleSmall: TextStyle,
    val body: TextStyle,
    val bodySmall: TextStyle,
    val label: TextStyle,
    val eyebrow: TextStyle,
    val caption: TextStyle,
    val numeral: TextStyle,
    val reader: TextStyle,
    val readerTitle: TextStyle,
    val wordmark: TextStyle,
)

private val tight = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)

val DefaultOPTypography = OPTypography(
    display = TextStyle(fontFamily = OPFonts.Display, fontSize = 34.sp, lineHeight = 37.sp, fontWeight = FontWeight.W500, letterSpacing = (-0.015).em, lineHeightStyle = tight),
    headline = TextStyle(fontFamily = OPFonts.Display, fontSize = 26.sp, lineHeight = 29.sp, fontWeight = FontWeight.W500, letterSpacing = (-0.01).em, lineHeightStyle = tight),
    title = TextStyle(fontFamily = OPFonts.Display, fontSize = 20.sp, lineHeight = 24.sp, fontWeight = FontWeight.W500, lineHeightStyle = tight),
    titleSmall = TextStyle(fontFamily = OPFonts.Display, fontSize = 16.5.sp, lineHeight = 20.sp, fontWeight = FontWeight.W500, lineHeightStyle = tight),
    body = TextStyle(fontFamily = OPFonts.Sans, fontSize = 15.sp, lineHeight = 23.sp, fontWeight = FontWeight.W400),
    bodySmall = TextStyle(fontFamily = OPFonts.Sans, fontSize = 13.sp, lineHeight = 19.sp, fontWeight = FontWeight.W400),
    label = TextStyle(fontFamily = OPFonts.Sans, fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.W600, letterSpacing = 0.01.em),
    eyebrow = TextStyle(fontFamily = OPFonts.Sans, fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.W700, letterSpacing = 0.12.em),
    caption = TextStyle(fontFamily = OPFonts.Sans, fontSize = 11.5.sp, lineHeight = 15.sp, fontWeight = FontWeight.W500, letterSpacing = 0.01.em),
    numeral = TextStyle(fontFamily = OPFonts.Display, fontSize = 40.sp, lineHeight = 40.sp, fontWeight = FontWeight.W300, letterSpacing = (-0.03).em, lineHeightStyle = tight),
    reader = TextStyle(fontFamily = OPFonts.Serif, fontSize = 18.sp, lineHeight = 30.sp, fontWeight = FontWeight.W400),
    readerTitle = TextStyle(fontFamily = OPFonts.Display, fontSize = 30.sp, lineHeight = 34.sp, fontWeight = FontWeight.W500, letterSpacing = (-0.01).em, lineHeightStyle = tight),
    wordmark = TextStyle(fontFamily = OPFonts.Display, fontSize = 23.sp, lineHeight = 26.sp, fontWeight = FontWeight.W600, letterSpacing = (-0.02).em, lineHeightStyle = tight),
)

val LocalOPTypography = staticCompositionLocalOf { DefaultOPTypography }
