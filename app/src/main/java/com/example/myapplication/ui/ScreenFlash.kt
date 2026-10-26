package com.example.myapplication.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

private const val FLASH_MILLIS = 700
/** The outline is drawn as this many bands, fading toward the middle of the screen. */
private const val BANDS = 6
private val BAND_WIDTH = 3.dp
private val WRONG_RED = Color(0xFFD93A3A)

/**
 * A brief outline around the screen that fades away: green each time
 * [cheers] goes up, red each time [misses] does. Touches pass through.
 */
@Composable
fun ScreenFlash(cheers: Int, misses: Int) {
    // Not replayed when the screen is rebuilt, such as after turning the phone.
    var seenCheers by rememberSaveable { mutableIntStateOf(cheers) }
    var seenMisses by rememberSaveable { mutableIntStateOf(misses) }
    var color by remember { mutableStateOf(CHECK_GREEN) }
    val glow = remember { Animatable(0f) }
    LaunchedEffect(cheers, misses) {
        color = when {
            cheers > seenCheers -> CHECK_GREEN
            misses > seenMisses -> WRONG_RED
            else -> return@LaunchedEffect
        }
        seenCheers = cheers
        seenMisses = misses
        glow.snapTo(1f)
        glow.animateTo(0f, tween(FLASH_MILLIS))
    }

    // Read the glow only while drawing, so each frame redraws without recomposing.
    Canvas(Modifier.fillMaxSize()) {
        val amount = glow.value
        if (amount <= 0f) return@Canvas
        val band = BAND_WIDTH.toPx()
        for (i in 0 until BANDS) {
            val inset = band * i + band / 2
            drawRect(
                color.copy(alpha = amount * (1f - i.toFloat() / BANDS)),
                topLeft = Offset(inset, inset),
                size = Size(size.width - 2 * inset, size.height - 2 * inset),
                style = Stroke(band),
            )
        }
    }
}
