package com.example.myapplication.quiz

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RichTextTest {
    private val boldWorld = RichText("hello world").toggled(6, 11, BOLD)

    @Test
    fun toggleTurnsOnThenOff() {
        assertEquals(listOf(StyleRun(6, 11, BOLD)), boldWorld.runs())
        assertTrue(boldWorld.allHave(6, 11, BOLD))
        // Part of the range is already bold, so the whole range turns bold.
        val more = boldWorld.toggled(4, 11, BOLD)
        assertEquals(listOf(StyleRun(4, 11, BOLD)), more.runs())
        assertEquals(emptyList<StyleRun>(), more.toggled(4, 11, BOLD).runs())
    }

    @Test
    fun boldAndItalicCombine() {
        val both = boldWorld.toggled(0, 8, ITALIC)
        assertEquals(
            listOf(StyleRun(0, 6, ITALIC), StyleRun(6, 8, BOLD or ITALIC), StyleRun(8, 11, BOLD)),
            both.runs(),
        )
    }

    @Test
    fun typingPicksUpTheStyleItsTypedInto() {
        assertEquals(listOf(StyleRun(6, 12, BOLD)), boldWorld.edited("hello world!").runs())
        assertEquals(listOf(StyleRun(7, 12, BOLD)), boldWorld.edited("hello, world").runs())
        assertEquals(listOf(StyleRun(6, 11, BOLD)), boldWorld.edited("hello world!", style = 0).runs())
    }

    @Test
    fun deletingKeepsTheRest() {
        assertEquals(listOf(StyleRun(0, 5, BOLD)), boldWorld.edited("world").runs())
        assertEquals(listOf(StyleRun(6, 9, BOLD)), boldWorld.edited("hello wod").runs())
    }

    @Test
    fun replacingRepeatedLettersStaysInBounds() {
        val text = RichText("aaaa").toggled(0, 2, BOLD)
        val edited = text.edited("aa")
        assertEquals(2, edited.styles.size)
        assertFalse(edited.allHave(0, 0, BOLD))
    }

    @Test
    fun runsRoundTrip() {
        val both = boldWorld.toggled(0, 8, ITALIC)
        assertEquals(both, RichText.fromRuns(both.text, both.runs()))
    }
}
