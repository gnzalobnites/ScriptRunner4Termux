package io.github.swiftstagrime.termuxrunner.ui.features.share
import androidx.hilt.navigation.compose.hiltViewModel

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import io.github.swiftstagrime.termuxrunner.R
import io.github.swiftstagrime.termuxrunner.ui.components.ShareScriptConfirmDialog
import io.github.swiftstagrime.termuxrunner.ui.theme.ScriptRunnerForTermuxTheme
import kotlinx.coroutines.launch

/**
 * Activity "invisible" que recibe un archivo .sh compartido desde otra
 * app (Intent.ACTION_SEND), pide confirmación y lo ejecuta en Termux.
 * El resultado se muestra luego en el ScriptResultDialog ya existente,
 * vía ScriptResultEventBus / pendingScriptResult.
 */
@AndroidEntryPoint
class ShareScriptActivity : ComponentActivity() {
    private val viewModel: ShareScriptViewModel by viewModels()

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { isGranted: Boolean ->
            if (isGranted) {
                viewModel.onPermissionGranted()
            } else {
                Toast
                    .makeText(
                        this,
                        getString(R.string.script_runner_permission_denied),
                        Toast.LENGTH_SHORT,
                    ).show()
                viewModel.onPermissionDenied()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val accent by viewModel.selectedAccent.collectAsStateWithLifecycle()
            val mode by viewModel.selectedMode.collectAsStateWithLifecycle()
            val customTheme by viewModel.customTheme.collectAsStateWithLifecycle()
            val pendingScript by viewModel.pendingScript.collectAsStateWithLifecycle()

            ScriptRunnerForTermuxTheme(accent = accent, mode = mode, customTheme = customTheme) {
                pendingScript?.let { script ->
                    ShareScriptConfirmDialog(
                        script = script,
                        onConfirm = { viewModel.confirmExecution() },
                        onDismiss = { viewModel.cancel() },
                    )
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { handleEvent(it) }
            }
        }
    }

    private fun handleEvent(event: ShareScriptEvent) {
        when (event) {
            is ShareScriptEvent.Finish -> finish()
            is ShareScriptEvent.RequestPermission -> {
                requestPermissionLauncher.launch("com.termux.permission.RUN_COMMAND")
            }
            is ShareScriptEvent.ShowError -> {
                Toast.makeText(this, event.message, Toast.LENGTH_LONG).show()
            }
        }
    }
}
