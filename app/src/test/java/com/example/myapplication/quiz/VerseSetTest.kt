package com.example.myapplication.quiz

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VerseSetTest {
    private val bible = Bible.parse(
        sequenceOf(
            "Book\tChapter\tVerse\tText",
            "John\t3\t16\tFor God so loved the world…",
            "John\t3\t17\tFor God did not send His Son…",
            "John\t4\t1\tNow Jesus learned…",
            "Romans\t8\t28\tAnd we know…",
        )
    )

    private fun custom(vararg refs: VerseRef) = Scope.Custom(VerseSet(1, "Mine", refs.toSet()))

    @Test
    fun customScopeKeepsOnlyPickedVerses() {
        val scope = custom(VerseRef("John", 3, 17), VerseRef("Romans", 8, 28))
        assertEquals(listOf("John 3:17", "Romans 8:28"), bible.versesIn(scope).map { it.reference })
    }

    @Test
    fun oneBookSetsDontAskForTheBook() {
        val johnOnly = custom(VerseRef("John", 3, 16), VerseRef("John", 4, 1))
        assertEquals("John", johnOnly.onlyBook)
        assertEquals(ScoringRules.SINGLE_BOOK, ScoringRules.forScope(johnOnly))

        val mixed = custom(VerseRef("John", 3, 16), VerseRef("Romans", 8, 28))
        assertNull(mixed.onlyBook)
        assertEquals(ScoringRules.WHOLE_BIBLE, ScoringRules.forScope(mixed))
    }

    @Test
    fun chaptersIndexBooksChaptersAndVerses() {
        assertEquals(listOf(3, 4), bible.chapters.getValue("John").keys.toList())
        assertEquals(listOf(16, 17), bible.refsIn("John", 3).map { it.verse })
        assertEquals(3, bible.refsIn("John").size)
    }
}
