package com.Ksinius.musico.ui.theme

import androidx.compose.ui.graphics.Color
import com.Ksinius.musico.model.ThemeSettings

val PureBlack = Color(0xFF07050A)
val SurfaceDark = Color(0xFF120E1A)
val SurfaceVariantDark = Color(0xFF1C162A)
val CardBackground = Color(0xFF221A33)

/** Translucent surfaces — bubbles show through these. */
val SurfaceGlass = SurfaceDark.copy(alpha = 0.72f)
val SurfaceVariantGlass = SurfaceVariantDark.copy(alpha = 0.65f)
val CardGlass = CardBackground.copy(alpha = 0.78f)

val PurplePrimary = Color(0xFFA855F7)
val PurpleSecondary = Color(0xFFC084FC)
val PurpleDark = Color(0xFF6B21A8)
val PurpleAccent = Color(0xFFE9D5FF)

val TextPrimary = Color(0xFFF9FAFB)
val TextSecondary = Color(0xFF9CA3AF)
val TextMuted = Color(0xFF6B7280)

val PlayheadColor = Color(0xFFD8B4FE)
val RipplePurple = Color(0x33A855F7)

// Dynamic theme colors based on RGB sliders
fun dynamicPrimaryColor(themeSettings: ThemeSettings): Color = themeSettings.toColor()
fun dynamicSecondaryColor(themeSettings: ThemeSettings): Color = themeSettings.toLighterColor()
fun dynamicDarkColor(themeSettings: ThemeSettings): Color = themeSettings.toDarkerColor()
fun dynamicAccentColor(themeSettings: ThemeSettings): Color = themeSettings.toLighterColor().copy(alpha = 0.8f)