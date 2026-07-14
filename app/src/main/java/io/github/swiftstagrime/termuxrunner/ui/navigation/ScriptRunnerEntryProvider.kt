package io.github.swiftstagrime.termuxrunner.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.swiftstagrime.termuxrunner.ui.MainViewModel
import io.github.swiftstagrime.termuxrunner.ui.features.automation.AutomationRoute
import io.github.swiftstagrime.termuxrunner.ui.features.customtheme.CustomThemeRoute
import io.github.swiftstagrime.termuxrunner.ui.features.editor.EditorRoute
import io.github.swiftstagrime.termuxrunner.ui.features.editor.EditorViewModel
import io.github.swiftstagrime.termuxrunner.ui.features.home.HomeRoute
import io.github.swiftstagrime.termuxrunner.ui.features.onboarding.OnboardingRoute
import io.github.swiftstagrime.termuxrunner.ui.features.settings.SettingsRoute
import io.github.swiftstagrime.termuxrunner.ui.features.tiles.TileSettingsRoute

@Composable
fun ScriptRunnerEntryProvider(
    mainViewModel: MainViewModel,
    navController: NavHostController = rememberNavController()
) {
    val startDestination by mainViewModel.startDestination.collectAsStateWithLifecycle()
    val destination = startDestination ?: return

    NavHost(
        navController = navController,
        startDestination = destination
    ) {
        composable("onboarding") {
            OnboardingRoute(
                onSetupFinished = {
                    navController.navigate("home") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }

        composable("home") {
            HomeRoute(
                onNavigateToEditor = { scriptId ->
                    navController.navigate("editor/$scriptId")
                },
                onNavigateToSettings = {
                    navController.navigate("settings")
                },
                onNavigateToTileSettings = {
                    navController.navigate("tile_settings")
                },
                onNavigateToAutomation = {
                    navController.navigate("automation")
                }
            )
        }

        composable("editor/{scriptId}") { backStackEntry ->
            val scriptId = backStackEntry.arguments?.getString("scriptId")?.toIntOrNull() ?: 0
            val viewModel: EditorViewModel = hiltViewModel()

            LaunchedEffect(scriptId) {
                viewModel.loadScript(scriptId.toInt())
            }

            EditorRoute(
                onBack = { navController.popBackStack() },
                viewModel = viewModel
            )
        }

        composable("settings") {
            SettingsRoute(
                onBack = { navController.popBackStack() },
                onNavigateToEditor = { scriptId ->
                    navController.navigate("editor/$scriptId")
                },
                onNavigateToCustomTheme = {
                    navController.navigate("custom_theme")
                }
            )
        }

        composable("tile_settings") {
            TileSettingsRoute(
                onBack = { navController.popBackStack() }
            )
        }

        composable("automation") {
            AutomationRoute(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("custom_theme") {
            CustomThemeRoute(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
