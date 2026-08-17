package com.manageyourmoney.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Ports index.html's 3-family type system:
 *  - Fraunces (headings, h1-h4, index.html:47)      -> [FrauncesFamily]
 *  - Source Serif 4 (body/buttons/inputs, :37,111,133) -> [SourceSerif4Family]
 *  - IBM Plex Mono (`.num` — every financial figure, tags, table headers, :48) -> [PlexMonoFamily]
 *
 * These currently fall back to the platform's built-in serif/monospace faces — see
 * `app/src/main/res/font/` (docs/FONTS.md) for how to swap in the real webfonts.
 * Every screen should reference `MaterialTheme.typography.*` rather than these
 * families directly, so that swap is the only file that ever needs to change.
 */
val FrauncesFamily = FontFamily.Serif
val SourceSerif4Family = FontFamily.Serif
val PlexMonoFamily = FontFamily.Monospace

val MoneyTypography = Typography(
    // ---- Fraunces headings ----
    displayLarge = TextStyle(fontFamily = FrauncesFamily, fontWeight = FontWeight.SemiBold, fontSize = 34.sp, lineHeight = 40.sp, letterSpacing = (-0.2).sp),
    headlineLarge = TextStyle(fontFamily = FrauncesFamily, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-0.2).sp),
    headlineMedium = TextStyle(fontFamily = FrauncesFamily, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = (-0.1).sp),
    headlineSmall = TextStyle(fontFamily = FrauncesFamily, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleLarge = TextStyle(fontFamily = FrauncesFamily, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp),

    // ---- Source Serif 4 body / labels / buttons ----
    titleMedium = TextStyle(fontFamily = SourceSerif4Family, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = SourceSerif4Family, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = SourceSerif4Family, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = SourceSerif4Family, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = SourceSerif4Family, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = SourceSerif4Family, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),

    // ---- IBM Plex Mono — tabular figures, eyebrows, tags, table headers ----
    labelMedium = TextStyle(fontFamily = PlexMonoFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.6.sp),
    labelSmall = TextStyle(fontFamily = PlexMonoFamily, fontWeight = FontWeight.Medium, fontSize = 10.sp, lineHeight = 14.sp, letterSpacing = 0.5.sp),
)

/** Convenience style for tabular financial figures anywhere in the UI — mirrors the
 *  web app's `.num { font-variant-numeric: tabular-nums }` utility class. Compose
 *  doesn't expose tabular-figure OpenType features directly via TextStyle, so callers
 *  should pair this with [PlexMonoFamily]'s naturally-monospaced digits instead. */
val TabularNumberStyle = TextStyle(fontFamily = PlexMonoFamily, fontFeatureSettings = "tnum")
