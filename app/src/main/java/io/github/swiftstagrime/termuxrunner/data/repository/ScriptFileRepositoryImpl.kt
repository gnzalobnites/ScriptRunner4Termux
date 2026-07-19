package io.github.swiftstagrime.termuxrunner.data.repository
import androidx.hilt.navigation.compose.hiltViewModel

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.swiftstagrime.termuxrunner.R
import io.github.swiftstagrime.termuxrunner.domain.model.SharedScriptFile
import io.github.swiftstagrime.termuxrunner.domain.repository.ScriptFileRepository
import java.io.File
import java.io.IOException
import javax.inject.Inject

/**
 * Repo for saving script to bridge folder if they are close to possibly exceeding Intent
 */
class ScriptFileRepositoryImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : ScriptFileRepository {
        override fun saveToBridge(
            fileName: String,
            code: String,
        ): String {
            val downloadDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

            val folderName = context.getString(R.string.bridge_folder_name)
            val bridgeDir = File(downloadDir, folderName)

            if (!bridgeDir.exists()) {
                if (!bridgeDir.mkdirs()) {
                    throw IOException(context.getString(R.string.error_create_bridge_dir))
                }
            }

            val bridgeFile = File(bridgeDir, fileName)
            bridgeFile.writeText(code)

            return bridgeFile.absolutePath
        }

        override fun readSharedFile(uri: Uri): SharedScriptFile {
            val displayName = queryDisplayName(uri) ?: uri.lastPathSegment ?: "shared_script.sh"

            val content =
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.bufferedReader().readText()
                } ?: throw IOException(context.getString(R.string.error_read_shared_file))

            return SharedScriptFile(
                fileName = displayName,
                content = content,
                realPath = resolveRealPath(uri),
            )
        }

        private fun queryDisplayName(uri: Uri): String? =
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) {
                    cursor.getString(nameIndex)
                } else {
                    null
                }
            }

        /**
         * Intenta resolver la ruta absoluta real en disco del Uri compartido,
         * para poder ejecutar el archivo original en lugar de copiar su
         * contenido. Devuelve null si el proveedor no expone una ruta real
         * (ej. Google Drive, adjuntos de apps de chat) o si el archivo
         * resuelto no existe.
         */
        private fun resolveRealPath(uri: Uri): String? =
            try {
                val candidate =
                    when {
                        uri.scheme == "file" -> uri.path

                        DocumentsContract.isDocumentUri(context, uri) &&
                            uri.authority == "com.android.externalstorage.documents" -> {
                            val docId = DocumentsContract.getDocumentId(uri)
                            val parts = docId.split(":", limit = 2)
                            if (parts.size == 2) {
                                val (volumeId, relativePath) = parts
                                val root =
                                    if (volumeId.equals("primary", ignoreCase = true)) {
                                        Environment.getExternalStorageDirectory().absolutePath
                                    } else {
                                        "/storage/$volumeId"
                                    }
                                "$root/$relativePath"
                            } else {
                                null
                            }
                        }

                        else -> queryDataColumn(uri)
                    }

                candidate?.takeIf { File(it).exists() }
            } catch (_: Exception) {
                null
            }

        private fun queryDataColumn(uri: Uri): String? =
            try {
                context.contentResolver.query(uri, arrayOf("_data"), null, null, null)?.use { cursor ->
                    val dataIndex = cursor.getColumnIndex("_data")
                    if (dataIndex >= 0 && cursor.moveToFirst()) {
                        cursor.getString(dataIndex)
                    } else {
                        null
                    }
                }
            } catch (_: Exception) {
                null
            }
    }
