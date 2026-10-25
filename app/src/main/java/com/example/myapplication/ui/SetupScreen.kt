package com.example.myapplication.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.myapplication.quiz.Bible
import com.example.myapplication.quiz.MAX_ROUNDS
import com.example.myapplication.quiz.QuizViewModel
import com.example.myapplication.quiz.Scope
import com.example.myapplication.quiz.ScoringRules

@Composable
fun SetupScreen(viewModel: QuizViewModel) {
    val bible = viewModel.bible ?: return
    var pickingBook by remember { mutableStateOf(false) }
    val scope = viewModel.scope

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Verse Quiz",
                    style = MaterialTheme.typography.displaySmall,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "Read a verse. Name where it's from.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            SectionCard("Verses from") {
                Column(Modifier.selectableGroup()) {
                    ScopeOption("Whole Bible", scope == Scope.WholeBible) {
                        viewModel.chooseScope(Scope.WholeBible)
                    }
                    ScopeOption("Old Testament", scope == Scope.OldTestament) {
                        viewModel.chooseScope(Scope.OldTestament)
                    }
                    ScopeOption("New Testament", scope == Scope.NewTestament) {
                        viewModel.chooseScope(Scope.NewTestament)
                    }
                    ScopeOption(
                        label = if (scope is Scope.SingleBook) "One book: ${scope.book}" else "One book…",
                        selected = scope is Scope.SingleBook,
                    ) { pickingBook = true }
                }
            }

            SectionCard("Number of verses") {
                val max = viewModel.maxRounds()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    FilledTonalButton(
                        onClick = { viewModel.changeRounds(viewModel.rounds - 1) },
                        enabled = viewModel.rounds > 1,
                    ) { Text("−", style = MaterialTheme.typography.titleLarge) }
                    Text(
                        viewModel.rounds.toString(),
                        modifier = Modifier.width(80.dp),
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                    )
                    FilledTonalButton(
                        onClick = { viewModel.changeRounds(viewModel.rounds + 1) },
                        enabled = viewModel.rounds < max,
                    ) { Text("+", style = MaterialTheme.typography.titleLarge) }
                }
                if (scope is Scope.SingleBook && max < MAX_ROUNDS) {
                    Text(
                        "${scope.book} has $max verses.",
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            SectionCard("Scoring") {
                ScoringSummary(ScoringRules.forScope(scope), singleBook = scope is Scope.SingleBook)
            }

            Button(
                onClick = viewModel::start,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
            ) { Text("Start", style = MaterialTheme.typography.titleMedium) }
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 12.dp, end = 8.dp),
        ) { MenuButton(listOf("Settings" to viewModel::openSettings)) }
    }

    if (pickingBook) {
        BookPickerDialog(
            bible = bible,
            selected = (scope as? Scope.SingleBook)?.book,
            onPick = {
                viewModel.chooseScope(Scope.SingleBook(it))
                pickingBook = false
            },
            onDismiss = { pickingBook = false },
        )
    }
}

@Composable
fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun ScopeOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.size(12.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ScoringSummary(rules: ScoringRules, singleBook: Boolean) {
    val lines = buildList {
        if (singleBook) {
            add("The book isn't asked. Guess the chapter and verse.")
        } else {
            add("Book: ${rules.bookPoints} pt. Get the book right to earn combo points:")
        }
        add("• Chapter: up to ${rules.chapterPoints} pts, −${rules.chapterPenalty} per number off")
        add("• Verse: up to ${rules.versePoints} pts, −${rules.versePenalty} per number off, only if the chapter is exact")
        add("Up to ${rules.maxPerVerse} points per verse. Answers are revealed at the end.")
    }
    lines.forEach {
        Text(
            it,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BookPickerDialog(
    bible: Bible,
    selected: String?,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose a book") },
        text = {
            LazyColumn(Modifier.heightIn(max = 420.dp)) {
                item { BookGroupHeader("Old Testament") }
                items(bible.oldTestamentBooks) { BookRow(it, it == selected, onPick) }
                item { BookGroupHeader("New Testament") }
                items(bible.newTestamentBooks) { BookRow(it, it == selected, onPick) }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun BookGroupHeader(title: String) {
    Text(
        title,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun BookRow(book: String, selected: Boolean, onPick: (String) -> Unit) {
    Text(
        book,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPick(book) }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
    )
}

