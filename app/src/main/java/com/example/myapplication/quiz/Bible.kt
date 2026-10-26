package com.example.myapplication.quiz

/** Genesis through Malachi. Books are numbered in file (canonical) order. */
const val OLD_TESTAMENT_BOOKS = 39

data class Verse(
    val book: String,
    val bookNumber: Int,
    val chapter: Int,
    val verse: Int,
    val text: String,
) {
    val reference get() = "$book $chapter:$verse"
    val ref get() = VerseRef(book, chapter, verse)
}

/** Where a verse is, without its text. */
data class VerseRef(val book: String, val chapter: Int, val verse: Int)

/** Verses the player picked and named, saved for later games. */
data class VerseSet(val id: Long, val name: String, val verses: Set<VerseRef>)

/** Which verses a game draws from. */
sealed interface Scope {
    val label: String

    /** The one book every verse comes from, if there is one. Its games don't ask for the book. */
    val onlyBook: String? get() = null

    data object WholeBible : Scope {
        override val label = "the whole Bible"
    }

    data object OldTestament : Scope {
        override val label = "the Old Testament"
    }

    data object NewTestament : Scope {
        override val label = "the New Testament"
    }

    data class SingleBook(val book: String) : Scope {
        override val label get() = book
        override val onlyBook get() = book
    }

    data class Custom(val set: VerseSet) : Scope {
        override val label get() = set.name
        override val onlyBook = set.verses.mapTo(HashSet()) { it.book }.singleOrNull()
    }
}

class Bible(
    /** Every verse that has text. The BSB leaves 16 verses blank; they're skipped. */
    val verses: List<Verse>,
    /** All book names, in canonical order. */
    val books: List<String>,
) {
    val oldTestamentBooks get() = books.take(OLD_TESTAMENT_BOOKS)
    val newTestamentBooks get() = books.drop(OLD_TESTAMENT_BOOKS)

    /** Each book's chapters, and each chapter's verses, in order. */
    val chapters: Map<String, Map<Int, List<VerseRef>>> by lazy {
        verses.groupBy { it.book }.mapValues { (_, inBook) ->
            inBook.groupBy({ it.chapter }, { it.ref })
        }
    }

    fun refsIn(book: String): List<VerseRef> = chapters[book].orEmpty().values.flatten()

    fun refsIn(book: String, chapter: Int): List<VerseRef> = chapters[book]?.get(chapter).orEmpty()

    fun versesIn(scope: Scope): List<Verse> = when (scope) {
        Scope.WholeBible -> verses
        Scope.OldTestament -> verses.filter { it.bookNumber <= OLD_TESTAMENT_BOOKS }
        Scope.NewTestament -> verses.filter { it.bookNumber > OLD_TESTAMENT_BOOKS }
        is Scope.SingleBook -> verses.filter { it.book == scope.book }
        is Scope.Custom -> verses.filter { it.ref in scope.set.verses }
    }

    /** Find a book by any name [normalizeBook] accepts, such as "1 john" for "I John". */
    fun findBook(name: String): String? {
        val key = normalizeBook(name)
        return books.firstOrNull { normalizeBook(it) == key }
    }

    companion object {
        /**
         * Parse BSB.tsv: a header row, then `Book`, `Chapter`, `Verse`, `Text`
         * separated by tabs, one verse per line.
         */
        fun parse(lines: Sequence<String>): Bible {
            val verses = mutableListOf<Verse>()
            val books = mutableListOf<String>()
            for (line in lines.drop(1)) {
                val fields = line.trimEnd('\r').split('\t', limit = 4)
                if (fields.size < 3) continue
                val book = fields[0]
                if (books.lastOrNull() != book) books += book
                val text = fields.getOrNull(3).orEmpty()
                val chapter = fields[1].toIntOrNull()
                val verse = fields[2].toIntOrNull()
                if (text.isBlank() || chapter == null || verse == null) continue
                verses += Verse(book, books.size, chapter, verse, text)
            }
            return Bible(verses, books)
        }
    }
}
