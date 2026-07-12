package io.github.swiftstagrime.termuxrunner.domain.repository
import androidx.hilt.navigation.compose.hiltViewModel

interface TermuxRepository {
    fun isTermuxInstalled(): Boolean

    fun isPermissionGranted(): Boolean

    fun requestTermuxOverlay()

    fun runCommand(
        command: String,
        runInBackground: Boolean,
        sessionAction: String,
        scriptId: Int,
        scriptName: String,
        notifyOnResult: Boolean,
        automationId: Int? = null,
    )

    fun isTermuxBatteryOptimized(): Boolean
}
