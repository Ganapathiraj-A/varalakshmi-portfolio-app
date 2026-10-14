package com.example.varalakshmiportfolio.ui.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale

object StockLinkHelper {

    /**
     * Builds the Google Finance quote URL for an Indian stock ticker symbol.
     * Most NSE stocks use `<SYMBOL>:NSE` (or if it's already qualified, uses as is).
     */
    fun buildGoogleFinanceUrl(symbol: String): String {
        val cleanSymbol = symbol.trim().uppercase()
        val formattedSymbol = if (cleanSymbol.contains(":")) {
            cleanSymbol
        } else {
            "$cleanSymbol:NSE"
        }
        val encoded = URLEncoder.encode(formattedSymbol, StandardCharsets.UTF_8.toString())
        return "https://www.google.com/finance/quote/$encoded"
    }

    /**
     * Launches the Google Finance chart page in Chrome if installed,
     * otherwise falls back to any available web browser.
     */
    fun openGoogleFinance(context: Context, symbol: String) {
        val url = buildGoogleFinanceUrl(symbol)
        openUrl(context, url)
    }

    /**
     * Launches a targeted search or portal for registrar IPO allotment verification in Chrome / default browser.
     */
    fun openRegistrarAllotmentCheck(context: Context, symbol: String, registrarName: String = "") {
        val cleanSymbol = symbol.trim().uppercase(Locale.US)
        val cleanRegistrar = registrarName.trim()
        val query = if (cleanRegistrar.isNotBlank()) {
            "$cleanSymbol IPO allotment status $cleanRegistrar"
        } else {
            "$cleanSymbol IPO allotment status registrar check"
        }
        val encoded = URLEncoder.encode(query, StandardCharsets.UTF_8.toString())
        val url = "https://www.google.com/search?q=$encoded"
        openUrl(context, url)
    }

    /**
     * Generic helper to launch URLs targeting Google Chrome first with standard browser fallback.
     */
    fun openUrl(context: Context, url: String) {
        val genericIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        // Try specifically targeting Google Chrome first
        val chromeIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            setPackage("com.android.chrome")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(chromeIntent)
            return
        } catch (_: ActivityNotFoundException) {
            // Chrome is not installed, fall through to generic browser intent
        } catch (_: SecurityException) {
            // Permission/visibility restriction, fall through
        } catch (_: Throwable) {
            // Fall through to generic browser intent
        }

        try {
            context.startActivity(genericIntent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "No web browser found to open link", Toast.LENGTH_SHORT).show()
        } catch (e: Throwable) {
            Toast.makeText(context, "Unable to open link: ${e.localizedMessage ?: "Unknown error"}", Toast.LENGTH_SHORT).show()
        }
    }
}
