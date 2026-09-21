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
