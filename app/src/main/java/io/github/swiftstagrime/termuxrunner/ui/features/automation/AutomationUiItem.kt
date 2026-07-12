package io.github.swiftstagrime.termuxrunner.ui.features.automation
import androidx.hilt.navigation.compose.hiltViewModel

import io.github.swiftstagrime.termuxrunner.domain.model.Automation

data class AutomationUiItem(
    val automation: Automation,
    val scriptName: String,
    val scriptIconPath: String?,
    val nextRunText: String,
    val lastRunText: String,
    val statusColor: Int,
)
