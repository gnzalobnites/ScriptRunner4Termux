package io.github.swiftstagrime.termuxrunner.domain.repository

import io.github.swiftstagrime.termuxrunner.domain.model.ScriptExecutionResult
import io.github.swiftstagrime.termuxrunner.ui.theme.AppTheme
import io.github.swiftstagrime.termuxrunner.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    val hasCompletedOnboarding: Flow<Boolean>
    val hasSeenOnboarding: Flow<Boolean>
    val pendingScriptResult: Flow<ScriptExecutionResult?>
    val selectedAccent: Flow<AppTheme>
    val selectedMode: Flow<ThemeMode>
    val selectedCustomThemeId: Flow<Int?>

    suspend fun setMode(mode: ThemeMode)
    suspend fun setAccent(accent: AppTheme)
    suspend fun setOnboardingCompleted(completed: Boolean)
    suspend fun setOnboardingSeen(seen: Boolean)
    suspend fun setPendingScriptResult(result: ScriptExecutionResult?)

    fun getScriptIdForTile(tileIndex: Int): Flow<Int?>
    suspend fun setScriptIdForTile(tileIndex: Int, scriptId: Int?)
    suspend fun setCustomThemeId(id: Int)
}
