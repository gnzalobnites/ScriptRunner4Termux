package io.github.swiftstagrime.termuxrunner.domain.repository
import androidx.hilt.navigation.compose.hiltViewModel

interface ScriptFileRepository {
    fun saveToBridge(
        fileName: String,
        code: String,
    ): String
}
