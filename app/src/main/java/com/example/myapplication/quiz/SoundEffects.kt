package com.example.myapplication.quiz

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.example.myapplication.R

/**
 * Short game sounds. SoundPool keeps them in memory so they play without delay.
 * The files are made by tools/make_sounds.py.
 */
class SoundEffects(context: Context) {
    private val pool = SoundPool.Builder()
        .setMaxStreams(2)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val click = pool.load(context, R.raw.click, 1)
    private val fanfare = pool.load(context, R.raw.fanfare, 1)

    fun playClick() = play(click)

    fun playFanfare() = play(fanfare)

    private fun play(sound: Int) {
        pool.play(sound, 1f, 1f, 1, 0, 1f)
    }

    fun release() = pool.release()
}
