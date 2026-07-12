package io.github.swiftstagrime.termuxrunner.domain.model
import androidx.hilt.navigation.compose.hiltViewModel

import kotlinx.serialization.Serializable

@Serializable
enum class AutomationType {
    ONE_TIME,
    PERIODIC,
    WEEKLY,
    BOOT,
}
