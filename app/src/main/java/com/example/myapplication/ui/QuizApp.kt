package com.example.myapplication.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.quiz.QuizScreen
import com.example.myapplication.quiz.QuizViewModel

@Composable
fun QuizApp(viewModel: QuizViewModel = viewModel()) {
    CompositionLocalProvider(LocalUiClick provides viewModel::playUiClick) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
                    .imePadding()
            ) {
                if (viewModel.showingSettings) {
                    SettingsScreen(viewModel)
                } else {
                    when (val screen = viewModel.screen) {
                        QuizScreen.Loading -> CenteredMessage("Loading verses…", showSpinner = true)
                        is QuizScreen.LoadFailed -> CenteredMessage(screen.message)
                        QuizScreen.Setup -> SetupScreen(viewModel)
                        QuizScreen.VerseSets -> VerseSetsScreen(viewModel)
                        is QuizScreen.EditingSet -> VerseSetEditor(viewModel, screen)
                        is QuizScreen.Playing -> QuestionScreen(viewModel, screen)
                        is QuizScreen.Finished -> ResultsScreen(
                            screen = screen,
                            onPlayAgain = viewModel::start,
                            onChangeSettings = viewModel::backToSetup,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CenteredMessage(message: String, showSpinner: Boolean = false) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (showSpinner) {
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
        }
        Text(message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
    }
}
