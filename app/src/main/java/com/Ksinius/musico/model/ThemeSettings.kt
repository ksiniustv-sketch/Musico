package com.Ksinius.musico.model

import androidx.compose.ui.graphics.Color

data class ThemeSettings(
    val red: Float = 0.66f,    // Default purple-ish
    val green: Float = 0.33f,
    val blue: Float = 0.97f,
    val brightness: Float = 1.0f
) {
    fun toColor(): Color {
        return Color(
            red = red.coerceIn(0f, 1f),
            green = green.coerceIn(0f, 1f),
            blue = blue.coerceIn(0f, 1f),
            alpha = 1f
        )
    }

    fun toDarkerColor(): Color {
        return Color(
            red = (red * 0.6f).coerceIn(0f, 1f),
            green = (green * 0.6f).coerceIn(0f, 1f),
            blue = (blue * 0.6f).coerceIn(0f, 1f),
            alpha = 1f
        )
    }

    fun toLighterColor(): Color {
        return Color(
            red = (red * 1.3f).coerceIn(0f, 1f),
            green = (green * 1.3f).coerceIn(0f, 1f),
            blue = (blue * 1.3f).coerceIn(0f, 1f),
            alpha = 1f
        )
    }
}
