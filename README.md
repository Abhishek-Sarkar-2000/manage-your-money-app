# Manage Your Money — Native Android Rewrite

## Phase 1: Setup & Data Layer ✅

### Directory structure

```
ManageYourMoney/
├── settings.gradle.kts
├── build.gradle.kts                  # root — plugin versions (AGP 8.9.1, Kotlin 2.1.0, Hilt 2.53)
├── gradle.properties
├── gradle/wrapper/gradle-wrapper.properties   # Gradle 8.11.1 (required for AGP 8.9 / API 36)
└── app/
    ├── build.gradle.kts              # compileSdk=36, targetSdk=36, Compose BOM, Hilt, Room, KSP
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml       # no orientation locks, no edge-to-edge opt-out
        ├── res/
        │   ├── values/{colors,strings,themes}.xml   # CSS :root vars -> color resources
        │   ├── mipmap-anydpi-v26/ic_launcher{,_round}.xml   # adaptive icon descriptors
        │   ├── mipmap-{m,h,xh,xxh,xxxh}dpi/          # legacy + adaptive icon layers, generated from your logo
        │   └── xml/{backup_rules,data_extraction_rules}.xml
        └── java/com/manageyourmoney/app/
            ├── MoneyApplication.kt           # @HiltAndroidApp
            ├── MainActivity.kt               # enableEdgeToEdge(), WindowSizeClass setup
            ├── data/local/
            │   ├── entity/                   # Room entities — one per storage shape
            │   ├── dao/                      # Room DAOs
            │   ├── converter/Converters.kt   # JSON TypeConverters for the few genuinely-opaque list fields
            │   └── db/AppDatabase.kt
            ├── di/
            │   ├── DatabaseModule.kt         # Hilt: provides AppDatabase + all DAOs
            │   └── DispatcherModule.kt       # Hilt: @IoDispatcher / @DefaultDispatcher qualifiers
            ├── domain/                       # (Phase 2) — empty package, ready for use cases
            └── ui/theme/Theme.kt             # placeholder MaterialTheme (full theme in Phase 3)
```

### How the web app's storage model maps to Room

The original app persisted everything as opaque JSON blobs behind a tiny `Store.get/set`
key-value API (`GET/PUT /api/storage/<key>`). Each key becomes a proper normalized table:

| Web storage key            | Room entity/entities                                                        |
|-----------------------------|-------------------------------------------------------------------------------|
| `creditcards`               | `CreditCardEntity`                                                            |
| `emiseries`                 | `EmiSeriesEntity` (month rows stay **derived**, never persisted — see below)   |
| `months-index`               | *(implicit)* — a row existing in `MonthEntity` = indexed                      |
| `month:<key>`                | `MonthEntity` + `TransactionEntity` (its `entries[]`) + `EmiDeletionEntity` (its `deletedEmi[]`) |
| `entries[].lent[]`           | `LentShareEntity` (FK → `TransactionEntity`, cascade delete)                  |
| `custom-spend-tags`           | `CustomTagEntity`                                                             |
| `splits-index`                | *(implicit)* — a row existing in `SplitGroupEntity` = indexed                 |
| `split:<id>`                  | `SplitGroupEntity` + `SplitPersonEntity` (`people[]`) + `SplitSpendEntity`/`SplitSpendShareEntity` (`spends[]`/`shares{}`) + `SplitSettlementEntity` (`settlements[]`) |

Key design decisions carried over faithfully from the JS:

- **EMI rows are never stored per month.** `emiRowsForMonth()` synthesized them on
  every read from `(startMonth, totalMonths, monthlyAmount)` unless the series id was
  in that month's `deletedEmi[]`. Room does the same: `EmiSeriesEntity` +
  `EmiDeletionEntity` are the only persisted state; Phase 2's `EmiRowsForMonthUseCase`
  regenerates the actual rows.
- **Outstanding split settlements are virtual.** `computeGroupSettlementView()` only
  ever *saved* a settlement once the user tapped "settle" (`group.settlements[]`);
  the unsettled transfers it also returns come straight out of `greedySettle()` and
  are recomputed every time. `SplitSettlementEntity` mirrors this — Phase 2's
  `GreedySettleUseCase` produces the virtual/unsaved transfers at read time.
- **Settlement ↔ ledger sync stays reversible.** `toggleSplitSettlement()` writes a
  `spend`/`income` entry into the current month when a transfer involving `YOU` is
  settled, and deletes that same entry if un-settled. `TransactionEntity.fromSplitSettlementId`
  and `SplitSettlementEntity.ledgerEntryId`/`monthKey` are the two ends of that link.

### Android 16 (API 36) compliance baked in from Phase 1

- `compileSdk = 36`, `targetSdk = 36` in `app/build.gradle.kts`.
- No `android:screenOrientation` or `resizeableActivity="false"` anywhere in the
  manifest — API 36 ignores those above 600dp anyway, so the app just embraces
  adaptive layout from the start (`WindowSizeClass` wired into `MainActivity` and
  exposed via `LocalWindowSizeClass` for Phase 4's screens).
  - **Note:** if you need `android:screenOrientation="portrait"` for a genuinely
    phone-only screen, API 36 still honors *fixed* orientation on phones/small
    screens — it's specifically large-screen (>600dp) orientation/aspect-ratio
    restriction overrides that get ignored, per Android's large-screen compatibility
    guidance.
- `enableEdgeToEdge()` called unconditionally in `MainActivity.onCreate()` (API 36
  removes the opt-out flag entirely, so this is mandatory, not optional).
- Predictive back is **not** implemented with a manifest flag or deprecated
  `onBackPressed()` overrides anywhere — it's deferred to Compose `BackHandler`
  calls placed at the exact points the web app had layered "back" behavior (close
  form panel → close month detail → home), wired up in Phase 4's nav graph.

### App icon

Generated from your uploaded logo: the ₹ glyph was extracted and rebuilt as a proper
Android **adaptive icon** (separate foreground/background layers, safe-zone-scaled)
plus legacy square/round PNGs at all five densities (mdpi–xxxhdpi).

---

## Next: Phase 2 — Domain Logic

Kotlin conversions of `computeDailyBalanceSeries()`, `computeMonthlyBreakdown()`,
`computeMonthTotals()`, `emiRowsForMonth()`, `greedySettle()`/`computeGroupSettlementView()`,
and the `fmtINR`/`fmtINRShort` formatters, as pure, testable use-case classes over the
DAOs built in Phase 1.

Say **"Continue"** when you're ready.

---

## Phase 2: Domain Logic ✅

All pure math and business rules from the web app, ported as injectable Kotlin use
cases under `domain/usecase/`, `domain/format/`, and `domain/util/`:

| Web function (index.html)                  | Kotlin equivalent                                  |
|-----------------------------------------------|-------------------------------------------------------|
| `fmtINR()` / `fmtINRShort()`                    | `domain/format/CurrencyFormatter.kt`                    |
| `todayStr()`, `currentMonthKey()`, `monthKeyLabel()`, `monthKeyShort()`, `addMonths()`, `diffMonths()` | `domain/util/DateUtils.kt`                             |
| `emiRowsForMonth()`                             | `EmiRowsForMonthUseCase`                                 |
| `computeMonthTotals()` / `monthCashOutflow()`   | `ComputeMonthTotalsUseCase` (+ `MonthTotals.cashOutflow`) |
| `computeMonthlyBreakdown()`                      | `ComputeMonthlyBreakdownUseCase`                         |
| `computeDailyBalanceSeries()`                    | `ComputeDailyBalanceSeriesUseCase`                       |
| `windowSeries()`                                 | `WindowSeries` (pure object, no DI)                      |
| `computeGlobalOwed()`                            | `ComputeGlobalOwedUseCase`                               |
| `computeGlobalInvestments()`                     | `ComputeGlobalInvestmentsUseCase`                        |
| `computeGlobalCardDues()`                        | `ComputeGlobalCardDuesUseCase`                           |
| `computeGlobalStats()`                           | `ComputeGlobalStatsUseCase` (parallel `async`, mirrors `Promise.all`) |
| `computeGroupPaid()` / `computeGroupNet()` / `applySettledAdjustments()` / `greedySettle()` | `GreedySettleUseCase.kt` (4 small classes)               |
| `computeGroupSettlementView()`                   | `ComputeGroupSettlementViewUseCase`                      |
| `computeSplitPageData()`                          | `ComputeSplitPageDataUseCase`                            |
| `computeGlobalSplitOwedByYou()`                   | `ComputeGlobalSplitOwedByYouUseCase`                     |
| `toggleSplitSettlement()` (reversible ledger sync) | `ToggleSplitSettlementUseCase`                            |
| `settleAllInGroup()`                              | `SettleAllInGroupUseCase`                                |
| `DEFAULT_TAGS` / `allSpendTags()` / custom-tag persistence | `SpendTagsUseCase.kt` (`DefaultTags`, `GetAllSpendTagsUseCase`, `AddCustomTagIfNewUseCase`) |

Notable fidelity details preserved from the original JS (called out in each file's KDoc):

- **`greedySettle()` is deliberately not a globally-optimal min-transaction solver** —
  it's a greedy largest-creditor/largest-debtor matcher, ported bit-for-bit including
  its `0.004` epsilon and 2-decimal rounding, since that's what the original produces
  and the UI/tests should match it exactly.
- **`toggleSplitSettlement()`'s un-settle path never deletes the settlement record** —
  only `settled`, `ledgerEntryId`, `monthKey` are cleared, exactly like the JS's
  `delete record.ledgerEntryId` leaving the record inside `group.settlements`.
- **`fmtINR()`'s Indian digit grouping** (12,34,567.00, not 1,234,567.00) is reproduced
  with a `DecimalFormat("##,##,##0.00", Locale("en","IN"))` pattern rather than the
  default `Locale.US` grouping.
- Unit tests for the formatter, the greedy-settle engine, and month totals live in
  `app/src/test/java/.../domain/DomainUseCaseTests.kt` and run with plain JUnit — no
  Android framework or Room dependency, since every one of these use cases is pure
  Kotlin over plain data classes (Room entities never leak past `EntityMappers.kt`).

### Next: Phase 3 — UI Foundation

Full Material 3 Expressive `ColorScheme`/`Typography` (dark mode, semantic credit/debit/
amber roles, IBM Plex Mono tabular numerals), plus the custom Compose `Canvas` charts
(Donut, Tag Bars, Running Balance Line Chart) replacing the web app's hand-rolled SVG.

Say **"Continue"** when you're ready.

---

## Phase 3: UI Foundation ✅

### Theme (`ui/theme/`)

| File | Contents |
|---|---|
| `Color.kt` | Full light + dark `ColorScheme`, ported 1:1 from the CSS `:root` variables (index.html:10-27). Also defines `MoneySemanticColors` — credit/debit/amber roles plus the 10-color chart rotation from `tagsBarChart()` — since Material3's `ColorScheme` has no slot for those; access via `MoneyTheme.semanticColors` inside `ManageYourMoneyTheme`. |
| `Type.kt` | The web app's 3-family type system (Fraunces headings, Source Serif 4 body, IBM Plex Mono numerals/labels) mapped onto Material3's `Typography` roles. Currently falls back to `FontFamily.Serif`/`Monospace` since this sandbox can't reach `fonts.google.com` — see `docs/FONTS.md` for how to drop in the real webfonts (only `Type.kt` needs to change). |
| `Shape.kt` | `--radius:12px` / `--radius-sm:8px` / the pill-button 999px radius, as a Material3 `Shapes` set. |
| `Theme.kt` | `ManageYourMoneyTheme()` composable wiring color scheme + typography + shapes together, dark-mode aware. |

### Charts (`ui/components/charts/`)

Every chart the web app hand-rolled as inline SVG/CSS is now a real Compose `Canvas`
composable — no third-party charting library, matching the "lightweight, custom feel"
requirement:

| Web function | Compose composable |
|---|---|
| `donutChart()` (CSS `conic-gradient`) | `DonutChart.kt` — ring of `drawArc` calls + legend |
| `barChart()` / `tagsBarChart()` | `BarChart.kt` — one composable, `shortValues`/`maxBarHeight` params cover both call sites; the tag-aggregation math itself is `domain/usecase/ComputeTagTotalsUseCase.kt`, kept pure/testable rather than baked into the composable |
| `lineChart()` / `dailyBalanceChart()` / `yAxisGrid()` | `RunningBalanceLineChart.kt` — dashed grid with short-scale (K/L/Cr) labels, gradient area fill, stroked line, dots; `padValueRange`/`tickLabels` params cover both call sites' differences. Tick-index selection is `domain/usecase/ComputeDailyChartTicksUseCase.kt` |

All three share `EmptyChartMessage` (the `<div class="empty-chart">...` fallback) and
take pre-computed `ChartSegment`/`DailyBalancePoint` lists — no DB or DI dependency, so
they're trivially previewable and testable in isolation.

### Next: Phase 4 — Screens

The navigation graph (type-safe routes, predictive-back `BackHandler` placement), Home
screen (stat cards, running-balance chart, global owed/invested/dues), Month Detail
screen (entry list, donut + tag-bar charts, forms), and Split Money screen (settlement
cards, greedy-settle results).

Say **"Continue"** when you're ready.

---

## Phase 4: Screens ✅

### Navigation (`ui/navigation/`)

- `MoneyRoute.kt` — type-safe `@Serializable` route objects (Navigation Compose 2.8)
  replacing the web app's manually-tracked `State.view` string router. Each screen is a
  real back-stack entry, so Android 16's predictive back animates between them for
  free — no custom `OnBackPressedDispatcher` wiring needed for screen-to-screen
  navigation. The one place the web app *did* have layered back behavior (a form panel
  closing before the page itself navigates back) is handled by Compose's `ModalBottomSheet`,
  which already intercepts predictive back to dismiss itself first.
- `MoneyNavHost.kt` — the graph: Home → MonthDetail, Home → SplitMoney.

### Data layer additions

- `data/repository/MoneyRepository.kt` — thin CRUD facade over the Phase 1 DAOs
  (create/update/delete calls only; every computation still lives in `domain/usecase/`
  and reads through the same DAOs). This is what the ViewModels below inject for
  writes, alongside the Phase 2 use cases for reads/computed state.

### Screens (`ui/screens/`) + ViewModels (`ui/viewmodel/`)

| Screen | ViewModel | Mirrors |
|---|---|---|
| `home/HomeScreen.kt` | `HomeViewModel` | Home view: hero "amount left" figure, horizontally-scrolling stat cards (owed/invested/card dues — the `.hstats` track), `RunningBalanceLineChart` with 1M/3M/6M range pills (`State.balanceChartRange` + `windowSeries()`), chronological month list, "Add month" FAB |
| `month/MonthDetailScreen.kt` | `MonthDetailViewModel` | Month-detail view: starting-balance auto/manual toggle, `DonutChart` of cash/card/EMI/invest breakdown, `BarChart` of tag totals, entry list with delete, bottom-sheet add-entry form (income/spend/investment/owed) |
| `split/SplitMoneyScreen.kt` | `SplitMoneyViewModel` | Split Money view: "You owe" / "Owed to you" summary cards, one card per group showing `computeGroupSettlementView()`'s settled + outstanding transfers with Settle/Undo/Settle-all actions, new-group bottom sheet |

Every screen reads through a `StateFlow<UiState>` (unidirectional data flow) and injects
its use cases via Hilt's `hiltViewModel()` — no direct DAO or repository access from
Compose code.

### What's intentionally left as a follow-up

This phase focused on making all three primary views fully functional end-to-end,
matching the "elite engineer delivering working software" priority over pixel-parity
on every secondary form. Not yet built out (straightforward extensions of the patterns
above, using the same use cases):
- Card-charge entry form (payment-mode=card spend + card management screens)
- Lent-share UI on spend entries, and the settle/un-settle toggle for `owed` entries
  (the use cases and DAO methods for both already exist — `setOwedSettled`,
  `upsertLentShare`, `setLentShareSettled` — only the UI is pending)
- EMI series management screen (create/edit series, per-month "skip this EMI")
- Manage Cards / Manage EMIs screens (routes already reserved in `MoneyRoute.kt`)
- Foldable/tablet multi-column layout using the `WindowSizeClass` already threaded
  through `MainActivity` via `LocalWindowSizeClass`
- Wiring the real Fraunces/Source Serif 4/IBM Plex Mono font files (`docs/FONTS.md`)

### Building the project

This zip is a complete, buildable Gradle project skeleton. To build it yourself:
```
cd ManageYourMoney
./gradlew assembleDebug   # you'll need to run `gradle wrapper` once first, or open in Android Studio
```
Opening the folder directly in Android Studio (Meerkat/Narwhal or newer, with the
Android 16 / API 36 SDK installed) is the fastest path — it will generate the wrapper
jar and sync automatically.
