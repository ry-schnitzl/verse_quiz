package com.example.myapplication.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.quiz.QuizScreen
import com.example.myapplication.quiz.ScoringRules
import com.example.myapplication.quiz.VerseResult

@Composable
fun ResultsScreen(
    screen: QuizScreen.Finished,
    onPlayAgain: () -> Unit,
    onChangeSettings: () -> Unit,
) {
    val game = screen.game
    val results = screen.results
    val click = LocalUiClick.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Your score", style = MaterialTheme.typography.titleMedium)
                    Text(
                        results.sumOf { it.total }.toString(),
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text("out of ${game.maxScore}", style = MaterialTheme.typography.bodyMedium)
                    if (!game.singleBook) {
                        Text(
                            "Books correct: ${results.count { it.bookRight }} / ${results.size}",
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                    Text("Combo points: ${results.sumOf { it.comboScore }}")
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        click()
                        onPlayAgain()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                ) { Text("Play again") }
                OutlinedButton(
                    onClick = {
                        click()
                        onChangeSettings()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                ) { Text("Back to setup") }
            }
        }

        itemsIndexed(results) { index, result ->
            ResultCard(index + 1, result, game.rules, showBook = !game.singleBook)
        }
    }
}

@Composable
private fun ResultCard(number: Int, result: VerseResult, rules: ScoringRules, showBook: Boolean) {
    val guess = result.guess
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "$number. ${result.verse.reference}",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    "+${result.total}",
                    style = MaterialTheme.typography.titleMedium,
                    color = markColor(result.total, rules.maxPerVerse),
                )
            }
            Text(
                result.verse.text,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Serif,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider(Modifier.padding(vertical = 4.dp))

            if (showBook) {
                ScoreLine("Book", guess.book, result.bookScore, rules.bookPoints)
            }
            if (result.bookRight) {
                ScoreLine("Chapter", guess.chapter, result.chapterScore, rules.chapterPoints)
                if (result.chapterScore == rules.chapterPoints) {
                    ScoreLine("Verse", guess.verse, result.verseScore, rules.versePoints)
                } else {
                    PlainLine("Verse", guess.verse)
                }
                Text(
                    "Combo points: ${result.comboScore}",
                    style = MaterialTheme.typography.labelLarge,
                )
            } else {
                PlainLine("Chapter", guess.chapter)
                PlainLine("Verse", guess.verse)
            }
        }
    }
}

/** ✓ for full points, ~ for partial, ✗ for none. */
@Composable
private fun ScoreLine(label: String, guess: String, points: Int, max: Int) {
    val mark = when {
        points == max -> "✓"
        points > 0 -> "~"
        else -> "✗"
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            mark,
            modifier = Modifier.width(24.dp),
            color = markColor(points, max),
            fontWeight = FontWeight.Bold,
        )
        Text(label, modifier = Modifier.width(72.dp), style = MaterialTheme.typography.bodyMedium)
        Text(guess.ifBlank { "?" }, modifier = Modifier.weight(1f))
        Text("+$points", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun PlainLine(label: String, guess: String) {
    Row {
        Text("", modifier = Modifier.width(24.dp))
        Text(label, modifier = Modifier.width(72.dp), style = MaterialTheme.typography.bodyMedium)
        Text(guess.ifBlank { "?" }, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Green, amber and red whatever the palette, so the marks keep their meaning. */
@Composable
private fun markColor(points: Int, max: Int): Color {
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    return when {
        points == max -> if (dark) Color(0xFF8BD39A) else Color(0xFF2E7D32)
        points > 0 -> if (dark) Color(0xFFF2C66D) else Color(0xFF8A5F00)
        else -> MaterialTheme.colorScheme.error
    }
}
