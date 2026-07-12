package io.github.swiftstagrime.termuxrunner.ui.features.customtheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember

@Composable
fun CustomThemeRoute(
    onBack: () -> Unit,
    viewModel: CustomThemeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val actions =
        remember(viewModel, onBack) {
            CustomThemeActions(
                onBack = onBack,
                onNewTheme = viewModel::createNewTheme,
                onThemeSelect = viewModel::selectTheme,
                onNameChange = viewModel::updateName,
                onColorChange = viewModel::updateColorField,
                onSave = {
                    viewModel.saveTheme()
                },
                onDelete = {
                    viewModel.deleteTheme()
                },
                onToggleDarkMode = viewModel::toggleDarkMode,
            )
        }
    CustomThemeScreen(
        state = state,
        actions = actions,
    )
}
