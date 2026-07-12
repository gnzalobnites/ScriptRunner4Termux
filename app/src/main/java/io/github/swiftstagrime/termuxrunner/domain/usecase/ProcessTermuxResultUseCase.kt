package io.github.swiftstagrime.termuxrunner.domain.usecase

import io.github.swiftstagrime.termuxrunner.data.automation.AutomationNotificationHelper
import io.github.swiftstagrime.termuxrunner.data.event.ScriptResultEventBus
import io.github.swiftstagrime.termuxrunner.data.local.dao.AutomationDao
import io.github.swiftstagrime.termuxrunner.domain.model.AutomationLog
import io.github.swiftstagrime.termuxrunner.domain.model.ScriptExecutionResult
import io.github.swiftstagrime.termuxrunner.domain.repository.AutomationLogRepository
import io.github.swiftstagrime.termuxrunner.domain.repository.UserPreferencesRepository
import io.github.swiftstagrime.termuxrunner.ui.utils.WidgetManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProcessTermuxResultUseCase
    @Inject
    constructor(
        private val automationDao: AutomationDao,
        private val logRepository: AutomationLogRepository,
        private val notificationHelper: AutomationNotificationHelper,
        private val widgetManager: WidgetManager,
        private val resultEventBus: ScriptResultEventBus,
        private val userPreferencesRepository: UserPreferencesRepository,
    ) {
        suspend fun execute(
            automationId: Int,
            scriptId: Int,
            scriptName: String,
            exitCode: Int,
            internalError: String?,
            stdout: String = "",
            stderr: String = "",
        ) {
            val timestamp = System.currentTimeMillis()

            if (automationId != -1) {
                automationDao.updateLastResult(automationId, exitCode, timestamp)

                logRepository.insertLog(
                    AutomationLog(
                        automationId = automationId,
                        timestamp = timestamp,
                        exitCode = exitCode,
                        message = internalError ?: stderr.takeIf { it.isNotBlank() }?.take(500),
                    ),
                )

                widgetManager.updateLogsWidget()
            }

            // Notificación de sistema: se conserva siempre, cubre el caso en que
            // la app esté cerrada o en segundo plano (p. ej. automatizaciones).
            notificationHelper.showResultNotification(
                scriptId = scriptId,
                name = scriptName,
                exitCode = exitCode,
                internalError = internalError,
            )

            // El resultado completo (stdout/stderr) se persiste primero en
            // DataStore, para que sobreviva aunque la app esté cerrada o el
            // proceso muera antes de que el usuario la vuelva a abrir. Luego
            // se emite también por el bus en memoria para el caso en que la
            // app ya esté abierta y quiera reaccionar al instante, igual que
            // hace MiX Explorer al ejecutar scripts desde su menú.
            val fullResult =
                ScriptExecutionResult(
                    scriptId = scriptId,
                    scriptName = scriptName,
                    exitCode = exitCode,
                    stdout = stdout,
                    stderr = stderr,
                    internalError = internalError,
                    automationId = automationId,
                )
            userPreferencesRepository.setPendingScriptResult(fullResult)
            resultEventBus.emit(fullResult)
        }
    }
