package io.github.swiftstagrime.termuxrunner.ui
import androidx.hilt.navigation.compose.hiltViewModel
// // import androidx.navigation.String

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
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
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel
    @Inject
    constructor(
        private val userPreferencesRepository: UserPreferencesRepository,
        private val customThemeRepository: CustomThemeRepository,
    ) : ViewModel() {
        private val _isReady = MutableStateFlow(false)
        val isReady = _isReady.asStateFlow()

        private val _backStack = mutableListOf<String>()
        val backStack = MutableStateFlow<List<String>>(emptyList())

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
                    if (_backStack.isEmpty()) {
                        val start = if (completed) Route.Home else Route.Onboarding
                        setRoot(start)
                    }
                    _isReady.value = true
                }
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
