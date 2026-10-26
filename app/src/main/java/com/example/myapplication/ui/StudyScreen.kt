package com.example.myapplication.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Card
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.unit.dp
import com.example.myapplication.quiz.BOLD
import com.example.myapplication.quiz.ITALIC
import com.example.myapplication.quiz.QuizViewModel
import com.example.myapplication.quiz.RichText

/** Study mode's first page: every book, each opening its notes. */
@Composable
fun StudyScreen(viewModel: QuizViewModel) {
    val bible = viewModel.bible ?: return
    val click = LocalUiClick.current
    BackHandler { viewModel.backToSetup() }

    @Composable
    fun bookCard(book: String) {
        val brighten = rememberBrighten()
        val shape = MaterialTheme.shapes.large
        Card(
            onClick = {
                click()
                brighten.then { viewModel.openNotes(book) }
            },
            modifier = Modifier
                .fillMaxWidth()
                .brightened(brighten, shape),
            shape = shape,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(book, style = MaterialTheme.typography.titleMedium)
                    val chapters = bible.chapters[book]?.size ?: 0
                    Text(
                        if (book in viewModel.notedBooks) "$chapters chapters · Has notes" else "$chapters chapters",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    "›",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        PageTitle("Study Mode") {
            click()
            viewModel.backToSetup()
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { BookGroupHeader("Old Testament") }
            items(bible.oldTestamentBooks) { bookCard(it) }
            item { BookGroupHeader("New Testament") }
            items(bible.newTestamentBooks) { bookCard(it) }
        }
    }
}

/** A book's notepad: a bar for each chapter, which the player can name, with notes under it. */
@Composable
fun NotesScreen(viewModel: QuizViewModel, book: String) {
    val bible = viewModel.bible ?: return
    val click = LocalUiClick.current
    val chapters = bible.chapters[book]?.keys?.toList().orEmpty()
    // The notes the B and I buttons apply to: the last ones the player typed in.
    var formatting by remember { mutableStateOf<RichEditorState?>(null) }
    BackHandler { viewModel.openStudy() }

    Column(Modifier.fillMaxSize()) {
        PageTitle(book) {
            click()
            viewModel.openStudy()
        }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 20.dp)) {
            items(chapters, key = { it }) { chapter ->
                ChapterBar(
                    chapter = chapter,
                    name = viewModel.bookNotes.names[chapter].orEmpty(),
                    onName = { viewModel.nameChapter(book, chapter, it) },
                    onFocus = { formatting = null },
                )
                val notes = remember(book, chapter) {
                    RichEditorState(viewModel.bookNotes.notes[chapter] ?: RichText()) {
                        viewModel.writeNotes(book, chapter, it)
                    }
                }
                // Scrolled out of sight, the notes can't be formatted.
                DisposableEffect(notes) {
                    onDispose { if (formatting === notes) formatting = null }
                }
                RichTextField(notes, onFocus = { formatting = notes })
            }
        }
        formatting?.let { FormatBar(it) }
    }
}

/** A page's title, with Done to leave it. */
@Composable
private fun PageTitle(title: String, onDone: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
        TextButton(onClick = onDone) { Text("Done") }
    }
}

/** A straight, full-width bar: "Chapter 3", then a name the player can type. */
@Composable
private fun ChapterBar(chapter: Int, name: String, onName: (String) -> Unit, onFocus: () -> Unit) {
    val color = MaterialTheme.colorScheme.onPrimaryContainer
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 20.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "$chapter",
            modifier = Modifier.padding(end = 16.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = color,
        )
        BasicTextField(
            value = name,
            onValueChange = onName,
            modifier = Modifier
                .weight(1f)
                .onFocusChanged { if (it.isFocused) onFocus() }
                .semantics { contentDescription = "Chapter $chapter name" },
            singleLine = true,
            textStyle = MaterialTheme.typography.titleMedium.copy(color = color),
            cursorBrush = SolidColor(color),
            decorationBox = { field ->
                Box {
                    if (name.isEmpty()) {
                        Text(
                            "Name this chapter",
                            style = MaterialTheme.typography.titleMedium,
                            color = color.copy(alpha = 0.5f),
                        )
                    }
                    field()
                }
            },
        )
    }
}

/**
 * One chapter's notes while they're on screen. Keeps the bold and italic
 * flags in step with the text as the player types.
 */
private class RichEditorState(initial: RichText, private val onChange: (RichText) -> Unit) {
    var rich by mutableStateOf(initial)
        private set
    var field by mutableStateOf(TextFieldValue(initial.text))
        private set
    /** Bold or italic turned on or off with nothing selected, for the next thing typed. */
    private var pending by mutableStateOf<Int?>(null)

    private val typingStyle get() = pending ?: rich.styleAt(field.selection.min)

    fun onFieldChange(new: TextFieldValue) {
        if (new.text != field.text) {
            rich = rich.edited(new.text, pending)
            onChange(rich)
            pending = null
        } else if (new.selection != field.selection) {
            pending = null
        }
        field = new
    }

    fun isOn(flag: Int): Boolean {
        val selection = field.selection
        return if (selection.collapsed) {
            typingStyle and flag != 0
        } else {
            rich.allHave(selection.min, selection.max, flag)
        }
    }

    /** Style the selected text, or with nothing selected, whatever's typed next. */
    fun toggle(flag: Int) {
        val selection = field.selection
        if (selection.collapsed) {
            pending = typingStyle xor flag
        } else {
            rich = rich.toggled(selection.min, selection.max, flag)
            onChange(rich)
        }
    }

    fun styled(): AnnotatedString = buildAnnotatedString {
        append(rich.text)
        for (run in rich.runs()) {
            addStyle(
                SpanStyle(
                    fontWeight = if (run.style and BOLD != 0) FontWeight.Bold else null,
                    fontStyle = if (run.style and ITALIC != 0) FontStyle.Italic else null,
                ),
                run.start,
                run.end,
            )
        }
    }
}

@Composable
private fun RichTextField(state: RichEditorState, onFocus: () -> Unit) {
    val textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface)
    BasicTextField(
        value = state.field,
        onValueChange = state::onFieldChange,
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { if (it.isFocused) onFocus() }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        textStyle = textStyle,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        minLines = 2,
        // Draw the bold and italic stretches; the text itself is unchanged, so offsets match.
        visualTransformation = { text ->
            val styled = if (text.text == state.rich.text) state.styled() else text
            TransformedText(styled, OffsetMapping.Identity)
        },
        decorationBox = { field ->
            Box {
                if (state.field.text.isEmpty()) {
                    Text(
                        "Write notes…",
                        style = textStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                field()
            }
        },
    )
}

/** B and I buttons for the notes being written, shown above the keyboard. */
@Composable
private fun FormatBar(state: RichEditorState) {
    val click = LocalUiClick.current
    Surface(tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilledIconToggleButton(
                checked = state.isOn(BOLD),
                onCheckedChange = {
                    click()
                    state.toggle(BOLD)
                },
                modifier = Modifier.semantics { contentDescription = "Bold" },
            ) { Text("B", fontWeight = FontWeight.Bold) }
            FilledIconToggleButton(
                checked = state.isOn(ITALIC),
                onCheckedChange = {
                    click()
                    state.toggle(ITALIC)
                },
                modifier = Modifier.semantics { contentDescription = "Italic" },
            ) { Text("I", fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold) }
        }
    }
}
