package io.github.swiftstagrime.termuxrunner.domain.repository
import androidx.hilt.navigation.compose.hiltViewModel

import io.github.swiftstagrime.termuxrunner.domain.model.ScriptExecutionResult
import io.github.swiftstagrime.termuxrunner.ui.theme.AppTheme
import io.github.swiftstagrime.termuxrunner.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    val hasCompletedOnboarding: Flow<Boolean>
    val pendingScriptResult: Flow<ScriptExecutionResult?>
    val selectedAccent: Flow<AppTheme>
    val selectedMode: Flow<ThemeMode>
    val selectedCustomThemeId: Flow<Int?>

    suspend fun setMode(mode: ThemeMode)

    suspend fun setAccent(accent: AppTheme)

    suspend fun setOnboardingCompleted(completed: Boolean)

    fun getScriptIdForTile(tileIndex: Int): Flow<Int?>

    suspend fun setScriptIdForTile(
        tileIndex: Int,
        scriptId: Int?,
    )

    suspend fun setCustomThemeId(id: Int)

    suspend fun setPendingScriptResult(result: ScriptExecutionResult?)
}
