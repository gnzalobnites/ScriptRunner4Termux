package io.github.swiftstagrime.termuxrunner.ui.features.onboarding
import androidx.hilt.navigation.compose.hiltViewModel

data class OnboardingUiState(
    val isTermuxInstalled: Boolean = false,
    val isPermissionGranted: Boolean = false,
    val isBatteryUnrestricted: Boolean = false,
    val isLoading: Boolean = true,
)
