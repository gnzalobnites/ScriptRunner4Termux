package io.github.swiftstagrime.termuxrunner.domain.model

/**
 * Contenido leído de un archivo .sh recibido vía Intent.ACTION_SEND
 * desde otra aplicación.
 */
data class SharedScriptFile(
    val fileName: String,
    val content: String,
)
