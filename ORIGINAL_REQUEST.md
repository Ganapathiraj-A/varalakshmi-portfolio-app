# Original User Request

## 2026-09-23T10:28:30Z

This is a single self-contained fix; keep it small and focused.

Enhance the Varalakshmi Portfolio Android application to add a dedicated "Top 5 Stock Recommendations" table at the bottom of the dashboard screen, where tapping any recommendation opens an interactive dialog or modal bottom sheet displaying that stock's past 2-month price trajectory line chart with high/low levels and price tooltips.

Working directory: /home/ganapathiraj/Code/Stock Research/Unified UI/app
Integrity mode: development

## Requirements

### R1. Top 5 Stock Recommendations Table
Add a dedicated composable table at the bottom of `VaralakshmiDashboardScreen` (below Recent Transactions):
- Section header: "TOP 5 STOCK RECOMMENDATIONS" with an icon and badge.
- Columns: Rank (`#`), Ticker Symbol (`TICKER`), Recommended Entry/Current Price (`PRICE`), Momentum Score / Alpha Rank (`SCORE`), and Action (`CHART` / tap indicator).
- Visual styling matching the dark trading theme (`Color(0xFF161B22)`, `Color(0xFF0D1117)`, GitHub Dark/OLED styling) with clean border alignment and no horizontal text truncation.
- Tapping anywhere on a recommendation row triggers the 2-month chart viewer.

### R2. Interactive 2-Month Historical Price Chart Modal
When a user taps any recommended stock, display a clean modal dialog or modal bottom sheet:
- Ticker symbol header, current price, 2-month net % change, and 2-month high/low range tags.
- A custom Jetpack Compose Canvas line chart rendering the stock's daily close trajectory over the past 2 months (approx. 45 trading days / 60 calendar days).
- Chart visual elements:
  * Smooth gradient fill below the price curve (green if 2-month return > 0, red/accent if < 0).
  * Horizontal dashed guidelines for 2-month Peak High, Trough Low, and Current Price.
  * Interactive touch gesture / drag scrub support that displays a floating tooltip with date and price at the touched point.
  * Prominent close button ("✕" or "Close").

### R3. Data Integration & Offline Cache Fallback
- Extend `VaralakshmiModels.kt` with a data class for `StockRecommendationItem` (rank, symbol, price, score, targetPrice, stopLossPrice, historical2mPoints).
- In `VaralakshmiRepository.kt`, provide recommendation loading with both live HTTPS sync (fetching from the live API or Cloudflare payload) and a rich default offline fallback snapshot containing realistic 2-month daily price arrays for the top 5 momentum candidates (e.g. `CUPID`, `ADANIPOWER`, `YASHO`, `DEEDEV`, `ARIHANT`).

### R4. Verification, Testing & Release Build
- Add unit tests in `VaralakshmiPortfolioTest.kt` verifying:
  1. Top 5 recommendations parsing and state handling.
  2. 2-month historical data point sorting and min/max calculation.
  3. Recommendation click interaction state updates.
- Verify that `./gradlew testDebugUnitTest` passes with exit code 0.
- Execute `./gradlew assembleRelease` to generate the production APK (`varalakshmi-portfolio.apk`).

## Acceptance Criteria

### UI & UX
- [ ] "TOP 5 STOCK RECOMMENDATIONS" table renders cleanly at the bottom of the dashboard screen.
- [ ] Table cleanly shows Rank 1 to 5, symbol, price, and score without wrapping or clipping on mobile screen widths.
- [ ] Tapping any recommendation opens the 2-month chart modal immediately.
- [ ] Chart displays a clean price line curve with high/low bounds and touch-to-inspect price tooltips.
- [ ] Existing dashboard cards (Header, Individual Tickers, Today's Value Change, Recent Transactions) remain fully functional.

### Testing & Build Verification
- [ ] `./gradlew testDebugUnitTest` completes with exit code 0 and all tests passing.
- [ ] `./gradlew assembleRelease` completes with exit code 0.
- [ ] `varalakshmi-portfolio.apk` is generated in `app/build/outputs/apk/release/` or copied to project root.

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

## 2026-09-28T11:12:09Z

This is a single self-contained fix; keep it small and focused.

Complete, stress-test, and release the swipeable VeeraLakshmi F&O Paper Trading dashboard in the Varalakshmi Portfolio Android application, ensuring seamless horizontal pager navigation, live sync with backend F&O endpoints, offline caching, and automated build verification.

Working directory: /home/ganapathiraj/Code/Stock Research/Unified UI/app
Integrity mode: development

## Requirements

### R1. Swipeable Horizontal Pager & Dual Strategy Tabs
- Wrap the main application content in a 2-page `HorizontalPager` allowing users to swipe horizontally in either direction between:
  * Page 0: **Varalakshmi (Equity Portfolio)**
  * Page 1: **VeeraLakshmi (F&O Paper Trading)**
- Provide a persistent top navigation tab bar displaying:
  * `📈 Varalakshmi (Equity)`
  * `⚡ VeeraLakshmi (F&O)` with a real-time unrealized P&L badge (e.g. `+₹50.7k`).
- Tapping either tab must trigger smooth animated scrolling to that page (`animateScrollToPage`).
- Vertical scrolling within each dashboard must operate independently without blocking horizontal drag gestures.

### R2. VeeraLakshmi F&O Paper Trading Screen
Implement a dedicated `VeeraLakshmiScreen` composable matching the OLED dark trading aesthetic:
1. **Capital & Margin Meter Card**: Displays Allocated Capital, Margin Blocked, Free Cash Buffer, Unrealized P&L, Return on Margin %, and a visual `LinearProgressIndicator` showing margin utilization.
2. **Multi-Tier Instance Selector**: Interactive filter chips for switching between `50L (Quad-Engine)`, `20L (Balanced)`, and `15L (Lean & Agile)` tiers, dynamically updating capital and positions.
3. **Active F&O Positions List**: Displays each open futures/options position with direction badge (`SHORT`/`LONG`), lot count, symbol, entry price, current price, stop loss, margin required, and unrealized P&L.
4. **Risk & Volatility Telemetry**: Displays Circuit Breaker state (Tier 0 Normal), India VIX level/regime, and Daily Step timestamp.
5. **10-Year Benchmark Baseline**: Displays 10-year audited CAGR (34.85%), Sharpe ratio (1.63), Win Rate (64.75%), and 2026 YTD return (+27.66%).

### R3. Dual-Engine Synchronization & Offline Persistence
- Fetch live telemetry from `/api/fno/instances`, `/api/fno/positions?instance={id}`, and `/api/fno/status`.
- Persist state to `veeralakshmi_fno_cache.json` in app storage.
- Include robust offline seed data so that in offline mode or before first network sync, authentic 2026-09-28 positions render immediately.
- Global refresh action (tap or auto-sync) must sync both Varalakshmi and VeeraLakshmi concurrently.

### R4. Automated Testing & Release Compilation
- Include unit tests in `VeeraLakshmiTest.kt` covering JSON parsing, instance switching, margin calculations, and disk cache serialization.
- Verify `./gradlew testDebugUnitTest` passes with exit code 0.
- Bump app version to `v1.9.3` (`versionCode = 21`) in `build.gradle.kts` and dashboard UI.
- Execute `./gradlew assembleRelease` to compile production APK (`varalakshmi-portfolio.apk`).

## Acceptance Criteria

### UI & Navigation
- [ ] Top navigation bar shows "Varalakshmi (Equity)" and "VeeraLakshmi (F&O)" tabs with live P&L badge.
- [ ] Swiping horizontally between Page 0 and Page 1 works smoothly with native touch physics.
- [ ] Tapping either tab animates smoothly to that screen.
- [ ] Tapping 50L, 20L, or 15L chips updates active positions and capital metrics immediately.
- [ ] All metrics and positions format monetary values cleanly with Indian rupee (`₹`) symbols and no text clipping.

### Data & Architecture
- [ ] VeeraLakshmi data fetches from live `/api/fno/` endpoints when connected.
- [ ] Offline fallback loads seamlessly from cache or seed data without blank screens.
- [ ] Manual refresh and periodic auto-sync refresh both equity and F&O state.

### Testing & Release Verification
- [ ] `./gradlew testDebugUnitTest` completes with exit code 0 and all tests passing.
- [ ] `./gradlew assembleRelease` completes with exit code 0.
- [ ] Production APK `varalakshmi-portfolio.apk` is generated and copied to project root.


## 2026-10-02T05:34:28Z

This is a single self-contained fix; keep it small and focused.

Enhance the Varalakshmi Portfolio Android application by adding a dedicated "Daily Returns" section to the main Varalakshmi Equity Portfolio dashboard tab (Tab 0), placed directly below Recent Transactions and above Stock Recommendations, displaying historical session-by-session performance.

Working directory: /home/ganapathiraj/Code/Stock Research/Unified UI/app
Integrity mode: development

## Requirements

### R1. Daily Returns Table Composable
Add a dedicated composable table (`DailyReturnsTable`) to `VaralakshmiDashboardScreen.kt` in the main equity dashboard tab (Tab 0):
- Section header: "DAILY RETURNS" with an appropriate icon (e.g., `Icons.Filled.ShowChart` or `Icons.Filled.CalendarToday`) and badge indicating recent session count.
- Columns:
  * `DATE`: Session date (e.g., "YYYY-MM-DD" or formatted date).
  * `DAILY P&L`: Absolute net profit or loss for the day in ₹ (e.g., `+₹12,450.00` or `-₹4,200.00`).
  * `RETURN %`: Percentage change for the day (e.g., `+1.24%` or `-0.42%`).
  * `STATUS`: Compact status badge (e.g., `WIN` in green, `LOSS` in red, or `FLAT` in neutral).
- Styling consistent with the application's dark trading theme (`DarkCardBg`, `DarkCardBorder`, `TextPrimary`, `TextSecondary`, `ProfitGreen`, `LossRed`, clean alignment, and no horizontal text clipping).

### R2. Data Model & State Integration
- In `VaralakshmiModels.kt`, declare a data class `DailyReturnItem` representing a daily performance record:
  * `date: String`
  * `dailyPnl: Double`
  * `returnPct: Double`
  * `totalNav: Double` (or closing equity)
  * `tradeCount: Int` (optional / default 0)
  * `status: String` (derived or explicit: "WIN", "LOSS", "FLAT")
  * Helper getters: `formattedPnl`, `formattedReturnPct`, `isProfit`
- In `VaralakshmiUiState`, add `dailyReturns: List<DailyReturnItem> = emptyList()`.
- In `VaralakshmiViewModel.kt` and `VaralakshmiRepository.kt`, populate `dailyReturns` with authentic historical equity performance records, supporting both offline fallback seed data and live network sync if available.

### R3. Verification Test Suite
- Add unit tests in `app/src/test/java/com/example/varalakshmiportfolio/` validating:
  * Model parsing and formatting of `DailyReturnItem` (positive, negative, and zero values).
  * Inclusion of `dailyReturns` in `VaralakshmiUiState` upon repository load.
  * Correct sign formatting and status attribution.

## Acceptance Criteria

### UI Presentation & Alignment
- [ ] The "DAILY RETURNS" table renders on Tab 0 (Varalakshmi Portfolio) below Recent Transactions without layout overflows or text truncation.
- [ ] Positive P&L and return percentages display in `ProfitGreen` with a leading `+` sign.
- [ ] Negative P&L and return percentages display in `LossRed` with a leading `-` sign.
- [ ] Zero / flat days render cleanly in muted/neutral styling.

### Data & Architecture
- [ ] `VaralakshmiUiState` exposes `dailyReturns: List<DailyReturnItem>`.
- [ ] `VaralakshmiRepository` provides realistic daily return records in the offline fallback state.
- [ ] All existing app unit tests pass cleanly via `./gradlew testDebugUnitTest`.
- [ ] New unit tests verifying daily returns state and formatting pass cleanly.

## 2026-10-15T08:44:01Z

This is a single self-contained fix; keep it small and focused.

Implement an always-on floating horizontal live stock screener overlay in the Varalakshmi Portfolio Android application that renders above other apps at ~1/4 above screen bottom, displaying today's total portfolio P&L and a continuous auto-scrolling marquee of individual stock % changes, with vertical dragging support, tap-to-open app navigation, and an in-app Settings toggle with overlay permission management.

Working directory: /home/ganapathiraj/Code/Stock Research/Unified UI/app
Integrity mode: development

## Requirements

### R1. Floating System Overlay Service (`FloatingScreenerService`)
- Android foreground `Service` using `WindowManager` with `WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY`.
- Layout positioned horizontally across the device screen, default positioned at approximately 1/4 (25%) above the bottom of the screen.
- Supports vertical drag gestures so the user can easily reposition the screener up or down to avoid obstructing keyboard, navigation bars, or other app controls.
- Content elements:
  1. **Portfolio Summary Badge (Left/Fixed)**: Today's total P&L in ₹ and % (e.g. `+₹2,965.67 (+2.1%)` in green, or negative in red) with compact styling.
  2. **Continuous Auto-Scrolling Marquee (Right/Flex)**: Smoothly animated horizontal ticker loop showcasing all active stocks with ticker symbols and their today's `%` change (e.g. `PCJEWELLER +4.2%  •  CUPID -1.1%  •  TATAMOTORS +0.8%`).
- Tap-to-open interaction: Tapping anywhere on the overlay launches `MainActivity` to bring the portfolio app directly to the foreground.
- Compact translucent OLED-dark styling (`#E60D1117` background, subtle rounded border) ensuring visibility without completely blocking underlying content.

### R2. In-App Settings / Dashboard Toggle & Permission Handling
- Add a dedicated "Always-On Live Screener" card/toggle switch in the app (in `VaralakshmiDashboardScreen` or Settings).
- Handle `Settings.canDrawOverlays(context)` checks:
  - If overlay permission is NOT granted when user toggles ON: show an explanatory dialog or snackbar and open the system `Settings.ACTION_MANAGE_OVERLAY_PERMISSION` intent.
  - When granted, start `FloatingScreenerService`.
  - When toggled OFF, stop `FloatingScreenerService` and remove overlay from `WindowManager`.
- Persist user toggle preference in `SharedPreferences` / repository state so the user's preference is remembered across app sessions.
- Declare `android.permission.SYSTEM_ALERT_WINDOW` and `android.permission.FOREGROUND_SERVICE` (and `FOREGROUND_SERVICE_SPECIAL_USE` / `SYSTEM_EXEMPTED` as required for Android 14+) in `AndroidManifest.xml`.

### R3. Dynamic Data Updates & Battery-Efficient Lifecycle
- Observe live portfolio changes: when repository updates positions and summary, broadcast or push updated numbers to the floating view so it always reflects live market updates.
- Stop ticker animation and timer when screen is off (`ACTION_SCREEN_OFF`) to conserve battery.

### R4. Build Verification & Unit Testing
- Unit tests verifying:
  - Permission check and intent generation logic.
  - Floating screener state preferences persistence.
  - Ticker text generation and formatting from portfolio position items.
- Programmatic verification: `./gradlew testDebugUnitTest` completes with 0 errors.
- Assemble release: `./gradlew assembleRelease` completes with 0 errors.

## Acceptance Criteria

### Functionality & Overlay UX
- [ ] Floating live screener renders horizontally above other apps at ~1/4 height above the screen bottom.
- [ ] Screener displays today's total portfolio P&L (₹ and %) and continuous auto-scrolling individual stock % changes.
- [ ] User can drag the floating screener vertically to reposition it anywhere on screen.
- [ ] Tapping the floating screener immediately brings `MainActivity` to the foreground.
- [ ] In-app toggle enables or disables the screener, directing user to Android overlay settings if permission is not yet granted.
- [ ] Preference is saved and restored across app restarts.

### Stability & Build
- [ ] `./gradlew testDebugUnitTest` passes with exit code 0.
- [ ] `./gradlew assembleRelease` passes with exit code 0 and produces `varalakshmi-portfolio.apk`.

