package com.example.myapplication.ui

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Called when an ordinary button or control is tapped, to play the click sound.
 * The view model decides whether it's heard, based on the sound setting.
 */
val LocalUiClick = staticCompositionLocalOf<() -> Unit> { {} }
