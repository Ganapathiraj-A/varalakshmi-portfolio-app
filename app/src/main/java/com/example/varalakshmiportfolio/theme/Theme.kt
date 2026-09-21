package com.example.varalakshmiportfolio.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FinancialDarkColorScheme = darkColorScheme(
    primary = AccentIndigo,
    onPrimary = Color.White,
    primaryContainer = AccentIndigoBg,
    onPrimaryContainer = AccentIndigoLight,
    secondary = ProfitGreen,
    onSecondary = Color.White,
    secondaryContainer = ProfitGreenBg,
    onSecondaryContainer = ProfitGreenLight,
    tertiary = GoldAccent,
    onTertiary = Color.White,
    tertiaryContainer = GoldAccentBg,
    onTertiaryContainer = GoldAccent,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkCard,
    onSurfaceVariant = TextSecondary,
    outline = DarkCardBorder,
    outlineVariant = DarkCardBorder.copy(alpha = 0.5f),
    error = LossRed,
    onError = Color.White,
    errorContainer = LossRedBg,
    onErrorContainer = LossRedLight
)

@Composable
fun VaralakshmiPortfolioTheme(
    darkTheme: Boolean = true, // Default to dark dashboard theme
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = FinancialDarkColorScheme,
        typography = Typography,
        content = content
    )
}
