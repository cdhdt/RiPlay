package it.fast4x.riplay.ui.spotify

import androidx.compose.ui.graphics.Color

/** Palette taken verbatim from docs/desktop/DESIGN.md (§8). Dark theme only. */
object RiPlayColors {
    val bg = Color(0xFF0A0A0A)          // app base, gutters
    val panel = Color(0xFF121212)       // sidebar / now-playing / content panels
    val card = Color(0xFF181818)        // card at rest
    val cardHover = Color(0xFF282828)   // card hover, active row
    val surface = Color(0xFF2A2A2A)     // input, chips, elevated
    val border = Color(0xFF2A2A2A)      // 1px separators
    val rail = Color(0xFF4D4D4D)        // slider rail

    val accent = Color(0xFF58CC86)      // primary green
    val accentHi = Color(0xFF6FD699)    // hover
    val accentLo = Color(0xFF46B873)    // pressed
    val accentVeil = Color(0x1A58CC86)  // 10% veil for active nav

    val textPrimary = Color(0xFFFFFFFF)
    val textSecondary = Color(0xFFB3B3B3)
    val textTertiary = Color(0xFF6A6A6A)
    val onAccent = Color(0xFF0A0A0A)    // icon over green pill
}
