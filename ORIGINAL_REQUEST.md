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


