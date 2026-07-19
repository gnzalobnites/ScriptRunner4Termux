package io.github.swiftstagrime.termuxrunner.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ScriptExecutionResult(
    val scriptId: Int,
    val scriptName: String,
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val internalError: String?,
    val automationId: Int = -1,
) : Parcelable {
    val isSuccess: Boolean get() = exitCode == 0
    val isUnknown: Boolean get() = exitCode == UNKNOWN_EXIT_CODE

    companion object {
        const val UNKNOWN_EXIT_CODE = -1337
    }
}
