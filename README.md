# Varalakshmi Portfolio Android Application 📱📈

A modern Android dashboard built with **Jetpack Compose** and **Material 3** for monitoring the live execution, telemetry, and portfolio status of the **Varalakshmi Strategy** (`VARALAKSHMI_ALPHA_SCALE_35`).

---

## 🎯 Strategy Overview: Varalakshmi Alpha
- **Engine Type**: Fast Rotation Compounder (Target CAGR 80%+)
- **Profit Target**: `+35%` fast profit recycling
- **Cut Loss Guard**: `-4%` stop protection
- **Trailing Stop**: `-8%` from peak execution price
- **Options Gating**: Dynamic Nifty Put Wall & Market Climate integration
- **Execution Mandate**: Strict Point-in-Time causality, Sell-liberated slot allocation

---

## ✨ Features

### 1. Portfolio Value & NAV Telemetry
- **Total Portfolio Value / NAV**: Real-time aggregated NAV (Invested Value + Cash Balance + Unrealized P&L).
- **Return Badge**: Color-coded percentage and absolute P&L gains (`+9.27% / +₹9,268.80`).
- **Metric Cards**:
  - Deployed Capital
  - Available Cash Balance
  - Realized Profit & Loss
  - Active Strategy Slots in Use

### 2. Individual Tickers Table
- **TICKER**: Active holdings (`STLNETWORK`, `AHCL`, `TBZ`) with allocated share count.
- **% UP/DOWN**: Real-time unrealized PnL percentage pill (`+12.16%`, `+9.90%`, `+5.62%`).
- **VALUE**: Current market value and live market price vs. entry price.
- **X**: 
  - Return multiplier factor (`1.12x`, `1.10x`, `1.06x`).
  - Interactive "✕" square-off button with confirmation modal.
- **Forensic Details**: Tap any row to inspect Entry Price, Peak Price, Trailing Stop level, and Entry Timestamp.

### 3. Recent Transactions Table
- **DATE**: Timestamp of order fills.
- **SIDE**: Color-coded order direction (`BUY` in emerald, `SELL` in crimson).
- **TICKER**: Instrument symbol.
- **PRICE**: Executed fill price and transaction volume.
- **DIFF / P&L**: Profit/Loss difference (mark-to-market difference vs. entry).

### 4. Live Sync & Offline Snapshot
- Pre-seeded with authentic production snapshot data from the local database.
- Integrated HTTP sync engine to fetch real-time updates from the Unified UI API (`/api/live-trading/...`).
- Configurable server settings for local emulator or LAN IP addresses.

---

## 🚀 Download & Installation

Directly download the compiled Android APK:
- [Download varalakshmi-portfolio.apk](https://github.com/Ganapathiraj-A/varalakshmi-portfolio-app/releases/download/v1.0.0/varalakshmi-portfolio.apk)

Or download directly from the repository root:
- [varalakshmi-portfolio.apk](./varalakshmi-portfolio.apk)

---

## 🛠️ Tech Stack & Architecture
- **Language**: Kotlin 2.3+
- **UI Framework**: Jetpack Compose with Material 3 Design
- **Android Target**: Android 16 (API 36), Min SDK 24 (Android 7.0+)
- **Networking**: Kotlin Coroutines + HttpURLConnection
- **Architecture**: MVI / Unidirectional Data Flow with In-Memory Repository
