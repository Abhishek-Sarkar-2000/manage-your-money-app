package com.manageyourmoney.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

@Composable
fun ManageYourMoneyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val semantics = if (darkTheme) DarkSemanticColors else LightSemanticColors

    CompositionLocalProvider(LocalMoneySemanticColors provides semantics) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MoneyTypography,
            shapes = MoneyShapeScheme,
            content = content,
        )
    }
}

/** Accessor mirroring `MaterialTheme.colorScheme` but for the web app's semantic
 *  credit/debit/amber roles, which Material3's ColorScheme has no slot for. */
object MoneyTheme {
    val semanticColors: MoneySemanticColors
        @Composable get() = LocalMoneySemanticColors.current
}

val LocalMoneySemanticColors = staticCompositionLocalOf<MoneySemanticColors> {
    error("MoneySemanticColors not provided — wrap your content in ManageYourMoneyTheme")
}
