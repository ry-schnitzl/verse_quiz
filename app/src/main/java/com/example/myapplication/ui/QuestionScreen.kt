package com.example.myapplication.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.quiz.QuizScreen
import com.example.myapplication.quiz.QuizViewModel
import com.example.myapplication.quiz.suggestBooks

@Composable
fun QuestionScreen(viewModel: QuizViewModel, screen: QuizScreen.Playing) {
    val game = screen.game
    val verse = game.verses[screen.round]
    val total = game.verses.size
    val isLast = screen.round == total - 1
    var confirming by remember { mutableStateOf<MenuAction?>(null) }
    val click = LocalUiClick.current

    BackHandler { confirming = MenuAction.QUIT }

    val firstField = remember { FocusRequester() }
    val chapterField = remember { FocusRequester() }
    val verseField = remember { FocusRequester() }
    // Put the cursor in the first box for each new verse.
    LaunchedEffect(screen.round) { firstField.requestFocus() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Verse ${screen.round + 1} of $total",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    "From ${game.scope.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            MenuButton(
                MenuAction.entries.map { action -> action.label to { confirming = action } } +
                    ("Settings" to viewModel::openSettings)
            )
        }
        LinearProgressIndicator(
            progress = { screen.round.toFloat() / total },
            modifier = Modifier.fillMaxWidth(),
        )

        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    verse.text,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = FontFamily.Serif,
                        lineHeight = 30.sp,
                    ),
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!game.singleBook) {
                LabeledField(
                    label = "Book",
                    value = viewModel.bookInput,
                    onValueChange = { viewModel.bookInput = it },
                    modifier = Modifier.weight(2f),
                    focusRequester = firstField,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next,
                    ),
                    keyboardActions = KeyboardActions(onNext = { chapterField.requestFocus() }),
                )
            }
            LabeledField(
                label = "Chapter",
                value = viewModel.chapterInput,
                onValueChange = { viewModel.chapterInput = it.filter(Char::isDigit) },
                modifier = Modifier.weight(1f),
                focusRequester = if (game.singleBook) firstField else chapterField,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(onNext = { verseField.requestFocus() }),
            )
            LabeledField(
                label = "Verse",
                value = viewModel.verseInput,
                onValueChange = { viewModel.verseInput = it.filter(Char::isDigit) },
                modifier = Modifier.weight(1f),
                focusRequester = verseField,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { viewModel.submitGuess() }),
            )
        }

        if (!game.singleBook) {
            val books = viewModel.bible?.books.orEmpty()
            val suggestions = remember(viewModel.bookInput, books) {
                suggestBooks(viewModel.bookInput, books)
            }
            if (suggestions.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(suggestions) { book ->
                        SuggestionChip(
                            onClick = {
                                click()
                                viewModel.bookInput = book
                                chapterField.requestFocus()
                            },
                            label = { Text(book) },
                        )
                    }
                }
            }
        }

        Button(
            onClick = viewModel::submitGuess,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
        ) {
            Text(
                if (isLast) "Finish and see results" else "Next verse",
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }

    confirming?.let { action ->
        AlertDialog(
            onDismissRequest = { confirming = null },
            title = { Text(action.question) },
            text = { Text("Your guesses so far won't be scored.") },
            confirmButton = {
                TextButton(onClick = {
                    click()
                    confirming = null
                    when (action) {
                        MenuAction.RESTART -> viewModel.start()
                        MenuAction.QUIT -> viewModel.backToSetup()
                    }
                }) { Text(action.label) }
            },
            dismissButton = {
                TextButton(onClick = {
                    click()
                    confirming = null
                }) { Text("Keep playing") }
            },
        )
    }
}

/** Game menu choices that end the current game, so each is confirmed first. */
private enum class MenuAction(val label: String, val question: String) {
    RESTART("Restart", "Restart with new verses?"),
    QUIT("Quit", "Quit this game?"),
}

/** A text box with its label above it rather than inside, so several fit on one line. */
@Composable
private fun LabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
    focusRequester: FocusRequester,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            singleLine = true,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
        )
    }
}
