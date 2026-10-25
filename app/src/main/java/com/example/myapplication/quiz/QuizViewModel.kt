package com.example.myapplication.quiz

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.ui.theme.Palette
import com.example.myapplication.ui.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

const val DEFAULT_ROUNDS = 10
const val MAX_ROUNDS = 50

class Game(val scope: Scope, val verses: List<Verse>) {
    val rules = ScoringRules.forScope(scope)
    val singleBook get() = scope is Scope.SingleBook
    val maxScore get() = verses.size * rules.maxPerVerse
}

sealed interface QuizScreen {
    data object Loading : QuizScreen
    data class LoadFailed(val message: String) : QuizScreen
    data object Setup : QuizScreen
    data class Playing(val game: Game, val round: Int) : QuizScreen
    data class Finished(val game: Game, val results: List<VerseResult>) : QuizScreen
}

class QuizViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("settings", Context.MODE_PRIVATE)

    // Appearance settings, saved on the device.
    var palette by mutableStateOf(savedEnum("palette", Palette.DEVICE))
        private set
    var themeMode by mutableStateOf(savedEnum("themeMode", ThemeMode.SYSTEM))
        private set

    /** The settings page is shown over whatever screen the player was on. */
    var showingSettings by mutableStateOf(false)
        private set

    var bible by mutableStateOf<Bible?>(null)
        private set
    var screen by mutableStateOf<QuizScreen>(QuizScreen.Loading)
        private set

    // Setup choices, kept between games.
    var scope by mutableStateOf<Scope>(Scope.WholeBible)
        private set
    var rounds by mutableIntStateOf(DEFAULT_ROUNDS)
        private set

    // The current guess.
    var bookInput by mutableStateOf("")
    var chapterInput by mutableStateOf("")
    var verseInput by mutableStateOf("")

    private val results = mutableListOf<VerseResult>()

    init {
        viewModelScope.launch {
            screen = try {
                val loaded = withContext(Dispatchers.IO) {
                    getApplication<Application>().assets.open("BSB.tsv")
                        .bufferedReader()
                        .useLines { Bible.parse(it) }
                }
                bible = loaded
                QuizScreen.Setup
            } catch (e: IOException) {
                QuizScreen.LoadFailed("Can't read the verse file: ${e.message}")
            }
        }
    }

    /** Verses available for [scope], which caps how many rounds can be played. */
    fun availableVerses(scope: Scope = this.scope) = bible?.versesIn(scope)?.size ?: 0

    fun maxRounds() = minOf(MAX_ROUNDS, availableVerses())

    fun chooseScope(newScope: Scope) {
        scope = newScope
        rounds = rounds.coerceIn(1, maxOf(1, maxRounds()))
    }

    fun changeRounds(newRounds: Int) {
        rounds = newRounds.coerceIn(1, maxOf(1, maxRounds()))
    }

    fun start() {
        val bible = bible ?: return
        val picks = bible.versesIn(scope).shuffled().take(rounds)
        if (picks.isEmpty()) return
        results.clear()
        clearInputs()
        screen = QuizScreen.Playing(Game(scope, picks), 0)
    }

    fun submitGuess() {
        val playing = screen as? QuizScreen.Playing ?: return
        val game = playing.game
        val verse = game.verses[playing.round]
        // Any book typed in single-book mode is ignored.
        val book = if (game.singleBook) verse.book else bookInput
        results += scoreGuess(verse, Guess(book, chapterInput, verseInput), game.rules)
        clearInputs()

        val next = playing.round + 1
        screen = if (next < game.verses.size) {
            QuizScreen.Playing(game, next)
        } else {
            QuizScreen.Finished(game, results.toList())
        }
    }

    fun backToSetup() {
        results.clear()
        clearInputs()
        screen = QuizScreen.Setup
    }

    fun openSettings() {
        showingSettings = true
    }

    fun closeSettings() {
        showingSettings = false
    }

    fun choosePalette(newPalette: Palette) {
        palette = newPalette
        prefs.edit().putString("palette", newPalette.name).apply()
    }

    fun chooseThemeMode(mode: ThemeMode) {
        themeMode = mode
        prefs.edit().putString("themeMode", mode.name).apply()
    }

    private inline fun <reified T : Enum<T>> savedEnum(key: String, default: T): T =
        prefs.getString(key, null)
            ?.let { name -> enumValues<T>().firstOrNull { it.name == name } }
            ?: default

    private fun clearInputs() {
        bookInput = ""
        chapterInput = ""
        verseInput = ""
    }
}
