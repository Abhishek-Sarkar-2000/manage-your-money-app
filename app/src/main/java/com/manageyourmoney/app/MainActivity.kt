package com.manageyourmoney.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.manageyourmoney.app.ui.navigation.MoneyNavHost
import com.manageyourmoney.app.ui.theme.ManageYourMoneyTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Android 16 (API 36) notes for this Activity:
 *  - [enableEdgeToEdge] is mandatory — API 36 removes the ability to opt out, so content
 *    is drawn behind both the status bar and the gesture/navigation bar by default.
 *    Every screen consumes `WindowInsets` itself (via Scaffold's inner padding or
 *    `Modifier.systemBarsPadding()`); nothing here reserves fixed space up front.
 *  - Predictive back is handled per-navigation-destination with Compose's `BackHandler`
 *    inside the nav graph (Phase 4), not here — `onBackPressedDispatcher` callbacks are
 *    registered where they're semantically meaningful (e.g. closing a form panel before
 *    popping the whole screen), matching the web app's own layered close-on-back model
 *    (form panel -> month detail -> home).
 *  - No `android:screenOrientation` lock exists in the manifest and none is requested
 *    here, since API 36 ignores such locks above 600dp anyway.
 */
val LocalWindowSizeClass = staticCompositionLocalOf<androidx.compose.material3.windowsizeclass.WindowSizeClass> {
    error("WindowSizeClass not provided")
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Mandatory on API 36 — draws behind system bars; individual screens apply
        // their own WindowInsets padding (see Phase 3/4 screen scaffolds).
        enableEdgeToEdge()

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)

            CompositionLocalProvider(LocalWindowSizeClass provides windowSizeClass) {
                ManageYourMoneyTheme {
                    // Edge-to-edge on API 36 is handled per-screen (Scaffold's own inner
                    // padding / systemBarsPadding), so Surface here just fills the window
                    // without reserving any inset space itself.
                    Surface(modifier = Modifier.fillMaxSize()) {
                        MoneyNavHost()
                    }
                }
            }
        }
    }
}
