# Fonts

The web app uses three Google Fonts, loaded via CSS `font-family` (index.html:37,47,48):
- **Fraunces** — headings (h1-h4)
- **Source Serif 4** — body text and form fields
- **IBM Plex Mono** — all financial numerals (`.num`), labels, and table headers

This sandbox's network access is restricted to package registries (npm/pypi/Maven/etc.)
and can't reach `fonts.google.com` to fetch the actual `.ttf` files, so `Type.kt`
currently falls back to `FontFamily.Serif` / `FontFamily.Monospace` (the platform's
built-in serif/mono faces) so the project **compiles and looks reasonable today**.

To get the exact web-app typography, do one of:

1. **Bundle the font files (recommended, works offline):** download the static `.ttf`
   weights you need from https://fonts.google.com/specimen/Fraunces,
   .../Source+Serif+4, and .../IBM+Plex+Mono, drop them in this folder as e.g.
   `fraunces_semibold.ttf`, `source_serif_4_regular.ttf`, `ibm_plex_mono_medium.ttf`,
   then build `FontFamily(Font(R.font.fraunces_semibold, FontWeight.SemiBold), ...)`
   and swap it into `Type.kt`'s `FrauncesFamily` / `SourceSerif4Family` / `PlexMonoFamily`.
2. **Downloadable Fonts (needs Google Play Services + network at runtime):** add
   `androidx.compose.ui:ui-text-google-fonts`, a real
   `com_google_android_gms_fonts_certs` resource (copy the standard one from Android's
   Downloadable Fonts guide — don't hand-type it), and build `FontFamily` values with
   `androidx.compose.ui.text.googlefonts.Font(GoogleFont("Fraunces"), provider, weight)`.

Either way, only `Type.kt` needs to change — every screen already references the
semantic `MaterialTheme.typography.*` roles, not a font family directly.
