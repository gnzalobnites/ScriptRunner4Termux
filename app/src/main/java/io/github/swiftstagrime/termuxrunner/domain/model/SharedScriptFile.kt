package io.github.swiftstagrime.termuxrunner.domain.model

/**
 * Contenido leído de un archivo .sh recibido vía Intent.ACTION_SEND
 * desde otra aplicación.
 */
data class SharedScriptFile(
    val fileName: String,
    val content: String,
    // Ruta absoluta real del archivo en el almacenamiento, si se pudo resolver.
    // Null cuando el proveedor no expone una ruta de archivo real (ej. nube,
    // adjuntos de chat), en cuyo caso hay que caer de vuelta a copiar el contenido.
    val realPath: String? = null,
)
