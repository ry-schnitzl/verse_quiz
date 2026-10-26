package com.example.myapplication.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.myapplication.quiz.Bible
import com.example.myapplication.quiz.QuizScreen
import com.example.myapplication.quiz.QuizViewModel
import com.example.myapplication.quiz.VerseRef

/** How much of a book or chapter is in the set. */
private enum class Picked(val description: String) {
    NONE("None picked"),
    SOME("Some picked"),
    ALL("All picked"),
}

private fun picked(count: Int, total: Int) = when {
    count == 0 -> Picked.NONE
    count >= total -> Picked.ALL
    else -> Picked.SOME
}

private enum class Confirm { DISCARD, DELETE }

/** Pick a verse set's verses: a book, then a chapter, then verses. */
@Composable
fun VerseSetEditor(viewModel: QuizViewModel, screen: QuizScreen.EditingSet) {
    val bible = viewModel.bible ?: return
    val original = screen.original
    val draft = viewModel.draftVerses
    var book by rememberSaveable { mutableStateOf<String?>(null) }
    var chapter by rememberSaveable { mutableStateOf<Int?>(null) }
    var confirming by remember { mutableStateOf<Confirm?>(null) }
    val click = LocalUiClick.current

    val changed = draft != original?.verses.orEmpty()
    fun leave() {
        if (changed) confirming = Confirm.DISCARD else viewModel.openVerseSets()
    }
    /** Pick all of [refs], or none of them if they're all picked already. */
    fun toggle(refs: List<VerseRef>, picked: Picked) {
        click()
        viewModel.selectDraftVerses(refs, picked != Picked.ALL)
    }

    // Back steps up one level before leaving.
    BackHandler {
        when {
            chapter != null -> chapter = null
            book != null -> book = null
            else -> leave()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                original?.name ?: "New verse set",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
            )
            TextButton(onClick = {
                click()
                leave()
            }) { Text("Cancel") }
            if (original != null) MenuButton(listOf("Delete" to { confirming = Confirm.DELETE }))
        }

        Breadcrumbs(
            book = book,
            chapter = chapter,
            onAllBooks = {
                book = null
                chapter = null
            },
            onBook = { chapter = null },
        )

        val levelModifier = Modifier
            .fillMaxWidth()
            .weight(1f)
        val openBook = book
        val openChapter = chapter
        when {
            openBook == null -> BookList(
                bible = bible,
                draft = draft,
                onOpen = {
                    click()
                    book = it
                },
                onToggle = { b, picked -> toggle(bible.refsIn(b), picked) },
                modifier = levelModifier,
            )
            openChapter == null -> ChapterGrid(
                bible = bible,
                book = openBook,
                draft = draft,
                onOpen = {
                    click()
                    chapter = it
                },
                onToggle = { c, picked -> toggle(bible.refsIn(openBook, c), picked) },
                onToggleBook = { picked -> toggle(bible.refsIn(openBook), picked) },
                modifier = levelModifier,
            )
            else -> VerseGrid(
                refs = bible.refsIn(openBook, openChapter),
                title = "$openBook $openChapter",
                draft = draft,
                onToggle = { ref -> toggle(listOf(ref), if (ref in draft) Picked.ALL else Picked.NONE) },
                onToggleChapter = { picked -> toggle(bible.refsIn(openBook, openChapter), picked) },
                modifier = levelModifier,
            )
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${versesLabel(draft.size)} picked",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (draft.isNotEmpty()) {
                TextButton(onClick = {
                    click()
                    viewModel.clearDraftVerses()
                }) { Text("Clear all") }
            }
        }
        Button(
            onClick = {
                click()
                viewModel.saveDraft()
            },
            enabled = draft.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
        ) {
            Text(
                if (draft.isEmpty()) "Pick some verses to save" else "Save",
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }

    confirming?.let { action ->
        AlertDialog(
            onDismissRequest = { confirming = null },
            title = {
                Text(
                    when (action) {
                        Confirm.DISCARD -> "Discard your changes?"
                        Confirm.DELETE -> "Delete \"${original?.name}\"?"
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    click()
                    confirming = null
                    when (action) {
                        Confirm.DISCARD -> viewModel.openVerseSets()
                        Confirm.DELETE -> original?.let(viewModel::deleteVerseSet)
                    }
                }) { Text(if (action == Confirm.DISCARD) "Discard" else "Delete") }
            },
            dismissButton = {
                TextButton(onClick = {
                    click()
                    confirming = null
                }) { Text(if (action == Confirm.DISCARD) "Keep editing" else "Keep it") }
            },
        )
    }
}

/** Where the player is, "Books › John › 3", with the earlier steps tappable. */
@Composable
private fun Breadcrumbs(book: String?, chapter: Int?, onAllBooks: () -> Unit, onBook: () -> Unit) {
    val click = LocalUiClick.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Crumb("Books", current = book == null) {
            click()
            onAllBooks()
        }
        if (book != null) {
            Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Crumb(book, current = chapter == null) {
                click()
                onBook()
            }
        }
        if (chapter != null) {
            Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Crumb("Chapter $chapter", current = true) {}
        }
    }
}

@Composable
private fun Crumb(label: String, current: Boolean, onClick: () -> Unit) {
    if (current) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
    } else {
        TextButton(onClick = onClick) { Text(label) }
    }
}

@Composable
private fun BookList(
    bible: Bible,
    draft: Set<VerseRef>,
    onOpen: (String) -> Unit,
    onToggle: (String, Picked) -> Unit,
    modifier: Modifier,
) {
    val totals = remember(bible) { bible.chapters.mapValues { (_, chapters) -> chapters.values.sumOf { it.size } } }
    val counts = remember(draft) { draft.groupingBy { it.book }.eachCount() }

    @Composable
    fun row(book: String) {
        val count = counts[book] ?: 0
        val total = totals[book] ?: 0
        BookChoice(book, count, total, picked(count, total), onOpen, onToggle)
    }

    LazyColumn(modifier) {
        item { BookGroupHeader("Old Testament") }
        items(bible.oldTestamentBooks) { row(it) }
        item { BookGroupHeader("New Testament") }
        items(bible.newTestamentBooks) { row(it) }
    }
}

/** A book's row: the checkbox picks the whole book, the rest opens its chapters. */
@Composable
private fun BookChoice(
    book: String,
    count: Int,
    total: Int,
    picked: Picked,
    onOpen: (String) -> Unit,
    onToggle: (String, Picked) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable { onOpen(book) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TriStateCheckbox(
            state = when (picked) {
                Picked.NONE -> ToggleableState.Off
                Picked.SOME -> ToggleableState.Indeterminate
                Picked.ALL -> ToggleableState.On
            },
            onClick = { onToggle(book, picked) },
        )
        Column(Modifier.weight(1f)) {
            Text(book, style = MaterialTheme.typography.bodyLarge)
            if (picked == Picked.SOME) {
                Text(
                    "$count of $total verses",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            "›",
            modifier = Modifier.padding(horizontal = 12.dp),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ChapterGrid(
    bible: Bible,
    book: String,
    draft: Set<VerseRef>,
    onOpen: (Int) -> Unit,
    onToggle: (Int, Picked) -> Unit,
    onToggleBook: (Picked) -> Unit,
    modifier: Modifier,
) {
    val chapters = bible.chapters[book].orEmpty()
    val counts = remember(draft, book) {
        draft.filter { it.book == book }.groupingBy { it.chapter }.eachCount()
    }
    val bookPicked = picked(counts.values.sum(), chapters.values.sumOf { it.size })

    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LevelHeader(book, bookPicked, onToggleBook)
        Text(
            "Tap a chapter to pick its verses. Hold it to pick the whole chapter.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        NumberGrid {
            items(chapters.keys.toList()) { chapter ->
                val picked = picked(counts[chapter] ?: 0, chapters.getValue(chapter).size)
                NumberSquare(
                    number = chapter,
                    picked = picked,
                    onClick = { onOpen(chapter) },
                    onLongClick = { onToggle(chapter, picked) },
                )
            }
        }
    }
}

@Composable
private fun VerseGrid(
    refs: List<VerseRef>,
    title: String,
    draft: Set<VerseRef>,
    onToggle: (VerseRef) -> Unit,
    onToggleChapter: (Picked) -> Unit,
    modifier: Modifier,
) {
    val chapterPicked = picked(refs.count { it in draft }, refs.size)

    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LevelHeader(title, chapterPicked, onToggleChapter)
        Text(
            "Tap verses to pick them.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        NumberGrid {
            items(refs) { ref ->
                NumberSquare(
                    number = ref.verse,
                    picked = if (ref in draft) Picked.ALL else Picked.NONE,
                    onClick = { onToggle(ref) },
                )
            }
        }
    }
}

/** The open book or chapter's name, with a button to pick or clear all of it. */
@Composable
private fun LevelHeader(title: String, picked: Picked, onToggle: (Picked) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        TextButton(onClick = { onToggle(picked) }) {
            Text(if (picked == Picked.ALL) "Clear" else "Pick all")
        }
    }
}

@Composable
private fun NumberGrid(content: LazyGridScope.() -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 56.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

/** A chapter or verse number as a rounded square, filled in as much as it's picked. */
@Composable
private fun NumberSquare(
    number: Int,
    picked: Picked,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    val (container, content) = when (picked) {
        Picked.NONE -> colors.surfaceVariant to colors.onSurfaceVariant
        Picked.SOME -> colors.primaryContainer to colors.onPrimaryContainer
        Picked.ALL -> colors.primary to colors.onPrimary
    }
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(MaterialTheme.shapes.medium)
            .background(container)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .semantics { stateDescription = picked.description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            number.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (picked == Picked.NONE) FontWeight.Normal else FontWeight.Bold,
            color = content,
            textAlign = TextAlign.Center,
        )
        // A small bar marks a partly picked chapter, so it doesn't rely on color alone.
        if (picked == Picked.SOME) {
            Spacer(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
                    .size(width = 16.dp, height = 3.dp)
                    .clip(CircleShape)
                    .background(content)
            )
        }
    }
}
