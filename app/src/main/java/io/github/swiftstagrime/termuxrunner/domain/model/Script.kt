package io.github.swiftstagrime.termuxrunner.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
enum class InteractionMode : Parcelable {
    NONE,
    TEXT_INPUT,
    MULTI_CHOICE,
}

@Parcelize
data class Script(
    val id: Int = 0,
    val name: String,
    val codePages: List<String>,
    val pageNames: List<String> = emptyList(),
    val interpreter: String = "bash",
    val fileExtension: String = "sh",
    val commandPrefix: String = "",
    val executionParams: String = "",
    val envVars: Map<String, String> = emptyMap(),
    val iconPath: String? = null,
    val runInBackground: Boolean = false, // If true, shows notification only
    val openNewSession: Boolean = true, // If true, opens Termux window
    val keepSessionOpen: Boolean = true, // If true, adds a hack to keep the screen open, don't really rely on it
    val useHeartbeat: Boolean = false, // Experimental hack to monitor script execution
    val heartbeatTimeout: Long = 30000,
    val heartbeatInterval: Long = 10000,
    val categoryId: Int? = null,
    val orderIndex: Int = 0,
    val notifyOnResult: Boolean = false,
    val interactionMode: InteractionMode = InteractionMode.NONE,
    val argumentPresets: List<String> = emptyList(),
    val prefixPresets: List<String> = emptyList(),
    val envVarPresets: List<String> = emptyList(),
    val adbCode: String? = null,
) : Parcelable {
    /**
     * Computed property that concatenates all code pages into a single string,
     * separated by newlines. This is used when sending the script to Termux.
     */
    val code: String
        get() = codePages.joinToString("\n")

    companion object {
        fun fromCode(
            code: String,
            name: String = "",
            id: Int = 0,
            interpreter: String = "bash",
            fileExtension: String = "sh",
            commandPrefix: String = "",
            executionParams: String = "",
            envVars: Map<String, String> = emptyMap(),
            iconPath: String? = null,
            runInBackground: Boolean = false,
            openNewSession: Boolean = true,
            keepSessionOpen: Boolean = true,
            useHeartbeat: Boolean = false,
            heartbeatTimeout: Long = 30000,
            heartbeatInterval: Long = 10000,
            categoryId: Int? = null,
            orderIndex: Int = 0,
            notifyOnResult: Boolean = false,
            interactionMode: InteractionMode = InteractionMode.NONE,
            argumentPresets: List<String> = emptyList(),
            prefixPresets: List<String> = emptyList(),
            envVarPresets: List<String> = emptyList(),
            adbCode: String? = null,
        ): Script = Script(
            id = id,
            name = name,
            codePages = listOf(code),
            interpreter = interpreter,
            fileExtension = fileExtension,
            commandPrefix = commandPrefix,
            executionParams = executionParams,
            envVars = envVars,
            iconPath = iconPath,
            runInBackground = runInBackground,
            openNewSession = openNewSession,
            keepSessionOpen = keepSessionOpen,
            useHeartbeat = useHeartbeat,
            heartbeatTimeout = heartbeatTimeout,
            heartbeatInterval = heartbeatInterval,
            categoryId = categoryId,
            orderIndex = orderIndex,
            notifyOnResult = notifyOnResult,
            interactionMode = interactionMode,
            argumentPresets = argumentPresets,
            prefixPresets = prefixPresets,
            envVarPresets = envVarPresets,
            adbCode = adbCode,
        )
    }
}
