package com.example.myapplication.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.example.myapplication.quiz.QuizViewModel
import com.example.myapplication.quiz.Scope
import com.example.myapplication.quiz.VerseSet

/** The player's verse sets: pick one to play it, tap its name to rename it, › to change its verses. */
@Composable
fun VerseSetsScreen(viewModel: QuizViewModel) {
    BackHandler { viewModel.backToSetup() }
    var renamingId by rememberSaveable { mutableStateOf<Long?>(null) }
    val chosenId = (viewModel.scope as? Scope.Custom)?.set?.id
    val click = LocalUiClick.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Verse sets",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
            )
            TextButton(onClick = {
                click()
                viewModel.backToSetup()
            }) { Text("Done") }
        }
        Text(
            "Pick a set to play it. Tap its name to rename it.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        LazyColumn(Modifier.weight(1f)) {
            items(viewModel.verseSets, key = { it.id }) { set ->
                VerseSetRow(
                    set = set,
                    chosen = set.id == chosenId,
                    renaming = set.id == renamingId,
                    onChoose = {
                        click()
                        viewModel.chooseScope(Scope.Custom(set))
                    },
                    onStartRename = {
                        click()
                        renamingId = set.id
                    },
                    onRename = { name ->
                        viewModel.renameVerseSet(set, name)
                        renamingId = null
                    },
                    onEdit = {
                        click()
                        viewModel.editVerseSet(set)
                    },
                )
                HorizontalDivider()
            }
            item {
                Text(
                    "+ Add new verse set",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            click()
                            viewModel.newVerseSet()
                        }
                        .padding(vertical = 16.dp, horizontal = 12.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun VerseSetRow(
    set: VerseSet,
    chosen: Boolean,
    renaming: Boolean,
    onChoose: () -> Unit,
    onStartRename: () -> Unit,
    onRename: (String) -> Unit,
    onEdit: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = chosen, onClick = onChoose)
        Spacer(Modifier.size(4.dp))
        if (renaming) {
            RenameField(set.name, onRename, Modifier.weight(1f))
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onStartRename)
                    .padding(vertical = 8.dp),
            ) {
                Text(set.name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    versesLabel(set.verses.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        ArrowButton("Edit ${set.name}", onEdit)
    }
}

/** Edits a set's name in place. It's kept when the player presses Done or taps away. */
@Composable
private fun RenameField(name: String, onDone: (String) -> Unit, modifier: Modifier) {
    // Start with the whole name selected, so typing replaces it.
    var value by remember { mutableStateOf(TextFieldValue(name, TextRange(0, name.length))) }
    var focused by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }
    fun finish() {
        if (!finished) onDone(value.text)
        finished = true
    }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    OutlinedTextField(
        value = value,
        onValueChange = { value = it },
        modifier = modifier
            .focusRequester(focusRequester)
            .onFocusChanged {
                if (focused && !it.isFocused) finish()
                focused = it.isFocused
            },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { finish() }),
    )
}

/** A › that opens the page for the thing to its left. */
@Composable
fun ArrowButton(description: String, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.semantics { contentDescription = description },
    ) {
        Text(
            "›",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
