package com.manageyourmoney.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Ported 1:1 from the web app's :root CSS custom properties (index.html:10-27).
// The XML-side copy used by the splash screen / launcher backdrop lives in colors.xml
// and must be kept in sync with these if either changes.
object MoneyColors {
    val Navy = Color(0xFF0B2545)
    val Royal = Color(0xFF13315C)
    val Blue = Color(0xFF2F6FB0)
    val BlueSoft = Color(0xFF5C8FC4)
    val Sky = Color(0xFF9BC4E4)
    val Ice = Color(0xFFEAF3FC)
    val Ice2 = Color(0xFFF5F9FD)
    val Paper = Color(0xFFFCFDFF)
    val Ink = Color(0xFF10243F)
    val Muted = Color(0xFF5D7290)
    val Hair = Color(0xFFD3E2F1)
    val Credit = Color(0xFF2E7D6B)
    val CreditBg = Color(0xFFE6F3EF)
    val Debit = Color(0xFFAD4358)
    val DebitBg = Color(0xFFFBEBEE)
    val Amber = Color(0xFF8A6A2F)
    val AmberBg = Color(0xFFF7F0DF)

    // Dark-mode counterparts — the web app has no dark theme to port, so these are a
    // reasonable tonal inversion keeping the same hues (darker backgrounds, desaturated
    // containers, lightened on-colors) rather than a literal translation.
    val NavyDark = Color(0xFF0A1A30)
    val SurfaceDark = Color(0xFF0F2038)
    val SurfaceVariantDark = Color(0xFF16304F)
    val InkDark = Color(0xFFE7EFF9)
    val MutedDark = Color(0xFF9AB0CB)
    val HairDark = Color(0xFF25476E)
    val CreditDark = Color(0xFF6FCBB2)
    val CreditBgDark = Color(0xFF16332C)
    val DebitDark = Color(0xFFE895A4)
    val DebitBgDark = Color(0xFF3A1E24)
    val AmberDark = Color(0xFFE0C486)
    val AmberBgDark = Color(0xFF3A311C)
}

/** Semantic roles the web app expressed as `.amt-credit`/`.amt-debit`/tag colors, which
 *  Material3's [androidx.compose.material3.ColorScheme] has no built-in slot for.
 *  Access via `MoneyTheme.semanticColors` inside any composable under [ManageYourMoneyTheme]. */
data class MoneySemanticColors(
    val credit: Color,
    val creditContainer: Color,
    val debit: Color,
    val debitContainer: Color,
    val amber: Color,
    val amberContainer: Color,
    /** Ported from `tagsBarChart()`'s hardcoded 10-color rotation (index.html:2189),
     *  used for both the tag bar chart and the spend-category donut chart. */
    val chartPalette: List<Color>,
)

val LightSemanticColors = MoneySemanticColors(
    credit = MoneyColors.Credit,
    creditContainer = MoneyColors.CreditBg,
    debit = MoneyColors.Debit,
    debitContainer = MoneyColors.DebitBg,
    amber = MoneyColors.Amber,
    amberContainer = MoneyColors.AmberBg,
    chartPalette = listOf(
        MoneyColors.Blue, Color(0xFFC98A3C), Color(0xFF8E6FB0), MoneyColors.Debit,
        MoneyColors.Credit, Color(0xFF5B4B9E), MoneyColors.Amber, MoneyColors.BlueSoft,
        Color(0xFF2E7D6B), Color(0xFFAD4358),
    ),
)

val DarkSemanticColors = MoneySemanticColors(
    credit = MoneyColors.CreditDark,
    creditContainer = MoneyColors.CreditBgDark,
    debit = MoneyColors.DebitDark,
    debitContainer = MoneyColors.DebitBgDark,
    amber = MoneyColors.AmberDark,
    amberContainer = MoneyColors.AmberBgDark,
    chartPalette = listOf(
        MoneyColors.BlueSoft, Color(0xFFE0A868), Color(0xFFB39DD6), MoneyColors.DebitDark,
        MoneyColors.CreditDark, Color(0xFF9385C9), MoneyColors.AmberDark, MoneyColors.Sky,
        Color(0xFF6FCBB2), Color(0xFFE895A4),
    ),
)

val LightColors = lightColorScheme(
    primary = MoneyColors.Blue,
    onPrimary = Color.White,
    primaryContainer = MoneyColors.Ice,
    onPrimaryContainer = MoneyColors.Navy,
    secondary = MoneyColors.Royal,
    onSecondary = Color.White,
    background = MoneyColors.Paper,
    onBackground = MoneyColors.Ink,
    surface = MoneyColors.Paper,
    onSurface = MoneyColors.Ink,
    surfaceVariant = MoneyColors.Ice,
    onSurfaceVariant = MoneyColors.Muted,
    error = MoneyColors.Debit,
    onError = Color.White,
    errorContainer = MoneyColors.DebitBg,
    onErrorContainer = MoneyColors.Debit,
    outline = MoneyColors.Hair,
    outlineVariant = MoneyColors.Ice2,
)

val DarkColors = darkColorScheme(
    primary = MoneyColors.BlueSoft,
    onPrimary = MoneyColors.NavyDark,
    primaryContainer = MoneyColors.SurfaceVariantDark,
    onPrimaryContainer = MoneyColors.Sky,
    secondary = MoneyColors.Sky,
    onSecondary = MoneyColors.NavyDark,
    background = MoneyColors.NavyDark,
    onBackground = MoneyColors.InkDark,
    surface = MoneyColors.SurfaceDark,
    onSurface = MoneyColors.InkDark,
    surfaceVariant = MoneyColors.SurfaceVariantDark,
    onSurfaceVariant = MoneyColors.MutedDark,
    error = MoneyColors.DebitDark,
    onError = MoneyColors.NavyDark,
    errorContainer = MoneyColors.DebitBgDark,
    onErrorContainer = MoneyColors.DebitDark,
    outline = MoneyColors.HairDark,
    outlineVariant = MoneyColors.SurfaceVariantDark,
)
