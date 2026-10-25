package com.example.myapplication.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.myapplication.quiz.QuizViewModel
import com.example.myapplication.quiz.SoundMode
import com.example.myapplication.ui.theme.Palette
import com.example.myapplication.ui.theme.ThemeMode
import com.example.myapplication.ui.theme.colorSchemeFor

private const val SWATCHES_PER_ROW = 4

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: QuizViewModel) {
    BackHandler { viewModel.closeSettings() }
    val dark = viewModel.themeMode.isDark()
    val click = LocalUiClick.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Settings",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
            )
            TextButton(onClick = {
                click()
                viewModel.closeSettings()
            }) { Text("Done") }
        }

        SectionCard("Mode") {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                ThemeMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = viewModel.themeMode == mode,
                        onClick = {
                            click()
                            viewModel.chooseThemeMode(mode)
                        },
                        shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size),
                    ) { Text(mode.label) }
                }
            }
        }

        SectionCard("Color") {
            Palette.entries.chunked(SWATCHES_PER_ROW).forEach { row ->
                Row(Modifier.fillMaxWidth()) {
                    row.forEach { palette ->
                        Swatch(
                            palette = palette,
                            dark = dark,
                            vibrant = viewModel.vibrant,
                            selected = viewModel.palette == palette,
                            onClick = {
                                click()
                                viewModel.choosePalette(palette)
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    // Keep a short last row lined up with the rows above it.
                    repeat(SWATCHES_PER_ROW - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            val canBeVibrant = viewModel.palette != Palette.DEVICE
            SwitchRow(
                title = "Vibrant colors",
                description = if (canBeVibrant) {
                    "Bolder, more saturated versions of each color."
                } else {
                    "Device uses your phone's wallpaper colors. Pick a color to use vibrant colors."
                },
                checked = viewModel.vibrant,
                enabled = canBeVibrant,
                onCheckedChange = viewModel::chooseVibrant,
            )
        }

        SectionCard("Preview") {
            Text(
                "For God so loved the world that He gave His one and only Son…",
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = FontFamily.Serif,
            )
            Text(
                "John 3:16",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Button(onClick = click, modifier = Modifier.fillMaxWidth()) { Text("Next verse") }
        }

        SectionCard("Sound") {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SoundMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = viewModel.soundMode == mode,
                        onClick = {
                            // Choose first, so picking "All" clicks and picking "Off" doesn't.
                            viewModel.chooseSoundMode(mode)
                            click()
                        },
                        shape = SegmentedButtonDefaults.itemShape(index, SoundMode.entries.size),
                    ) { Text(mode.label) }
                }
            }
            Text(
                viewModel.soundMode.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    val click = LocalUiClick.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = {
                    click()
                    onCheckedChange(it)
                },
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

@Composable
private fun Swatch(
    palette: Palette,
    dark: Boolean,
    vibrant: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val colors = colorSchemeFor(palette, dark, vibrant)
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .then(
                    if (selected) {
                        Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                    } else {
                        Modifier
                    }
                )
                .padding(if (selected) 5.dp else 0.dp)
                .clip(CircleShape)
                .background(colors.primary),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Text("✓", color = colors.onPrimary)
        }
        Text(
            palette.label,
            modifier = Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
        )
    }
}
