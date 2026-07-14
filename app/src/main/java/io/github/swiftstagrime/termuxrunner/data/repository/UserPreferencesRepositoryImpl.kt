package io.github.swiftstagrime.termuxrunner.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.swiftstagrime.termuxrunner.domain.model.ScriptExecutionResult
import io.github.swiftstagrime.termuxrunner.domain.repository.UserPreferencesRepository
import io.github.swiftstagrime.termuxrunner.ui.theme.AppTheme
import io.github.swiftstagrime.termuxrunner.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import javax.inject.Inject

/**
 * Manages general app settings and UI preferences using Jetpack DataStore.
 */

class UserPreferencesRepositoryImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : UserPreferencesRepository {
        private object Keys {
            val THEME_ACCENT = stringPreferencesKey("theme_accent")
            val THEME_MODE = stringPreferencesKey("theme_mode")
            val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
            val ONBOARDING_SEEN = booleanPreferencesKey("onboarding_seen")
            val SELECTED_CUSTOM_THEME_ID = intPreferencesKey("selected_custom_theme_id")
            val PENDING_SCRIPT_RESULT = stringPreferencesKey("pending_script_result")
        }

        override val selectedAccent: Flow<AppTheme> =
            context.dataStore.data
                .map { prefs ->
                    val name = prefs[Keys.THEME_ACCENT] ?: AppTheme.GREEN.name
                    AppTheme.valueOf(name)
                }

        override val selectedMode: Flow<ThemeMode> =
            context.dataStore.data
                .map { prefs ->
                    val name = prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name
                    ThemeMode.valueOf(name)
                }

        override val selectedCustomThemeId: Flow<Int?> =
            context.dataStore.data.map { it[Keys.SELECTED_CUSTOM_THEME_ID] }

        override suspend fun setAccent(accent: AppTheme) {
            context.dataStore.edit { it[Keys.THEME_ACCENT] = accent.name }
        }

        override suspend fun setMode(mode: ThemeMode) {
            context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
        }

        override val hasCompletedOnboarding: Flow<Boolean> =
            context.dataStore.data
                .map { preferences -> preferences[Keys.ONBOARDING_COMPLETED] ?: false }

        override val hasSeenOnboarding: Flow<Boolean> =
            context.dataStore.data
                .map { preferences -> preferences[Keys.ONBOARDING_SEEN] ?: false }

        override suspend fun setOnboardingCompleted(completed: Boolean) {
            context.dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
        }

        override suspend fun setOnboardingSeen(seen: Boolean) {
            context.dataStore.edit { it[Keys.ONBOARDING_SEEN] = seen }
        }

        private fun getTileKey(index: Int) = intPreferencesKey("qs_tile_${index}_script_id")

        override fun getScriptIdForTile(tileIndex: Int): Flow<Int?> =
            context.dataStore.data
                .map { preferences -> preferences[getTileKey(tileIndex)] }

        override suspend fun setScriptIdForTile(
            tileIndex: Int,
            scriptId: Int?,
        ) {
            context.dataStore.edit { preferences ->
                val key = getTileKey(tileIndex)
                if (scriptId == null) {
                    preferences.remove(key)
                } else {
                    preferences[key] = scriptId
                }
            }
        }

        override suspend fun setCustomThemeId(id: Int) {
            context.dataStore.edit { it[Keys.SELECTED_CUSTOM_THEME_ID] = id }
        }

        // Último resultado de ejecución pendiente de mostrar en el popup.
        // Se persiste en disco (no solo en memoria) para que sobreviva aunque
        // la app esté cerrada o el proceso muera entre que Termux devuelve
        // el resultado y el usuario vuelve a abrir la app.
        override val pendingScriptResult: Flow<ScriptExecutionResult?> =
            context.dataStore.data
                .map { prefs -> prefs[Keys.PENDING_SCRIPT_RESULT]?.let { decodeScriptResult(it) } }

        override suspend fun setPendingScriptResult(result: ScriptExecutionResult?) {
            context.dataStore.edit { prefs ->
                if (result == null) {
                    prefs.remove(Keys.PENDING_SCRIPT_RESULT)
                } else {
                    prefs[Keys.PENDING_SCRIPT_RESULT] = encodeScriptResult(result)
                }
            }
        }

        private fun encodeScriptResult(result: ScriptExecutionResult): String =
            JSONObject().apply {
                put("scriptId", result.scriptId)
                put("scriptName", result.scriptName)
                put("exitCode", result.exitCode)
                put("stdout", result.stdout)
                put("stderr", result.stderr)
                put("internalError", result.internalError)
                put("automationId", result.automationId)
            }.toString()

        private fun decodeScriptResult(raw: String): ScriptExecutionResult? =
            try {
                val json = JSONObject(raw)
                ScriptExecutionResult(
                    scriptId = json.getInt("scriptId"),
                    scriptName = json.getString("scriptName"),
                    exitCode = json.getInt("exitCode"),
                    stdout = json.optString("stdout"),
                    stderr = json.optString("stderr"),
                    internalError = if (json.isNull("internalError")) null else json.optString("internalError"),
                    automationId = json.optInt("automationId", -1),
                )
            } catch (e: Exception) {
                null
            }
    }

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")