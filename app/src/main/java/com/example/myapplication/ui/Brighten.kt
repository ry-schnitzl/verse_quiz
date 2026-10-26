package com.example.myapplication.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val BRIGHTEN_MILLIS = 50
private const val FADE_MILLIS = 100
/** How far a control brightens: a white wash at this opacity. */
private const val BRIGHTEST = 0.3f

/** Brightens a › control for a moment, then opens its page, so the tap shows first. */
class Brighten(private val scope: CoroutineScope) {
    private val glow = Animatable(0f)
    private var busy = false

    /** How bright the control is now, from 0 to 1. */
    val amount get() = glow.value

    /** Brighten, then do [action]. Taps while it's brightening are ignored. */
    fun then(action: () -> Unit) {
        if (busy) return
        busy = true
        scope.launch {
            try {
                glow.animateTo(1f, tween(BRIGHTEN_MILLIS))
                action()
                // Fades back if the control is still on screen.
                glow.animateTo(0f, tween(FADE_MILLIS))
            } finally {
                busy = false
            }
        }
    }
}

@Composable
fun rememberBrighten(): Brighten {
    val scope = rememberCoroutineScope()
    return remember { Brighten(scope) }
}

/** Draws [brighten]'s glow over this control, in its [shape]. */
fun Modifier.brightened(brighten: Brighten, shape: Shape = RectangleShape) = drawWithContent {
    drawContent()
    val amount = brighten.amount
    if (amount > 0f) {
        drawOutline(
            shape.createOutline(size, layoutDirection, this),
            Color.White.copy(alpha = BRIGHTEST * amount),
        )
    }
}
