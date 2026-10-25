package com.example.myapplication.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

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
 * The rest are built from one hue: [saturation] keeps them soft (muted), while
 * [vibrantSaturation] is used when vibrant colors are on. [lightness] is the shade
 * of buttons and accents in light mode, darkened further if white text needs it.
 */
enum class Palette(
    val label: String,
    val hue: Float,
    val saturation: Float,
    val lightness: Float = 0.38f,
    val vibrantSaturation: Float = 0.85f,
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
    BROWN("Brown", 25f, 0.28f, 0.32f, vibrantSaturation = 0.5f),
    GRAY("Gray", 220f, 0.05f, 0.36f, vibrantSaturation = 0.12f),
}

/** WCAG contrast ratio between two colors, from 1 (none) to 21 (black on white). */
fun contrastRatio(a: Color, b: Color): Float {
    val lighter = maxOf(a.luminance(), b.luminance())
    val darker = minOf(a.luminance(), b.luminance())
    return (lighter + 0.05f) / (darker + 0.05f)
}

/**
 * The lightest shade, up to [start], that white text can sit on and stay readable.
 * Aims past the 4.5 minimum so the shade also reads as text on the tinted background.
 */
private fun shadeForWhiteText(hue: Float, saturation: Float, start: Float): Float {
    var lightness = start
    while (lightness > 0.15f &&
        contrastRatio(Color.White, Color.hsl(hue, saturation, lightness)) < 5f
    ) {
        lightness -= 0.01f
    }
    return lightness
}

/** A full Material color scheme from one of the hue-based palettes. */
fun paletteColorScheme(palette: Palette, dark: Boolean, vibrant: Boolean = false): ColorScheme {
    val hue = palette.hue
    val sat = if (vibrant) palette.vibrantSaturation else palette.saturation
    val accentHue = (hue + 40f) % 360f
    // Backgrounds and outlines get only a hint of the hue.
    val tint = minOf(sat, 0.12f)

    fun tone(lightness: Float, saturation: Float = sat, h: Float = hue) =
        Color.hsl(h, saturation.coerceIn(0f, 1f), lightness)

    fun neutral(lightness: Float) = tone(lightness, tint)

    // Light-mode shades for buttons and accents, which carry white text.
    val primaryShade = shadeForWhiteText(hue, sat, palette.lightness)
    val secondaryShade = shadeForWhiteText(hue, sat * 0.5f, 0.38f)
    val tertiaryShade = shadeForWhiteText(accentHue, sat, palette.lightness)
    // Dark-mode highlight backgrounds go a little darker when vibrant, so light text stays readable.
    val darkContainer = if (vibrant) 0.24f else 0.28f

    return if (dark) {
        darkColorScheme(
            primary = tone(0.76f),
            onPrimary = tone(0.15f),
            primaryContainer = tone(darkContainer),
            onPrimaryContainer = tone(0.90f),
            inversePrimary = tone(primaryShade),
            secondary = tone(0.76f, sat * 0.5f),
            onSecondary = tone(0.15f, sat * 0.5f),
            secondaryContainer = tone(darkContainer, sat * 0.5f),
            onSecondaryContainer = tone(0.90f, sat * 0.5f),
            tertiary = tone(0.76f, h = accentHue),
            onTertiary = tone(0.15f, h = accentHue),
            tertiaryContainer = tone(darkContainer, h = accentHue),
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
            primary = tone(primaryShade),
            onPrimary = Color.White,
            primaryContainer = tone(0.88f),
            onPrimaryContainer = tone(0.15f),
            inversePrimary = tone(0.76f),
            secondary = tone(secondaryShade, sat * 0.5f),
            onSecondary = Color.White,
            secondaryContainer = tone(0.89f, sat * 0.5f),
            onSecondaryContainer = tone(0.15f, sat * 0.5f),
            tertiary = tone(tertiaryShade, h = accentHue),
            onTertiary = Color.White,
            tertiaryContainer = tone(0.89f, h = accentHue),
            onTertiaryContainer = tone(0.15f, h = accentHue),
            background = neutral(0.98f),
            onBackground = neutral(0.11f),
            surface = neutral(0.98f),
            onSurface = neutral(0.11f),
            surfaceVariant = neutral(0.89f),
            onSurfaceVariant = neutral(0.30f),
            surfaceTint = tone(primaryShade),
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
