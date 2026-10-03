package com.olotpalot.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.olotpalot.core.design.theme.OlotPalotTheme
import com.olotpalot.core.design.theme.Palettes
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        // Keep the system splash up until preferences are read so the first frame already wears the saved theme.
        splash.setKeepOnScreenCondition { viewModel.preferences.value == null }
        enableEdgeToEdge()

        setContent {
            val prefs by viewModel.preferences.collectAsStateWithLifecycle()
            val p = prefs ?: return@setContent
            val isDark = Palettes.forMode(p.theme, p.customPalette).isDark
            LaunchedEffect(isDark) {
                val transparent = Color.TRANSPARENT
                enableEdgeToEdge(
                    statusBarStyle = if (isDark) SystemBarStyle.dark(transparent) else SystemBarStyle.light(transparent, transparent),
                    navigationBarStyle = if (isDark) SystemBarStyle.dark(transparent) else SystemBarStyle.light(transparent, transparent),
                )
            }
            OlotPalotTheme(mode = p.theme, custom = p.customPalette, reducedMotion = p.reducedMotion) {
                AppShell(viewModel)
            }
        }
    }
}
