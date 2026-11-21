package com.example.dailycheck.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dailycheck.ui.theme.LocalDarkTheme

/**
 * 纯圆形 FAB：无任何 Material 阴影，靠底色 + 1dp 边框区分层次。
 */
@Composable
fun AddTaskFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dark = LocalDarkTheme.current
    val container = if (dark) Color(0xFF2A2B30) else Color.White
    val borderColor = if (dark) Color.White.copy(alpha = 0.14f) else Color.White

    Box(
        modifier = modifier
            .size(62.dp)
            .clip(CircleShape)
            .background(container)
            .border(width = 1.dp, color = borderColor, shape = CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "+",
            fontSize = 28.sp,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
