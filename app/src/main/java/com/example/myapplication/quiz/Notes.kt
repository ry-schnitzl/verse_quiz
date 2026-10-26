package com.example.myapplication.quiz

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** Style flags for each character of [RichText]. */
const val BOLD = 1
const val ITALIC = 2

/** A stretch of [RichText] that shares one set of style flags. */
data class StyleRun(val start: Int, val end: Int, val style: Int)

/** Text where each character can be bold, italic, or both. */
data class RichText(
    val text: String = "",
    /** One set of style flags per character of [text]. */
    val styles: List<Int> = List(text.length) { 0 },
) {
    init {
        require(styles.size == text.length) { "Need one style per character" }
    }

    /** The style typing at [index] picks up: the character before it, or after it at the start. */
    fun styleAt(index: Int) = styles.getOrNull(index - 1) ?: styles.getOrNull(index) ?: 0

    /**
     * The same styles after the text changed to [newText]. Kept characters keep
     * their styles; new ones get [style], or the style of the text they were typed into.
     */
    fun edited(newText: String, style: Int? = null): RichText {
        val prefix = text.commonPrefixWith(newText).length
        val suffix = text.commonSuffixWith(newText).length
            .coerceAtMost(minOf(text.length, newText.length) - prefix)
        val inserted = newText.length - prefix - suffix
        val insertStyle = style ?: styleAt(prefix)
        return RichText(
            newText,
            styles.subList(0, prefix) +
                List(inserted) { insertStyle } +
                styles.subList(text.length - suffix, text.length),
        )
    }

    /** Whether every character from [start] to [end] has [flag]. */
    fun allHave(start: Int, end: Int, flag: Int) =
        start < end && (start until end).all { styles[it] and flag != 0 }

    /** Turn [flag] on from [start] to [end], or off if it's already on for all of it. */
    fun toggled(start: Int, end: Int, flag: Int): RichText {
        val on = !allHave(start, end, flag)
        return copy(styles = styles.mapIndexed { i, s ->
            when {
                i !in start until end -> s
                on -> s or flag
                else -> s and flag.inv()
            }
        })
    }

    /** The styled stretches, leaving out plain text. */
    fun runs(): List<StyleRun> = buildList {
        var start = 0
        for (i in 1..text.length) {
            if (i == text.length || styles[i] != styles[start]) {
                if (styles[start] != 0) add(StyleRun(start, i, styles[start]))
                start = i
            }
        }
    }

    companion object {
        fun fromRuns(text: String, runs: List<StyleRun>): RichText {
            val styles = IntArray(text.length)
            for (run in runs) {
                for (i in run.start.coerceAtLeast(0) until run.end.coerceAtMost(text.length)) {
                    styles[i] = run.style
                }
            }
            return RichText(text, styles.toList())
        }
    }
}

/** A book's study notes: a name and notes for any of its chapters. */
data class BookNotes(
    val names: Map<Int, String> = emptyMap(),
    val notes: Map<Int, RichText> = emptyMap(),
) {
    fun isEmpty() = names.isEmpty() && notes.isEmpty()
}

/**
 * A book's notes as JSON:
 * `{"names": {"3": "Born again"}, "notes": {"3": {"text": "…", "runs": [[0, 4, 1]]}}}`
 */
fun encodeBookNotes(book: BookNotes): String {
    val names = JSONObject()
    book.names.forEach { (chapter, name) -> names.put(chapter.toString(), name) }
    val notes = JSONObject()
    book.notes.forEach { (chapter, rich) ->
        val runs = JSONArray()
        rich.runs().forEach { runs.put(JSONArray(listOf(it.start, it.end, it.style))) }
        notes.put(chapter.toString(), JSONObject().put("text", rich.text).put("runs", runs))
    }
    return JSONObject().put("names", names).put("notes", notes).toString()
}

/** The notes in [json], or none if it can't be read. */
fun decodeBookNotes(json: String?): BookNotes {
    if (json == null) return BookNotes()
    return try {
        val root = JSONObject(json)
        val names = root.getJSONObject("names")
        val notes = root.getJSONObject("notes")
        BookNotes(
            names = names.keys().asSequence().associate { it.toInt() to names.getString(it) },
            notes = notes.keys().asSequence().associate { chapter ->
                val note = notes.getJSONObject(chapter)
                val runs = note.getJSONArray("runs")
                chapter.toInt() to RichText.fromRuns(
                    note.getString("text"),
                    List(runs.length()) {
                        val run = runs.getJSONArray(it)
                        StyleRun(run.getInt(0), run.getInt(1), run.getInt(2))
                    },
                )
            },
        )
    } catch (e: JSONException) {
        BookNotes()
    } catch (e: NumberFormatException) {
        BookNotes()
    }
}
