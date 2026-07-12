package io.github.swiftstagrime.termuxrunner.ui.navigation

sealed class Route(val route: String) {
    data object Onboarding : Route("onboarding")
    data object Home : Route("home")
    data class Editor(val scriptId: Int) : Route("editor/$scriptId")
    data object Settings : Route("settings")
    data object TileSettings : Route("tile_settings")
    data object Automation : Route("automation")
    data object CustomTheme : Route("custom_theme")
}
