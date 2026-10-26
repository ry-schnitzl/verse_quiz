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

enum class SoundMode(val label: String, val description: String) {
    ALL("All", "Clicks for every button, plus the game sounds."),
    GAME("Game only", "A click for each answer and a fanfare for your results."),
    OFF("Off", "No sounds."),
}

class Game(val scope: Scope, val verses: List<Verse>) {
    val rules = ScoringRules.forScope(scope)
    val singleBook get() = scope.onlyBook != null
}

sealed interface QuizScreen {
    data object Loading : QuizScreen
    data class LoadFailed(val message: String) : QuizScreen
    data object Setup : QuizScreen
    data object VerseSets : QuizScreen
    /** Picking verses for a new set, or changing [original]. */
    data class EditingSet(val original: VerseSet?) : QuizScreen
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
    var vibrant by mutableStateOf(prefs.getBoolean("vibrant", false))
        private set
    // Earlier versions saved an on/off switch; keep "off" if the player chose it.
    var soundMode by mutableStateOf(
        savedEnum("soundMode", if (prefs.getBoolean("soundOn", true)) SoundMode.ALL else SoundMode.OFF)
    )
        private set

    private val sounds = SoundEffects(application)

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

    /** The player's named verse sets, saved on the device. */
    var verseSets by mutableStateOf(decodeVerseSets(prefs.getString("verseSets", null) ?: "[]"))
        private set

    // The set the Custom option plays, remembered while another option is chosen.
    private var customSetId by mutableStateOf(
        prefs.getLong("customSetId", 0L).takeIf { it != 0L }
    )
    val customSet get() = verseSets.firstOrNull { it.id == customSetId }

    // The verse set being edited.
    var draftVerses by mutableStateOf<Set<VerseRef>>(emptySet())
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
        if (newScope is Scope.Custom) {
            customSetId = newScope.set.id
            prefs.edit().putLong("customSetId", newScope.set.id).apply()
        }
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
        if (soundMode != SoundMode.OFF) sounds.playClick()

        val next = playing.round + 1
        if (next < game.verses.size) {
            screen = QuizScreen.Playing(game, next)
        } else {
            screen = QuizScreen.Finished(game, results.toList())
            if (soundMode != SoundMode.OFF) sounds.playFanfare()
        }
    }

    fun openVerseSets() {
        screen = QuizScreen.VerseSets
    }

    fun newVerseSet() = editVerseSet(null)

    fun editVerseSet(set: VerseSet?) {
        draftVerses = set?.verses.orEmpty()
        screen = QuizScreen.EditingSet(set)
    }

    /** Add [refs] to the set being edited, or take them out. */
    fun selectDraftVerses(refs: Collection<VerseRef>, selected: Boolean) {
        draftVerses = if (selected) draftVerses + refs else draftVerses - refs.toSet()
    }

    fun clearDraftVerses() {
        draftVerses = emptySet()
    }

    /**
     * Save the set being edited and go back to the list. A new set gets a
     * placeholder name, which the player can change from the list.
     */
    fun saveDraft() {
        val editing = screen as? QuizScreen.EditingSet ?: return
        if (draftVerses.isEmpty()) return
        val original = editing.original
        if (original == null) {
            val name = generateSequence(verseSets.size + 1) { it + 1 }
                .map { "Verse set $it" }
                .first { name -> verseSets.none { it.name == name } }
            val saved = VerseSet(System.currentTimeMillis(), name, draftVerses)
            storeVerseSets(verseSets + saved)
            chooseScope(Scope.Custom(saved))
        } else {
            replaceVerseSet(original.copy(verses = draftVerses))
        }
        screen = QuizScreen.VerseSets
    }

    fun renameVerseSet(set: VerseSet, name: String) {
        if (name.isNotBlank()) replaceVerseSet(set.copy(name = name.trim()))
    }

    fun deleteVerseSet(set: VerseSet) {
        storeVerseSets(verseSets.filter { it.id != set.id })
        if (customSetId == set.id) {
            customSetId = null
            prefs.edit().remove("customSetId").apply()
        }
        if ((scope as? Scope.Custom)?.set?.id == set.id) chooseScope(Scope.WholeBible)
        screen = QuizScreen.VerseSets
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

    fun chooseVibrant(on: Boolean) {
        vibrant = on
        prefs.edit().putBoolean("vibrant", on).apply()
    }

    fun chooseSoundMode(mode: SoundMode) {
        soundMode = mode
        prefs.edit().putString("soundMode", mode.name).apply()
    }

    /** The click for an ordinary button tap, heard only when all sounds are on. */
    fun playUiClick() {
        if (soundMode == SoundMode.ALL) sounds.playClick()
    }

    override fun onCleared() {
        sounds.release()
    }

    private inline fun <reified T : Enum<T>> savedEnum(key: String, default: T): T =
        prefs.getString(key, null)
            ?.let { name -> enumValues<T>().firstOrNull { it.name == name } }
            ?: default

    /** Swap in a changed copy of a set, keeping its place, and play the new copy if it's chosen. */
    private fun replaceVerseSet(changed: VerseSet) {
        storeVerseSets(verseSets.map { if (it.id == changed.id) changed else it })
        if ((scope as? Scope.Custom)?.set?.id == changed.id) chooseScope(Scope.Custom(changed))
    }

    private fun storeVerseSets(sets: List<VerseSet>) {
        verseSets = sets
        prefs.edit().putString("verseSets", encodeVerseSets(sets)).apply()
    }

    private fun clearInputs() {
        bookInput = ""
        chapterInput = ""
        verseInput = ""
    }
}
