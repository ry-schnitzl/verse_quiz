package com.example.myapplication.quiz

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Saved verse sets as JSON, grouped by book then chapter to keep big sets small:
 * `[{"id": 1, "name": "Memory verses", "verses": {"John": {"3": [16, 17]}}}]`
 */
fun encodeVerseSets(sets: List<VerseSet>): String {
    val array = JSONArray()
    for (set in sets) {
        val books = JSONObject()
        set.verses.groupBy { it.book }.forEach { (book, inBook) ->
            val chapters = JSONObject()
            inBook.groupBy { it.chapter }.toSortedMap().forEach { (chapter, refs) ->
                chapters.put(chapter.toString(), JSONArray(refs.map { it.verse }.sorted()))
            }
            books.put(book, chapters)
        }
        array.put(
            JSONObject()
                .put("id", set.id)
                .put("name", set.name)
                .put("verses", books)
        )
    }
    return array.toString()
}

/** The sets in [json], or none if it can't be read. */
fun decodeVerseSets(json: String): List<VerseSet> = try {
    val array = JSONArray(json)
    List(array.length()) { i ->
        val set = array.getJSONObject(i)
        val books = set.getJSONObject("verses")
        val verses = buildSet {
            for (book in books.keys()) {
                val chapters = books.getJSONObject(book)
                for (chapter in chapters.keys()) {
                    val numbers = chapters.getJSONArray(chapter)
                    repeat(numbers.length()) { add(VerseRef(book, chapter.toInt(), numbers.getInt(it))) }
                }
            }
        }
        VerseSet(set.getLong("id"), set.getString("name"), verses)
    }
} catch (e: JSONException) {
    emptyList()
} catch (e: NumberFormatException) {
    emptyList()
}
