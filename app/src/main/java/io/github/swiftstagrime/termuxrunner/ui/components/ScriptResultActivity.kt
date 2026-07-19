package io.github.swiftstagrime.termuxrunner.ui.components

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import io.github.swiftstagrime.termuxrunner.domain.model.ScriptExecutionResult
import io.github.swiftstagrime.termuxrunner.ui.theme.ScriptRunnerForTermuxTheme

@AndroidEntryPoint
class ScriptResultActivity : ComponentActivity() {
    companion object {
        const val EXTRA_RESULT = "script_result"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val result = intent.getParcelableExtra(EXTRA_RESULT) as? ScriptExecutionResult

        setContent {
            ScriptRunnerForTermuxTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.3f)
                ) {
                    if (result != null) {
                        ScriptResultDialog(
                            result = result,
                            onDismiss = { finish() }
                        )
                    } else {
                        finish()
                    }
                }
            }
        }
    }
}
