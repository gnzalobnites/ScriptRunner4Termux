package io.github.swiftstagrime.termuxrunner.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import io.github.swiftstagrime.termuxrunner.domain.usecase.ProcessTermuxResultUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TermuxResultReceiver : BroadcastReceiver() {
    @Inject
    lateinit var processUseCase: ProcessTermuxResultUseCase

    override fun onReceive(
        context: Context?,
        intent: Intent?,
    ) {
        if (context == null || intent == null) return
        val expectedAction = "${context.packageName}.SCRIPT_RESULT"
        if (intent.action != expectedAction) return

        val scriptName = intent.getStringExtra("script_name") ?: "Script"
        val scriptId = intent.getIntExtra("script_id", -1)
        val automationId = intent.getIntExtra("automation_id", -1)

        var exitCode = -1337
        var internalError: String? = null
        var stdout = ""
        var stderr = ""

        val bundle = intent.getBundleExtra("result")
        if (bundle != null) {
            // Claves publicadas por el RunCommandService de Termux dentro del
            // bundle "result" que viaja en el PendingIntent de retorno.
            exitCode = bundle.getInt("exitCode", -1337)
            internalError = bundle.getString("errmsg")
            stdout = bundle.getString("stdout").orEmpty()
            stderr = bundle.getString("stderr").orEmpty()
        } else if (intent.hasExtra("com.termux.RUN_COMMAND_RESULT_CODE")) {
            exitCode = intent.getIntExtra("com.termux.RUN_COMMAND_RESULT_CODE", -1337)
            internalError = intent.getStringExtra("com.termux.RUN_COMMAND_ERRMSG")
            stdout = intent.getStringExtra("com.termux.RUN_COMMAND_RESULT_STDOUT").orEmpty()
            stderr = intent.getStringExtra("com.termux.RUN_COMMAND_RESULT_STDERR").orEmpty()
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                processUseCase.execute(
                    automationId = automationId,
                    scriptId = scriptId,
                    scriptName = scriptName,
                    exitCode = exitCode,
                    internalError = internalError,
                    stdout = stdout,
                    stderr = stderr,
                )
            } finally {
                pendingResult?.finish()
            }
        }
    }
}
