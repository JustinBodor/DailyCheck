package com.example.dailycheck.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/** 当前是否深色（由设置里的外观模式决定） */
val LocalDarkTheme = staticCompositionLocalOf { false }

/** 全局纯色背景：深色 #1A1B1F，浅色 #F7F8FA */
val AppBackgroundDark = Color(0xFF1A1B1F)
val AppBackgroundLight = Color(0xFFF7F8FA)

/** 全局卡片色 */
val AppCardDark = Color(0xFF2A2B30)
val AppCardLight = Color.White

val AppCardBorderDark = Color.White.copy(alpha = 0.10f)
val AppCardBorderLight = Color.Black.copy(alpha = 0.05f)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF3A4BE0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDE2FF),
    onPrimaryContainer = Color(0xFF0A1240),
    secondary = Color(0xFF4C5A86),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE1E5F5),
    onSecondaryContainer = Color(0xFF121A33),
    tertiary = Color(0xFF9B3F7A),
    onTertiary = Color.White,
    background = AppBackgroundLight,
    onBackground = Color(0xFF10131A),
    surface = AppCardLight,
    onSurface = Color(0xFF10131A),
    surfaceVariant = Color(0xFFE9EDF6),
    onSurfaceVariant = Color(0xFF4B5162),
    outline = Color(0xFF8A90A2),
    outlineVariant = Color(0xFFDDE2ED),
    error = Color(0xFFB3261E),
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFBAC4FF),
    onPrimary = Color(0xFF0C1745),
    primaryContainer = Color(0xFF2C3A8E),
    onPrimaryContainer = Color(0xFFE3E7FF),
    secondary = Color(0xFFC2C9EA),
    onSecondary = Color(0xFF141B33),
    secondaryContainer = Color(0xFF2A3457),
    onSecondaryContainer = Color(0xFFE6EAFF),
    tertiary = Color(0xFFFFB0D6),
    onTertiary = Color(0xFF3A0B25),
    background = AppBackgroundDark,
    onBackground = Color(0xFFF2F3F5),
    surface = AppCardDark,
    onSurface = Color(0xFFF2F3F5),
    surfaceVariant = Color(0xFF34353B),
    onSurfaceVariant = Color(0xFFA9ABB3),
    outline = Color(0xFF7C7F88),
    outlineVariant = Color(0xFF3A3C43),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF3B0907)
)

@Composable
fun DailyCheckTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = DailyCheckTypography,
            content = content
        )
    }
}

/** 纯色背景，无渐变、无光晕、无断层 */
@Composable
fun LiquidBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val base = if (LocalDarkTheme.current) AppBackgroundDark else AppBackgroundLight
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(base)
    ) {
        content()
    }
}
