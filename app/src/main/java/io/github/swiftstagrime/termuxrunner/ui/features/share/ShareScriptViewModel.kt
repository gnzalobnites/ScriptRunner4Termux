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
import io.github.swiftstagrime.termuxrunner.domain.repository.TermuxRepository
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
import java.io.File
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShareScriptViewModel
    @Inject
    constructor(
        private val scriptFileRepository: ScriptFileRepository,
        private val runScriptUseCase: RunScriptUseCase,
        private val termuxRepository: TermuxRepository,
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
        private var pendingRealPath: String? = null

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
                    pendingRealPath = sharedFile.realPath

                    val extension = sharedFile.fileName.substringAfterLast('.', "sh").lowercase()
                    val interpreter = INTERPRETER_MAP[extension] ?: "bash"

                    _pendingScript.value =
                        Script.fromCode(
                            code = sharedFile.content,
                            name = sharedFile.fileName,
                            id = SHARED_SCRIPT_ID,
                            interpreter = interpreter,
                            fileExtension = extension,
                            runInBackground = true,
                            notifyOnResult = true,
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
            executeScript(script, pendingRealPath)
        }

        fun cancel() {
            _pendingScript.value = null
            sendEvent(ShareScriptEvent.Finish)
        }

        private fun executeScript(
            script: Script,
            realPath: String?,
        ) {
            viewModelScope.launch(ioDispatcher) {
                try {
                    if (realPath != null) {
                        // Ejecuta el archivo directamente desde su ubicación real,
                        // sin copiarlo ni re-codificarlo.
                        termuxRepository.runCommand(
                            command = buildDirectPathCommand(script.interpreter, realPath),
                            runInBackground = script.runInBackground,
                            sessionAction = "1",
                            scriptId = SHARED_SCRIPT_ID,
                            scriptName = script.name,
                            notifyOnResult = true,
                        )
                    } else {
                        // No se pudo resolver una ruta real (ej. proveedor de nube,
                        // adjunto de chat): usamos el método anterior, que copia
                        // el contenido y lo ejecuta.
                        runScriptUseCase(script = script)
                    }
                    sendEvent(ShareScriptEvent.Finish)
                } catch (_: TermuxPermissionException) {
                    scriptAwaitingPermission = script
                    pendingRealPath = realPath
                    sendEvent(ShareScriptEvent.RequestPermission)
                } catch (e: Exception) {
                    sendEvent(ShareScriptEvent.ShowError("Error: ${e.message}"))
                    sendEvent(ShareScriptEvent.Finish)
                }
            }
        }

        private fun buildDirectPathCommand(
            interpreter: String,
            realPath: String,
        ): String {
            val escapedPath = shellEscape(realPath)
            val parentDir = File(realPath).parent

            return if (parentDir != null) {
                // Nos posicionamos en la carpeta del script para que las rutas
                // relativas dentro de él (ej. "./data.txt") se resuelvan igual
                // que si se ejecutara localmente desde esa carpeta.
                val escapedDir = shellEscape(parentDir)
                "cd '$escapedDir' && $interpreter '$escapedPath'"
            } else {
                "$interpreter '$escapedPath'"
            }
        }

        private fun shellEscape(value: String): String = value.replace("'", "'\\''")

        fun onPermissionGranted() {
            val script = scriptAwaitingPermission ?: return
            executeScript(script, pendingRealPath)
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

            // Mismo criterio extensión -> intérprete que usa
            // ScriptRepositoryImpl al importar un script nuevo.
            private val INTERPRETER_MAP =
                mapOf(
                    "sh" to "bash",
                    "bash" to "bash",
                    "py" to "python",
                    "py3" to "python",
                )
        }
    }

sealed interface ShareScriptEvent {
    data object Finish : ShareScriptEvent

    data object RequestPermission : ShareScriptEvent

    data class ShowError(
        val message: String,
    ) : ShareScriptEvent
}
