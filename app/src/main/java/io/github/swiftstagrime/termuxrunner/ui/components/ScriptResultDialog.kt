package io.github.swiftstagrime.termuxrunner.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.github.swiftstagrime.termuxrunner.domain.model.ScriptExecutionResult
import io.github.swiftstagrime.termuxrunner.ui.theme.ScriptRunnerForTermuxTheme

private val SuccessColor = Color(0xFF4CAF50)
private val ErrorColor = Color(0xFFF44336)
private val UnknownColor = Color(0xFFFFA726)

/**
 * Ventana emergente con el resultado de la ejecución de un script:
 * estado (éxito/fallo), código de salida y la salida completa
 * (stdout + stderr), al estilo del popup que muestra MiX Explorer
 * al ejecutar un archivo desde su menú de tres puntos.
 */
@Composable
fun ScriptResultDialog(
    result: ScriptExecutionResult,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val statusColor =
        when {
            result.isUnknown -> UnknownColor
            result.isSuccess -> SuccessColor
            else -> ErrorColor
        }
    val statusIcon =
        when {
            result.isUnknown -> Icons.Default.HelpOutline
            result.isSuccess -> Icons.Default.CheckCircle
            else -> Icons.Default.Error
        }
    val combinedOutput =
        buildString {
            if (result.stdout.isNotBlank()) append(result.stdout.trimEnd())
            if (result.stderr.isNotBlank()) {
                if (isNotEmpty()) append("\n\n")
                append("stderr:\n")
                append(result.stderr.trimEnd())
            }
            if (isEmpty() && !result.internalError.isNullOrBlank()) {
                append(result.internalError)
            }
        }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(28.dp),
                    )
                    Spacer(modifier = Modifier.padding(horizontal = 6.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = result.scriptName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text =
                                if (result.isUnknown) {
                                    "Sin resultado (¿Termux fue cerrado?)"
                                } else {
                                    "Código de salida: ${result.exitCode}"
                                },
                            style = MaterialTheme.typography.bodySmall,
                            color = statusColor,
                        )
                    }
                }

                Spacer(modifier = Modifier.padding(vertical = 6.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.padding(vertical = 6.dp))

                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 80.dp, max = 320.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceContainerLowest,
                                RoundedCornerShape(12.dp),
                            ).padding(12.dp)
                            .verticalScroll(scrollState),
                ) {
                    Text(
                        text = combinedOutput.ifBlank { "(sin salida)" },
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { copyToClipboard(context, combinedOutput) }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copiar salida")
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismiss) {
                        Text("Cerrar")
                    }
                }
            }
        }
    }
}

private fun copyToClipboard(
    context: Context,
    text: String,
) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Resultado del script", text))
}

@Preview(showBackground = true)
@Composable
private fun PreviewScriptResultDialogSuccess() {
    ScriptRunnerForTermuxTheme {
        ScriptResultDialog(
            result =
                ScriptExecutionResult(
                    scriptId = 1,
                    scriptName = "backup.sh",
                    exitCode = 0,
                    stdout = "Copiando archivos...\nListo. 128 MB respaldados.",
                    stderr = "",
                    internalError = null,
                ),
            onDismiss = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewScriptResultDialogError() {
    ScriptRunnerForTermuxTheme {
        ScriptResultDialog(
            result =
                ScriptExecutionResult(
                    scriptId = 1,
                    scriptName = "deploy.sh",
                    exitCode = 1,
                    stdout = "Conectando al servidor...",
                    stderr = "rsync: connection unexpectedly closed",
                    internalError = null,
                ),
            onDismiss = {},
        )
    }
}
