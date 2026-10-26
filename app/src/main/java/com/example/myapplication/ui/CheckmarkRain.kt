package com.example.myapplication.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** How long one shower lasts, from the first checkmark to the last one leaving. */
private const val SHOWER_MILLIS = 3600
/** Checkmarks in one shower. */
private const val DROPS = 18
/** The latest a checkmark starts falling, as a share of the shower. */
private const val MAX_DELAY = 0.35f
internal val CHECK_GREEN = Color(0xFF2EA84F)

/** One falling checkmark. Positions are shares of the screen; size is in dp. */
private class Drop(
    val x: Float,
    val delay: Float,
    val size: Float,
    val sway: Float,
    val phase: Float,
    val tilt: Float,
)

/**
 * Green checkmarks falling slowly down the screen, a new shower each time
 * [cheers] goes up. Touches pass through to the screen underneath.
 */
@Composable
fun CheckmarkRain(cheers: Int) {
    val showers = remember { mutableStateListOf<Int>() }
    // Not replayed when the screen is rebuilt, such as after turning the phone.
    var seen by rememberSaveable { mutableIntStateOf(cheers) }
    LaunchedEffect(cheers) {
        if (cheers > seen) {
            seen = cheers
            showers += cheers
        }
    }
    Box(Modifier.fillMaxSize()) {
        for (id in showers) {
            key(id) { Shower(onDone = { showers.remove(id) }) }
        }
    }
}

@Composable
private fun Shower(onDone: () -> Unit) {
    val drops = remember {
        List(DROPS) {
            Drop(
                x = Random.nextFloat(),
                delay = Random.nextFloat() * MAX_DELAY,
                size = 26f + Random.nextFloat() * 22f,
                sway = 6f + Random.nextFloat() * 14f,
                phase = Random.nextFloat() * 2 * PI.toFloat(),
                tilt = Random.nextFloat() * 40f - 20f,
            )
        }
    }
    val time = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        time.animateTo(1f, tween(SHOWER_MILLIS, easing = LinearEasing))
        onDone()
    }
    val check = remember { Path() }

    // Read the time only while drawing, so each frame redraws without recomposing.
    Canvas(Modifier.fillMaxSize()) {
        val t = time.value
        for (drop in drops) {
            val fall = (t - drop.delay) / (1f - MAX_DELAY)
            if (fall <= 0f || fall >= 1f) continue
            val s = drop.size.dp.toPx()
            val x = drop.x * size.width + sin(fall * 2 * PI.toFloat() + drop.phase) * drop.sway.dp.toPx()
            val y = -s + fall * (size.height + s)
            // Fade in at the top and out near the bottom.
            val alpha = minOf(1f, fall / 0.08f, (1f - fall) / 0.2f)

            check.reset()
            check.moveTo(0.12f * s, 0.55f * s)
            check.lineTo(0.4f * s, 0.82f * s)
            check.lineTo(0.9f * s, 0.22f * s)
            translate(x - s / 2, y) {
                rotate(drop.tilt + sin(fall * 4 * PI.toFloat() + drop.phase) * 10f, Offset(s / 2, s / 2)) {
                    drawPath(
                        check,
                        CHECK_GREEN.copy(alpha = alpha),
                        style = Stroke(width = s * 0.17f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                }
            }
        }
    }
}
