package io.github.swiftstagrime.termuxrunner.domain.model

/**
 * Resultado completo de la ejecución de un script en Termux, incluyendo
 * la salida estándar y de error, para poder mostrarlo en una ventana
 * emergente (al estilo del popup de resultado de MiX Explorer) además
 * de (u opcionalmente en lugar de) la notificación del sistema.
 */
data class ScriptExecutionResult(
    val scriptId: Int,
    val scriptName: String,
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val internalError: String?,
    val automationId: Int = -1,
) {
    val isSuccess: Boolean get() = exitCode == 0
    val isUnknown: Boolean get() = exitCode == UNKNOWN_EXIT_CODE

    companion object {
        const val UNKNOWN_EXIT_CODE = -1337
    }
}
