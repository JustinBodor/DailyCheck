package com.example.dailycheck.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * DeepSeek 风格卡片：
 * 深色：深灰实底 #2A2B30 + 1dp 微弱白边，无模糊、无彩色光晕。
 * 浅色：纯白实底 + 极淡灰弥散阴影 + 1dp 白边。
 */
@Composable
fun Modifier.liquidGlass(
    level: GlassLevel = GlassLevel.FROSTED,
    shape: Shape = RoundedCornerShape(GlassTokens.Corner),
    emphasized: Boolean = false,
    glowColor: Color = Color.White
): Modifier {
    val dark = LocalDarkTheme.current

    val container = if (dark) Color(0xFF2A2B30) else Color.White
    val borderColor = when {
        emphasized && dark -> Color.White.copy(alpha = 0.28f)
        emphasized -> Color(0xFF3A4BE0).copy(alpha = 0.55f)
        dark -> Color.White.copy(alpha = 0.10f)
        else -> Color.Black.copy(alpha = 0.05f)
    }

    val shadowModifier = if (dark) {
        Modifier
    } else {
        Modifier.shadow(
            elevation = 16.dp,
            shape = shape,
            clip = false,
            ambientColor = Color.Black.copy(alpha = 0.08f),
            spotColor = Color.Black.copy(alpha = 0.15f)
        )
    }

    return this
        .then(shadowModifier)
        .clip(shape)
        .background(container, shape)
        .border(width = 1.dp, color = borderColor, shape = shape)
}
