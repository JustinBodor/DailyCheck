@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.dailycheck.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dailycheck.ui.theme.LocalDarkTheme

private val ITEM_HEIGHT = 44.dp
private val VISIBLE_ITEMS = 5

/**
 * iOS 风格滚轮时间选择器：两个可拖拽的 LazyColumn + 上下渐变遮罩 + 中央高亮框。
 * 不使用任何 Material TimePicker / 软键盘。
 */
@Composable
fun TimeWheelPicker(
    title: String,
    initialMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val dark = LocalDarkTheme.current
    val sheetColor = if (dark) Color(0xFF1F2024) else Color(0xFFF7F8FA)

    var hour by remember { mutableIntStateOf((initialMinutes / 60).coerceIn(0, 23)) }
    var minute by remember { mutableIntStateOf((initialMinutes % 60).coerceIn(0, 59)) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = sheetColor,
        contentColor = MaterialTheme.colorScheme.onSurface,
        scrimColor = Color.Black.copy(alpha = 0.55f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                WheelColumn(
                    values = (0..23).toList(),
                    selected = hour,
                    fadeColor = sheetColor,
                    width = 92.dp
                ) { hour = it }

                Text(
                    text = ":",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 10.dp)
                )

                WheelColumn(
                    values = (0..59).toList(),
                    selected = minute,
                    fadeColor = sheetColor,
                    width = 92.dp
                ) { minute = it }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) { Text("取消") }
                TextButton(onClick = { onConfirm(hour * 60 + minute) }) { Text("确定") }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

@Composable
private fun WheelColumn(
    values: List<Int>,
    selected: Int,
    fadeColor: Color,
    width: Dp,
    onSelect: (Int) -> Unit
) {
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = values.indexOf(selected).coerceAtLeast(0)
    )
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    val currentIndex by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex.coerceIn(0, values.lastIndex)
        }
    }

    LaunchedEffect(currentIndex) {
        val value = values[currentIndex]
        if (value != selected) onSelect(value)
    }

    Box(
        modifier = Modifier
            .width(width)
            .height(ITEM_HEIGHT * VISIBLE_ITEMS)
    ) {
        // 中央高亮框（在列表下层）
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(ITEM_HEIGHT)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f))
        )

        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            contentPadding = PaddingValues(vertical = ITEM_HEIGHT * ((VISIBLE_ITEMS - 1) / 2)),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(values) { index, value ->
                val isSelected = index == currentIndex
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ITEM_HEIGHT),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = String.format(java.util.Locale.US, "%02d", value),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                        },
                        fontSize = if (isSelected) 27.sp else 21.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        // 上下渐变遮罩
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(ITEM_HEIGHT * 2)
                .background(
                    Brush.verticalGradient(
                        listOf(fadeColor, fadeColor.copy(alpha = 0f))
                    )
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(ITEM_HEIGHT * 2)
                .background(
                    Brush.verticalGradient(
                        listOf(fadeColor.copy(alpha = 0f), fadeColor)
                    )
                )
        )
    }
}
