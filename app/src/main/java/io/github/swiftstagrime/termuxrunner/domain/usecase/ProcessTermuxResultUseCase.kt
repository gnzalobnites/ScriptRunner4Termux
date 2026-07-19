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

            notificationHelper.showResultNotification(
                scriptId = scriptId,
                name = scriptName,
                exitCode = exitCode,
                internalError = internalError,
                stdout = stdout,
                stderr = stderr,
            )

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
