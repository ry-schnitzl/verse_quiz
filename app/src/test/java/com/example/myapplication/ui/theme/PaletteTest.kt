package com.example.myapplication.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

class PaletteTest {
    private fun contrast(a: Color, b: Color): Float {
        val (light, dark) = listOf(a.luminance(), b.luminance()).sortedDescending()
        return (light + 0.05f) / (dark + 0.05f)
    }

    /** Text on buttons, cards and the background stays readable (WCAG AA, 4.5:1). */
    @Test
    fun everyPaletteHasReadableText() {
        for (palette in Palette.entries.filter { it != Palette.DEVICE }) {
            for (dark in listOf(false, true)) {
                val c = paletteColorScheme(palette, dark)
                val pairs = mapOf(
                    "onPrimary/primary" to (c.onPrimary to c.primary),
                    "onPrimaryContainer/primaryContainer" to (c.onPrimaryContainer to c.primaryContainer),
                    "onSurface/surface" to (c.onSurface to c.surface),
                    "onSurfaceVariant/surfaceContainerHighest" to
                        (c.onSurfaceVariant to c.surfaceContainerHighest),
                    "primary/surface" to (c.primary to c.surface),
                    "error/surface" to (c.error to c.surface),
                )
                for ((name, colors) in pairs) {
                    val ratio = contrast(colors.first, colors.second)
                    assertTrue("$palette dark=$dark $name is $ratio", ratio >= 4.5f)
                }
            }
        }
    }
}
