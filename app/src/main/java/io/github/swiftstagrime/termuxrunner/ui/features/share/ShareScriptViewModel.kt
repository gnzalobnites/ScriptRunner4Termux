package io.github.swiftstagrime.termuxrunner.ui.features.share

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.swiftstagrime.termuxrunner.data.repository.TermuxPermissionException
import io.github.swiftstagrime.termuxrunner.di.IoDispatcher
import io.github.swiftstagrime.termuxrunner.domain.model.Script
import io.github.swiftstagrime.termuxrunner.domain.repository.CustomThemeRepository
import io.github.swiftstagrime.termuxrunner.domain.repository.ScriptFileRepository
import io.github.swiftstagrime.termuxrunner.domain.repository.UserPreferencesRepository
import io.github.swiftstagrime.termuxrunner.domain.usecase.RunScriptUseCase
import io.github.swiftstagrime.termuxrunner.ui.theme.AppTheme
import io.github.swiftstagrime.termuxrunner.ui.theme.ThemeMode
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShareScriptViewModel
    @Inject
    constructor(
        private val scriptFileRepository: ScriptFileRepository,
        private val runScriptUseCase: RunScriptUseCase,
        private val userPreferencesRepository: UserPreferencesRepository,
        private val customThemeRepository: CustomThemeRepository,
        savedStateHandle: SavedStateHandle,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : ViewModel() {
        private val sharedUri: Uri? = savedStateHandle.get<Uri>(Intent.EXTRA_STREAM)

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

        private val _pendingScript = MutableStateFlow<Script?>(null)
        val pendingScript = _pendingScript.asStateFlow()

        private val _events = Channel<ShareScriptEvent>()
        val events = _events.receiveAsFlow()

        private var scriptAwaitingPermission: Script? = null

        init {
            val uri = sharedUri
            if (uri == null) {
                sendEvent(ShareScriptEvent.Finish)
            } else {
                loadSharedScript(uri)
            }
        }

        private fun loadSharedScript(uri: Uri) {
            viewModelScope.launch(ioDispatcher) {
                try {
                    val sharedFile = scriptFileRepository.readSharedFile(uri)
                    _pendingScript.value =
                        Script.fromCode(
                            code = sharedFile.content,
                            name = sharedFile.fileName,
                            id = SHARED_SCRIPT_ID,
                        )
                } catch (e: Exception) {
                    sendEvent(ShareScriptEvent.ShowError("Error: ${e.message}"))
                    sendEvent(ShareScriptEvent.Finish)
                }
            }
        }

        fun confirmExecution() {
            val script = _pendingScript.value ?: return
            _pendingScript.value = null
            executeScript(script)
        }

        fun cancel() {
            _pendingScript.value = null
            sendEvent(ShareScriptEvent.Finish)
        }

        private fun executeScript(script: Script) {
            viewModelScope.launch(ioDispatcher) {
                try {
                    runScriptUseCase(script = script)
                    sendEvent(ShareScriptEvent.Finish)
                } catch (_: TermuxPermissionException) {
                    scriptAwaitingPermission = script
                    sendEvent(ShareScriptEvent.RequestPermission)
                } catch (e: Exception) {
                    sendEvent(ShareScriptEvent.ShowError("Error: ${e.message}"))
                    sendEvent(ShareScriptEvent.Finish)
                }
            }
        }

        fun onPermissionGranted() {
            val script = scriptAwaitingPermission ?: return
            executeScript(script)
        }

        fun onPermissionDenied() {
            sendEvent(ShareScriptEvent.Finish)
        }

        private fun sendEvent(event: ShareScriptEvent) {
            viewModelScope.launch { _events.send(event) }
        }

        companion object {
            // ID reservado para scripts efímeros que llegan por "compartir",
            // no existen en la base de datos.
            const val SHARED_SCRIPT_ID = -2
        }
    }

sealed interface ShareScriptEvent {
    data object Finish : ShareScriptEvent

    data object RequestPermission : ShareScriptEvent

    data class ShowError(
        val message: String,
    ) : ShareScriptEvent
}
