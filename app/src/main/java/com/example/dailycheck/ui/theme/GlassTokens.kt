package com.example.dailycheck.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 液态玻璃的三档强度。
 * TRANSPARENT：几乎透明，仅留边缘高光
 * TRANSLUCENT：半透明，标准卡片
 * FROSTED：磨砂，最"厚"的一档
 */
enum class GlassLevel {
    TRANSPARENT,
    TRANSLUCENT,
    FROSTED
}

object GlassTokens {

    /** 统一大圆角 */
    val Corner: Dp = 26.dp
    val BorderWidth: Dp = 1.dp

    /** 卡片填充透明度 */
    fun fillAlpha(level: GlassLevel): Float = when (level) {
        GlassLevel.TRANSPARENT -> 0.06f
        GlassLevel.TRANSLUCENT -> 0.12f
        GlassLevel.FROSTED -> 0.20f
    }

    /** 边缘高光透明度（0.2 ~ 0.4） */
    fun borderAlpha(level: GlassLevel): Float = when (level) {
        GlassLevel.TRANSPARENT -> 0.20f
        GlassLevel.TRANSLUCENT -> 0.28f
        GlassLevel.FROSTED -> 0.38f
    }

    fun glowAlpha(level: GlassLevel): Float = when (level) {
        GlassLevel.TRANSPARENT -> 0.05f
        GlassLevel.TRANSLUCENT -> 0.10f
        GlassLevel.FROSTED -> 0.16f
    }

    fun shadowElevation(level: GlassLevel): Dp = when (level) {
        GlassLevel.TRANSPARENT -> 6.dp
        GlassLevel.TRANSLUCENT -> 12.dp
        GlassLevel.FROSTED -> 18.dp
    }

    fun blurRadius(level: GlassLevel): Dp = when (level) {
        GlassLevel.TRANSPARENT -> 0.dp
        GlassLevel.TRANSLUCENT -> 18.dp
        GlassLevel.FROSTED -> 28.dp
    }

    // 深色：深蓝 -> 深紫 的弥散渐变 + 光晕
    val DarkBackground = listOf(
        Color(0xFF070B18),
        Color(0xFF111A3A),
        Color(0xFF1B1338),
        Color(0xFF0A0E1E)
    )
    val DarkGlowPrimary = Color(0xFF4A5BFF)
    val DarkGlowSecondary = Color(0xFFB44CFF)

    // 浅色：极浅蓝灰 -> 极浅紫
    val LightBackground = listOf(
        Color(0xFFF2F5FC),
        Color(0xFFE8EDFA),
        Color(0xFFF4EEFB),
        Color(0xFFFBF9FE)
    )
    val LightGlowPrimary = Color(0xFF9DB2FF)
    val LightGlowSecondary = Color(0xFFC9A8FF)

    /** 卡片顶部的高光色 */
    val CardHighlight = Color(0xFFFFFFFF)
}
