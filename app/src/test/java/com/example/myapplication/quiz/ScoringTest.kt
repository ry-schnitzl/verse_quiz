package com.example.myapplication.quiz

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoringTest {
    private val john316 = Verse("John", 43, 3, 16, "For God so loved the world…")

    private fun score(book: String, chapter: String, verse: String, rules: ScoringRules = ScoringRules.WHOLE_BIBLE) =
        scoreGuess(john316, Guess(book, chapter, verse), rules)

    @Test
    fun readmeExamples() {
        assertEquals(25, score("John", "3", "16").total)
        assertEquals(15, score("John", "3", "18").total)
        assertEquals(3, score("John", "5", "16").total)
        assertEquals(0, score("Luke", "3", "16").total)
    }

    @Test
    fun verseNeedsExactChapter() {
        val result = score("John", "4", "16")
        assertEquals(3, result.chapterScore)
        assertEquals(0, result.verseScore)
    }

    @Test
    fun blankOrNonNumericPartsScoreZero() {
        assertEquals(1, score("John", "", "").total)
        assertEquals(1, score("John", "three", "16").total)
        assertEquals(5, score("John", "3", "").total)
    }

    @Test
    fun singleBookMovesBookPointToChapter() {
        assertEquals(25, score("John", "3", "16", ScoringRules.SINGLE_BOOK).total)
        assertEquals(4, score("John", "4", "16", ScoringRules.SINGLE_BOOK).total)
    }

    @Test
    fun bookNamesNormalize() {
        listOf("1 John", "1john", "First John", "1st John", "I John", " i john").forEach {
            assertEquals(it, "ijohn", normalizeBook(it))
        }
        assertEquals("iiijohn", normalizeBook("3rd John"))
        assertEquals("revelationofjohn", normalizeBook("Revelation"))
        assertEquals("songofsolomon", normalizeBook("Song of Songs"))
        assertEquals("psalms", normalizeBook("psalm"))
    }

    @Test
    fun suggestionsPreferPrefixMatches() {
        val books = listOf("Job", "Joel", "John", "I John", "Revelation of John")
        assertEquals(listOf("John", "I John", "Revelation of John"), suggestBooks("joh", books))
        assertEquals(emptyList<String>(), suggestBooks("john", books))
        assertEquals(listOf("I John"), suggestBooks("1 jo", books))
        assertEquals(emptyList<String>(), suggestBooks("", books))
    }

    @Test
    fun parseSkipsHeaderAndBlankVerses() {
        val bible = Bible.parse(
            sequenceOf(
                "Book\tChapter\tVerse\tText",
                "Genesis\t1\t1\tIn the beginning…",
                "Matthew\t17\t21\t",
                "Matthew\t17\t22\tWhen they gathered…",
            )
        )
        assertEquals(listOf("Genesis", "Matthew"), bible.books)
        assertEquals(2, bible.verses.size)
        assertEquals(2, bible.verses[1].bookNumber)
        assertEquals("I John", Bible(emptyList(), listOf("I John")).findBook("1 john"))
        assertNull(bible.findBook("Hezekiah"))
    }

    @Test
    fun eachStepNeedsAnExactAnswer() {
        assertTrue(isExact(GuessStep.BOOK, "john", john316))
        assertFalse(isExact(GuessStep.BOOK, "I John", john316))
        assertTrue(isExact(GuessStep.CHAPTER, "3", john316))
        assertFalse(isExact(GuessStep.CHAPTER, "4", john316))
        assertTrue(isExact(GuessStep.VERSE, "016", john316))
        assertFalse(isExact(GuessStep.VERSE, "15", john316))
        assertFalse(isExact(GuessStep.VERSE, "", john316))
    }
}
