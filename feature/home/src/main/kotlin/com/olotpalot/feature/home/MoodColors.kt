package com.olotpalot.feature.home

import androidx.compose.ui.graphics.Color

/** Each mood owns a hue; tiles, chips and onboarding all draw from here so a mood is recognisable at a glance. */
object MoodColors {
    private val map = mapOf(
        "m-calm" to Color(0xFF2E7D7A),
        "m-wistful" to Color(0xFF7A5C8E),
        "m-tense" to Color(0xFFB3382F),
        "m-wonder" to Color(0xFF5B4FBF),
        "m-warm" to Color(0xFFC77A2E),
        "m-hopeful" to Color(0xFF3F8A4C),
        "m-nocturnal" to Color(0xFF26355F),
        "m-reflective" to Color(0xFF5E6B73),
        "m-melancholy" to Color(0xFF8A5A6E),
        "m-epic" to Color(0xFFA8842C),
    )
    fun of(id: String): Color = map[id] ?: Color(0xFF6B655B)
}
