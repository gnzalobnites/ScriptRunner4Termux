package io.github.swiftstagrime.termuxrunner.ui

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.splashscreen.SplashScreenViewProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import io.github.swiftstagrime.termuxrunner.ui.components.ScriptResultDialog
import io.github.swiftstagrime.termuxrunner.ui.navigation.ScriptRunnerEntryProvider
import io.github.swiftstagrime.termuxrunner.ui.theme.ScriptRunnerForTermuxTheme
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Verificar si debemos mostrar el resultado pendiente
        if (intent?.getBooleanExtra("SHOW_SCRIPT_RESULT", false) == true) {
            // Limpiamos el extra para que no se procese de nuevo al rotar la pantalla
            intent.removeExtra("SHOW_SCRIPT_RESULT")
            // Mostrar el resultado pendiente
            showPendingScriptResult()
        }

        splashScreen.setKeepOnScreenCondition {
            !mainViewModel.isReady.value
        }
        splashScreen.setOnExitAnimationListener { splashScreenView ->
            applyTerminalSlide(splashScreenView)
        }
        enableEdgeToEdge()

        setContent {
            val accent by mainViewModel.selectedAccent.collectAsStateWithLifecycle()
            val mode by mainViewModel.selectedMode.collectAsStateWithLifecycle()
            val customTheme by mainViewModel.customTheme.collectAsStateWithLifecycle()
            val scriptResult by mainViewModel.scriptResult.collectAsStateWithLifecycle()

            CompositionLocalProvider(
                LocalInspectionMode provides false,
            ) {
                ScriptRunnerForTermuxTheme(accent = accent, mode = mode, customTheme = customTheme) {
                    Surface(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .semantics { testTagsAsResourceId = true },
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        val navController = rememberNavController()
                        ScriptRunnerEntryProvider(
                            mainViewModel = mainViewModel,
                            navController = navController,
                        )
                    }

                    // Popup de resultado de ejecución, superpuesto sobre
                    // cualquier pantalla en la que esté el usuario
                    scriptResult?.let { result ->
                        ScriptResultDialog(
                            result = result,
                            onDismiss = { mainViewModel.dismissScriptResult() },
                        )
                    }
                }
            }
        }
    }

    /**
     * Recupera y muestra el resultado pendiente del script cuando la app se abre
     * desde una notificación.
     */
    private fun showPendingScriptResult() {
        lifecycleScope.launch {
            val pendingResult = mainViewModel.getPendingScriptResult()
            if (pendingResult != null) {
                mainViewModel.showScriptResult(pendingResult)
            }
        }
    }

    private fun applyTerminalSlide(splashProvider: SplashScreenViewProvider) {
        val splashScreenView = splashProvider.view

        val slideUp =
            ObjectAnimator.ofFloat(
                splashScreenView,
                View.TRANSLATION_Y,
                0f,
                -splashScreenView.height.toFloat(),
            )

        slideUp.apply {
            interpolator = OvershootInterpolator(1.2f)
            duration = 600L
            doOnEnd { splashProvider.remove() }
            start()
        }
    }
}