package io.github.swiftstagrime.termuxrunner.domain.repository
import androidx.hilt.navigation.compose.hiltViewModel

import android.net.Uri
import io.github.swiftstagrime.termuxrunner.domain.model.SharedScriptFile

interface ScriptFileRepository {
    fun saveToBridge(
        fileName: String,
        code: String,
    ): String

    fun readSharedFile(uri: Uri): SharedScriptFile
}
