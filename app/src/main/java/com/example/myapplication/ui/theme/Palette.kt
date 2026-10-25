package com.example.myapplication.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark");

    @Composable
    fun isDark() = when (this) {
        SYSTEM -> isSystemInDarkTheme()
        LIGHT -> false
        DARK -> true
    }
}

/**
 * App color palettes. [DEVICE] follows the phone's wallpaper colors (Android 12+).
 * The rest are muted colors built from one hue: [saturation] keeps them soft, and
 * [lightness] is the shade of buttons and accents in light mode.
 */
enum class Palette(
    val label: String,
    val hue: Float,
    val saturation: Float,
    val lightness: Float = 0.38f,
) {
    DEVICE("Device", 0f, 0f),
    RED("Red", 0f, 0.42f),
    ORANGE("Orange", 22f, 0.55f, 0.40f),
    GOLD("Gold", 43f, 0.55f, 0.34f),
    GREEN("Green", 135f, 0.28f, 0.34f),
    TEAL("Teal", 172f, 0.35f, 0.30f),
    CYAN("Cyan", 190f, 0.42f, 0.33f),
    BLUE("Blue", 215f, 0.38f),
    PURPLE("Purple", 270f, 0.28f, 0.42f),
    PINK("Pink", 330f, 0.38f, 0.40f),
    BROWN("Brown", 25f, 0.28f, 0.32f),
    GRAY("Gray", 220f, 0.05f, 0.36f),
}

/** A full Material color scheme from one of the hue-based palettes. */
fun paletteColorScheme(palette: Palette, dark: Boolean): ColorScheme {
    val hue = palette.hue
    val sat = palette.saturation
    val accentHue = (hue + 40f) % 360f
    // Backgrounds and outlines get only a hint of the hue.
    val tint = minOf(sat, 0.12f)

    fun tone(lightness: Float, saturation: Float = sat, h: Float = hue) =
        Color.hsl(h, saturation.coerceIn(0f, 1f), lightness)

    fun neutral(lightness: Float) = tone(lightness, tint)

    return if (dark) {
        darkColorScheme(
            primary = tone(0.76f),
            onPrimary = tone(0.15f),
            primaryContainer = tone(0.28f),
            onPrimaryContainer = tone(0.90f),
            inversePrimary = tone(palette.lightness),
            secondary = tone(0.76f, sat * 0.5f),
            onSecondary = tone(0.15f, sat * 0.5f),
            secondaryContainer = tone(0.27f, sat * 0.5f),
            onSecondaryContainer = tone(0.90f, sat * 0.5f),
            tertiary = tone(0.76f, h = accentHue),
            onTertiary = tone(0.15f, h = accentHue),
            tertiaryContainer = tone(0.28f, h = accentHue),
            onTertiaryContainer = tone(0.90f, h = accentHue),
            background = neutral(0.07f),
            onBackground = neutral(0.90f),
            surface = neutral(0.07f),
            onSurface = neutral(0.90f),
            surfaceVariant = neutral(0.24f),
            onSurfaceVariant = neutral(0.78f),
            surfaceTint = tone(0.76f),
            inverseSurface = neutral(0.90f),
            inverseOnSurface = neutral(0.15f),
            outline = neutral(0.56f),
            outlineVariant = neutral(0.28f),
            surfaceBright = neutral(0.22f),
            surfaceDim = neutral(0.07f),
            surfaceContainerLowest = neutral(0.05f),
            surfaceContainerLow = neutral(0.10f),
            surfaceContainer = neutral(0.12f),
            surfaceContainerHigh = neutral(0.16f),
            surfaceContainerHighest = neutral(0.20f),
        )
    } else {
        lightColorScheme(
            primary = tone(palette.lightness),
            onPrimary = Color.White,
            primaryContainer = tone(0.88f),
            onPrimaryContainer = tone(0.15f),
            inversePrimary = tone(0.76f),
            secondary = tone(0.38f, sat * 0.5f),
            onSecondary = Color.White,
            secondaryContainer = tone(0.89f, sat * 0.5f),
            onSecondaryContainer = tone(0.15f, sat * 0.5f),
            tertiary = tone(palette.lightness, h = accentHue),
            onTertiary = Color.White,
            tertiaryContainer = tone(0.89f, h = accentHue),
            onTertiaryContainer = tone(0.15f, h = accentHue),
            background = neutral(0.98f),
            onBackground = neutral(0.11f),
            surface = neutral(0.98f),
            onSurface = neutral(0.11f),
            surfaceVariant = neutral(0.89f),
            onSurfaceVariant = neutral(0.30f),
            surfaceTint = tone(palette.lightness),
            inverseSurface = neutral(0.19f),
            inverseOnSurface = neutral(0.95f),
            outline = neutral(0.50f),
            outlineVariant = neutral(0.80f),
            surfaceBright = neutral(0.98f),
            surfaceDim = neutral(0.86f),
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = neutral(0.96f),
            surfaceContainer = neutral(0.94f),
            surfaceContainerHigh = neutral(0.92f),
            surfaceContainerHighest = neutral(0.90f),
        )
    }
}
