package com.example.myapplication.quiz

import kotlin.math.abs

/**
 * Points per verse, the same as verse_quiz.sh:
 *   Book:    [bookPoints] if correct.
 *   Combo points (only awarded when the book is correct):
 *     Chapter: [chapterPoints] minus [chapterPenalty] per number off, never below 0.
 *     Verse:   [versePoints] minus [versePenalty] per number off, never below 0.
 *              Only awarded when the chapter is also exactly right.
 */
data class ScoringRules(
    val bookPoints: Int = 1,
    val chapterPoints: Int = 4,
    val chapterPenalty: Int = 1,
    val versePoints: Int = 20,
    val versePenalty: Int = 5,
) {
    val maxPerVerse get() = bookPoints + chapterPoints + versePoints

    companion object {
        val WHOLE_BIBLE = ScoringRules()

        /** When playing a single book, the book isn't asked and its point goes to the chapter. */
        val SINGLE_BOOK = ScoringRules(bookPoints = 0, chapterPoints = 5)

        fun forScope(scope: Scope) =
            if (scope.onlyBook != null) SINGLE_BOOK else WHOLE_BIBLE
    }
}

/** What the player typed. Any part may be blank. */
data class Guess(val book: String, val chapter: String, val verse: String)

data class VerseResult(
    val verse: Verse,
    val guess: Guess,
    val bookRight: Boolean,
    val bookScore: Int,
    val chapterScore: Int,
    val verseScore: Int,
) {
    val comboScore get() = chapterScore + verseScore
    val total get() = bookScore + comboScore
}

private val ORDINAL_PREFIXES = listOf(
    Regex("^(1st|first|1)\\s*") to "i ",
    Regex("^(2nd|second|2)\\s*") to "ii ",
    Regex("^(3rd|third|3)\\s*") to "iii ",
)

private val BOOK_ALIASES = mapOf(
    "revelation" to "revelationofjohn",
    "revelations" to "revelationofjohn",
    "songofsongs" to "songofsolomon",
    "psalm" to "psalms",
)

/**
 * Turn a book name into a comparable key, so "1 john", "1John", "First John"
 * and "I John" all match the file's "I John".
 */
fun normalizeBook(name: String): String {
    var key = name.lowercase().trimStart()
    for ((prefix, roman) in ORDINAL_PREFIXES) {
        key = key.replaceFirst(prefix, roman)
    }
    key = key.replace(Regex("[^a-z]"), "")
    return BOOK_ALIASES[key] ?: key
}

/**
 * Max points minus penalty per number off, floored at 0.
 * A guess that isn't a whole number scores 0.
 */
fun closenessPoints(guess: String, answer: Int, max: Int, penalty: Int): Int {
    val number = guess.filterNot { it.isWhitespace() }
        .takeIf { it.isNotEmpty() && it.all { c -> c in '0'..'9' } }
        ?.toLongOrNull()
        ?: return 0
    val points = max - abs(number - answer) * penalty
    return points.coerceAtLeast(0).toInt()
}

fun scoreGuess(verse: Verse, guess: Guess, rules: ScoringRules): VerseResult {
    val bookRight = normalizeBook(guess.book) == normalizeBook(verse.book)
    if (!bookRight) return VerseResult(verse, guess, false, 0, 0, 0)

    val chapterScore =
        closenessPoints(guess.chapter, verse.chapter, rules.chapterPoints, rules.chapterPenalty)
    val verseScore = if (chapterScore == rules.chapterPoints) {
        closenessPoints(guess.verse, verse.verse, rules.versePoints, rules.versePenalty)
    } else {
        0
    }
    return VerseResult(verse, guess, true, rules.bookPoints, chapterScore, verseScore)
}

/** Books to suggest as the player types: prefix matches first, then other matches. */
fun suggestBooks(input: String, books: List<String>, limit: Int = 6): List<String> {
    val key = normalizeBook(input)
    if (key.isEmpty()) return emptyList()
    val keyed = books.map { it to normalizeBook(it) }
    if (keyed.any { it.second == key }) return emptyList()
    val prefix = keyed.filter { it.second.startsWith(key) }.map { it.first }
    val contains = keyed.filter { key in it.second && it.first !in prefix }.map { it.first }
    return (prefix + contains).take(limit)
}
