package io.github.swiftstagrime.termuxrunner.domain.model
import androidx.hilt.navigation.compose.hiltViewModel

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ScriptRuntimeParams(
    val arguments: String = "",
    val prefix: String = "",
    val envVars: Map<String, String> = emptyMap(),
) : Parcelable
