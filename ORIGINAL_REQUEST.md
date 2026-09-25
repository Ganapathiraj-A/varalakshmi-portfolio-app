# Original User Request

## 2026-09-21T08:30:18Z

Perform a comprehensive 360° code review, security, performance, and financial data audit of the Varalakshmi Portfolio Android application, generate a structured audit report, and implement high-priority remediations.

Working directory: `/home/ganapathiraj/Code/Stock Research/Unified UI/app`
Integrity mode: development

## Requirements

### R1. Comprehensive 360° Forensic Audit
Execute an in-depth audit across the following domains:
1. **Financial Precision & Invariants**: Validate that NAV, unrealized PnL, deployed capital, and available cash calculations maintain mathematical consistency (NAV = Deployed + Available + UnrealizedPnL) without floating-point drift or inaccurate rounding.
2. **Jetpack Compose Performance & Recomposition**: Audit Composable functions (PortfolioHeaderCard, HoldingsTable, TransactionsTable, VaralakshmiDashboardScreen) for unnecessary recompositions, state hoisting hygiene, stable keys in lists, and efficient memory usage.
3. **Security & Networking**: Review network layer (HttpURLConnection, URL parsing, cleartext traffic allowance, timeout handling, thread safety with Dispatchers.IO) to ensure robust exception handling and protect against malformed payloads.
4. **Android & Kotlin Architecture**: Assess adherence to modern Android architecture best practices, lifecycle-aware coroutines, resource management, and UI state encapsulation.

### R2. Forensic Audit Report
Compile all findings into a structured markdown document (`AUDIT_REPORT.md`) in the project root:
- Categorize issues by severity: **Critical**, **High**, **Medium**, **Low/Suggestion**.
- Include exact file paths, line references, risk impact analysis, and remediation strategies.

### R3. Implementation of High-Priority Remediations
Refactor the codebase to address all identified **Critical** and **High** severity issues:
- Fix any potential crash hazards, coroutine leaks, or calculation inaccuracies.
- Optimize Compose recomposition triggers and list rendering performance.
- Maintain existing visual design, ticker table columns (`TICKER`, `% UP/DN`, `VALUE`, `X`), and recent transactions history.

### R4. Verification & APK Rebuild
Ensure the refactored project compiles cleanly without warnings or build errors, execute `./gradlew assembleRelease`, and produce an updated release APK (`varalakshmi-portfolio.apk`).

## Acceptance Criteria

### Audit Deliverable
- [ ] `AUDIT_REPORT.md` exists in `/home/ganapathiraj/Code/Stock Research/Unified UI/app` detailing findings across Security, Performance, Calculation Precision, and Architecture.
- [ ] Every finding has an assigned severity, code reference, and proposed fix.

### Codebase Remediation
- [ ] All Critical and High severity findings identified in the audit are resolved in the source code.
- [ ] No regression in financial calculations (NAV = ₹109,268.80 with default snapshot, consistent PnL tracking).
- [ ] Holdings table retains ticker %up/down, value, and X multiplier with interactive exit capabilities.
- [ ] Transactions table retains chronological fill history with date, side, ticker, price, and difference.

### Build Verification
- [ ] `./gradlew assembleRelease` exits with code 0.
- [ ] Updated `varalakshmi-portfolio.apk` is generated and passes archive verification.

## 2026-09-21T10:48:45Z

This is a single self-contained fix; keep it small and focused.

Enhance the Varalakshmi Portfolio Android application to display today's NAV value change alongside total portfolio value in the header card, and add a dedicated table below the individual tickers table showing each ticker's current price, today's price change (₹ and %), and today's total value change.

Working directory: `/home/ganapathiraj/Code/Stock Research/Unified UI/app`
Integrity mode: development

## Requirements

### R1. Portfolio Header Today's Value Change
Display today's portfolio NAV change (in ₹ and %) alongside total portfolio NAV and all-time return in the Portfolio Header Card. Ensure the daily change cleanly reflects the sum of today's price changes across all active positions without calculation drift.

### R2. Today's Ticker Changes Table (Below Individual Tickers)
Add a dedicated table directly below the "INDIVIDUAL TICKERS" table displaying each holding's daily market movements:
- Ticker Symbol
- Current Price (LTP)
- Today's Price Change (₹ difference and % change)
- Today's Total Value Change (Quantity × Today's Price Change in ₹)

### R3. Data Integration & Model Extension
Extend `VaralakshmiModels.kt` and `VaralakshmiRepository.kt` with fields for today's price change and today's value change (with previous close / entry price references), ensuring robust calculations in both live Cloudflare HTTPS sync and offline snapshot fallback modes.

### R4. UI/UX Consistency & Preservation
Maintain existing functionality and styling:
- Preserve the 5-column aligned holdings table (`TICKER`, `% UP/DN`, `VALUE`, `X`, `EXIT`).
- Preserve the live ticking seconds clock and sync status card.
- Preserve the recent transactions table and confirmation modal.
- Ensure all text and columns are cleanly aligned without wrapping on mobile screens.

### R5. Test Verification & Release Build
Update unit tests to verify today's portfolio value change calculations and table data mapping, execute `./gradlew testDebugUnitTest`, and build an updated release APK via `./gradlew assembleRelease`.

## Acceptance Criteria

### Header Presentation
- [ ] Portfolio Header Card displays today's NAV change with value (₹) and percentage (%) side-by-side with total P&L.
- [ ] Color-coding matches market direction (green for positive daily change, red for negative).

### Today's Changes Table
- [ ] Rendered directly below Individual Tickers and above Recent Transactions.
- [ ] Contains columns: `TICKER`, `PRICE`, `TODAY CHG (%)`, and `TODAY VALUE (₹)`.
- [ ] Ticker symbols, prices, and changes fit comfortably on mobile screen widths without clipping or misaligned borders.

### Verification & Deliverables
- [ ] All unit tests pass cleanly via `./gradlew testDebugUnitTest` with exit code 0.
- [ ] `./gradlew assembleRelease` completes with exit code 0, generating `varalakshmi-portfolio.apk`.
- [ ] Release APK is updated on the repository and release asset on GitHub.

## 2026-09-21T10:49:58Z

Additional requirement from the user:
"also add a link under settings that can be used to download new apks for update"

Please ensure that inside the Server Settings Dialog (in VaralakshmiDashboardScreen.kt), an "App Updates & Releases" section is included with clickable buttons/links using LocalUriHandler.current.openUri(...) to:
1. Download latest APK directly: `https://github.com/Ganapathiraj-A/varalakshmi-portfolio-app/releases/latest/download/varalakshmi-portfolio.apk` (or the direct release tag URL)
2. View all GitHub Releases: `https://github.com/Ganapathiraj-A/varalakshmi-portfolio-app/releases`

Please incorporate this seamlessly alongside R1-R5.

## 2026-09-21T11:08:42Z

CRITICAL BUG REPORTED BY USER:
"When i click refresh it shows latest data but when closing and open it shows 9:26 data. It should show latest data got from server during last refresh"

Root Causes:
1. `VaralakshmiRepository` stores cached data only in-memory variables (`cachedSummary`, `cachedPositions`, `cachedTransactions`), which are lost when the app process exits.
2. `VaralakshmiViewModel` does not have an `init { refresh() }` block, so it never automatically syncs with the server when the app is launched.

Remediation Required:
1. Implement persistent local caching in `VaralakshmiRepository` (e.g. `portfolio_cache.json` in `context.filesDir` or `SharedPreferences`):
   - Provide a static `VaralakshmiRepository.initialize(cacheDir: File)` called in `MainActivity.onCreate()` or use application context.
   - On startup, if persistent cache exists, load `cachedSummary`, `cachedPositions`, and `cachedTransactions` from disk so the app immediately starts with the latest refreshed data.
   - On successful `refreshData()`, save the fetched data to disk.
2. In `VaralakshmiViewModel`, add `init { refresh() }` so that whenever the ViewModel starts, it immediately and automatically syncs live data from the server in the background.

Please implement this fix immediately, ensure unit tests pass, and rebuild the release APK.

## 2026-09-23T19:57:18Z

This is a single self-contained fix; keep it small and focused.

Create and release a standalone Android application dedicated exclusively to Top Stock Recommendations and Recommendation History (with interactive 2-month price trajectory charts and track record filtering), build the production release APK, and publish a release with a direct public download URL.

Working directory: /home/ganapathiraj/Code/Stock Research/Unified UI/recommendations-app
Integrity mode: development

## Requirements

### R1. Standalone Recommendations & History Android Application
- Scaffold a clean, modern Android application using Jetpack Compose and Material 3 in a new dedicated directory (`/home/ganapathiraj/Code/Stock Research/Unified UI/recommendations-app`).
- The application UI must be focused solely on stock recommendations:
  1. **Top 5 Stock Recommendations Table**: Rank, Symbol (full visibility, no truncation), Live/Entry Price, Momentum Score, and 2-Month Chart inspection pill.
  2. **Interactive 2-Month Chart Modal/Sheet**: Opens immediately on tapping any recommendation, showing high/low levels, 2-month % return, interactive canvas curve with touch scrubber tooltip.
  3. **Recommendation History & Track Record Section**: Full 3-month performance track record, win rate metric card (e.g. 75.0% Win Rate), quick filter pills (All, Profitable, Losses, Active), and detailed trade cards (entry date, exit date, holding period, return %, exit reason).
- Omit all portfolio tracking clutter (no NAV cards, no cash balances, no transaction ledger, no manual buy/sell order inputs).

### R2. Data Repository & Seamless Offline/Online Sync
- Connect to the Unified UI server endpoints:
  - `/api/live-trading/recommendations?limit=5`
  - `/api/live-trading/recommendations/history`
- Include robust offline cache and authentic default seeds so the app runs smoothly with rich data even without an active network connection.

### R3. Automated Testing & Verification
- Unit test suite covering:
  - Recommendation parsing and sorting.
  - History calculations (win rate, total trades, average return, best return, max drawdown/loss).
  - Filtering logic (All, Profitable, Losses, Active).
  - 2-month chart bounds and coordinate mapping.
- Programmatic verification: `./gradlew testDebugUnitTest` must pass with 0 errors.

### R4. Production Build & Release Publication
- Configure release signing and build `recommendations-app.apk` via `./gradlew assembleRelease`.
- Publish the release to GitHub via `gh release create` (or git tag) and provide the exact public download URL to the user.

## Acceptance Criteria

### Functionality & UI
- [ ] Dedicated standalone recommendations app runs cleanly without portfolio/account clutter.
- [ ] Long ticker symbols render with full width without ellipsis truncation.
- [ ] Tapping any recommendation opens the interactive 2-month line chart with touch inspection.
- [ ] Recommendation history displays win rate summary and working filter tabs.
- [ ] App launches and displays authentic recommendations and history offline or online.

### Build & Release Verification
- [ ] `./gradlew testDebugUnitTest` passes with exit code 0.
- [ ] `./gradlew assembleRelease` completes with exit code 0 and produces `recommendations-app.apk`.
- [ ] GitHub release created and direct download URL verified and shared with the user.

## 2026-09-25T07:35:22Z

This is a single self-contained fix; keep it small and focused.

Reconcile the daily change calculations in the Varalakshmi Portfolio Android application so that individual ticker changes strictly tally with the portfolio header's total today's value change, and ensure positions bought today use their entry execution price as the daily baseline.

Working directory: /home/ganapathiraj/Code/Stock Research/Unified UI/app
Integrity mode: development

## Requirements

### R1. Dynamic Reference Price for Today's Buy Transactions
- In `PositionItem` (in both `data/VaralakshmiModels.kt` and `model/VaralakshmiModels.kt`) and `VaralakshmiRepository`:
  - When calculating `todayPriceChange`, `todayPriceChangePct`, and `todayValueChange`:
    - Detect if the position was bought today (i.e. `entryDate` starts with today's calendar date `yyyy-MM-dd` in local time).
    - If bought today, use `entryPrice` as the baseline reference price (since the investor did not own the security prior to today's entry).
    - If bought on a previous trading day, use `previousClose` as the baseline reference price (falling back to `entryPrice` if `previousClose <= 0.0`).
  - Ensure `referencePrice` property accurately reflects this decision so tooltips and row subtitles ("Ref: ₹...") display the true baseline.

### R2. Strict Mathematical Equivalence between Table Rows and Header Summary
- In `VaralakshmiRepository` (both in `syncWithServer` and in cache hydration `applyCachedJsonState` / `loadFromDisk`):
  - Ensure `summary.todayPnl` is always strictly calculated as the exact sum of `todayValueChange` across all active positions in `cachedPositions`:
    `todayPnl = roundPaise(cachedPositions.sumOf { it.todayValueChange })`
  - Remove any logic in `applyCachedJsonState` that could override `todayPnl` with a stale cached summary value from disk when position rows are present.
  - Compute `todayPnlPct` consistently against previous closing NAV (`val prevNav = totalNav - todayPnl`, `todayPnlPct = (todayPnl / prevNav) * 100.0`).
  - In `TodayTickerChangesTable`, add a summary footer row or badge confirming the total daily delta matches the header card exactly.

### R3. Automated Test Suite & Release Verification
- In `app/src/test/java/com/example/varalakshmiportfolio/VaralakshmiPortfolioTest.kt`:
  - Add test asserting that a position bought today (e.g. `entryDate = today`, `entryPrice = 1183.36`, `currentPrice = 1182.55`, `previousClose = 1203.85`) produces:
    - `referencePrice == 1183.36`
    - `todayPriceChange == -0.81`
    - `todayValueChange == roundPaise(quantity * -0.81)` (NOT based on 1203.85).
  - Add test asserting that a position held prior to today (e.g. `entryDate = 2026-09-21`, `previousClose = 692.45`, `currentPrice = 674.85`) uses `previousClose` as `referencePrice`.
  - Add test verifying that `summary.todayPnl` strictly equals `positions.sumOf { it.todayValueChange }`.
  - Run `./gradlew testDebugUnitTest` and ensure all tests pass with exit code 0.
  - Execute `./gradlew assembleRelease` to produce the updated `varalakshmi-portfolio.apk`.

## Acceptance Criteria

### Mathematical Consistency & UI
- [ ] Individual stock `TODAY VALUE (₹)` sum in `TodayTickerChangesTable` strictly equals `TODAY'S VALUE CHANGE` in `PortfolioHeaderCard`.
- [ ] Positions bought today (such as `RAYMOND`) measure today's P&L against entry price (₹1,183.36), avoiding false multi-day gap losses against yesterday's close.
- [ ] Positions bought on earlier dates continue measuring today's P&L against yesterday's official close (`previousClose`).
- [ ] Header `todayPnlPct` is mathematically consistent with `todayPnl / (totalNav - todayPnl) * 100`.

### Build & Verification
- [ ] `./gradlew testDebugUnitTest` completes with 0 errors and all unit tests passing.
- [ ] `./gradlew assembleRelease` completes with exit code 0.
- [ ] `varalakshmi-portfolio.apk` is generated and verified.
