package io.github.swiftstagrime.termuxrunner.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.swiftstagrime.termuxrunner.data.event.ScriptResultEventBus
import io.github.swiftstagrime.termuxrunner.domain.model.ScriptExecutionResult
import io.github.swiftstagrime.termuxrunner.domain.repository.CustomThemeRepository
import io.github.swiftstagrime.termuxrunner.domain.repository.UserPreferencesRepository
import io.github.swiftstagrime.termuxrunner.ui.navigation.Route
import io.github.swiftstagrime.termuxrunner.ui.theme.AppTheme
import io.github.swiftstagrime.termuxrunner.ui.theme.ThemeMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel
    @Inject
    constructor(
        private val userPreferencesRepository: UserPreferencesRepository,
        private val customThemeRepository: CustomThemeRepository,
        private val scriptResultEventBus: ScriptResultEventBus,
    ) : ViewModel() {
        private val _isReady = MutableStateFlow(false)
        val isReady = _isReady.asStateFlow()

        private val _startDestination = MutableStateFlow<String?>(null)
        val startDestination = _startDestination.asStateFlow()

        private val _backStack = mutableListOf<String>()
        val backStack = MutableStateFlow<List<String>>(emptyList())

        // Resultado de la última ejecución de script pendiente de mostrar
        // en la ventana emergente (null = no hay nada que mostrar).
        private val _scriptResult = MutableStateFlow<ScriptExecutionResult?>(null)
        val scriptResult = _scriptResult.asStateFlow()

        val selectedAccent =
            userPreferencesRepository.selectedAccent
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppTheme.GREEN)

        val selectedMode =
            userPreferencesRepository.selectedMode
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

        @OptIn(ExperimentalCoroutinesApi::class)
        val customTheme =
            userPreferencesRepository.selectedCustomThemeId
                .flatMapLatest { id ->
                    if (id != null) {
                        customThemeRepository.getThemeByIdFlow(id)
                    } else {
                        flowOf(null)
                    }
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

        init {
            viewModelScope.launch {
                userPreferencesRepository.hasCompletedOnboarding.take(1).collect { completed ->
                    val start = if (completed) Route.Home else Route.Onboarding
                    _startDestination.value = start.route
                    if (_backStack.isEmpty()) {
                        setRoot(start)
                    }
                    _isReady.value = true
                }
            }

            // Al abrir la app, primero revisamos si quedó un resultado
            // pendiente de mostrar (persistido en disco), por si el script
            // terminó mientras la app estaba cerrada o en background.
            viewModelScope.launch {
                userPreferencesRepository.pendingScriptResult.take(1).collect { pending ->
                    if (pending != null) {
                        _scriptResult.value = pending
                    }
                }
            }

            // Mientras la app siga abierta, seguimos escuchando resultados
            // en vivo por el bus en memoria.
            viewModelScope.launch {
                scriptResultEventBus.events.collect { result ->
                    _scriptResult.value = result
                }
            }
        }

        /**
         * Obtiene el resultado pendiente del repositorio.
         * Usado por la Activity cuando se abre desde una notificación.
         */
        suspend fun getPendingScriptResult(): ScriptExecutionResult? {
            return userPreferencesRepository.pendingScriptResult.first()
        }

        /**
         * Muestra un resultado de script en el popup.
         * Usado por la Activity cuando se abre desde una notificación.
         */
        fun showScriptResult(result: ScriptExecutionResult) {
            _scriptResult.value = result
        }

        fun dismissScriptResult() {
            _scriptResult.value = null
            viewModelScope.launch {
                userPreferencesRepository.setPendingScriptResult(null)
            }
        }

        fun navigateTo(route: Route) {
            val current = _backStack.toMutableList()
            current.add(route.route)
            updateStack(current)
        }

        fun goBack() {
            val current = _backStack.toMutableList()
            if (current.size > 1) {
                current.removeAt(current.lastIndex)
                updateStack(current)
            }
        }

        fun replaceRoot(route: Route) {
            updateStack(listOf(route.route))
        }

        private fun setRoot(route: Route) {
            updateStack(listOf(route.route))
        }

        private fun updateStack(newStack: List<String>) {
            _backStack.clear()
            _backStack.addAll(newStack)
            backStack.value = newStack.toList()
        }
    }