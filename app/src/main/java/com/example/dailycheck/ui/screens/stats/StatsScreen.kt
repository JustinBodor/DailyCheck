@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.dailycheck.ui.screens.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.dailycheck.ui.theme.AppCardBorderDark
import com.example.dailycheck.ui.theme.AppCardBorderLight
import com.example.dailycheck.ui.theme.AppCardDark
import com.example.dailycheck.ui.theme.LocalDarkTheme
import kotlin.math.cos
import kotlin.math.sin
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/** 与设置页一致的抽屉底色 */
private val StatsSheetDark = Color(0xFF080C18)
private val StatsSheetLight = Color(0xFFF6F8FE)

/** 统计抽屉：核心数字 + 当前自然月日历。 */
@Composable
fun StatsScreen(
    onDismiss: () -> Unit,
    viewModel: StatsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val dark = LocalDarkTheme.current
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = if (dark) StatsSheetDark else StatsSheetLight,
        contentColor = MaterialTheme.colorScheme.onSurface,
        scrimColor = Color.Black.copy(alpha = 0.80f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.90f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "统计",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onDismiss) { Text("完成") }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    label = "总打卡次数",
                    value = state.totalCheckIns.toString(),
                    unit = "次",
                    dark = dark
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    label = "当前连续",
                    value = state.currentStreak.toString(),
                    unit = "天",
                    dark = dark
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            MonthCalendarCard(state = state, dark = dark)

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    unit: String,
    dark: Boolean
) {
    val shape = RoundedCornerShape(24.dp)
    val shadowModifier = if (dark) {
        Modifier
    } else {
        Modifier.shadow(
            elevation = 12.dp,
            shape = shape,
            clip = false,
            ambientColor = Color.Black.copy(alpha = 0.04f),
            spotColor = Color.Black.copy(alpha = 0.08f)
        )
    }

    Column(
        modifier = modifier
            .then(shadowModifier)
            .clip(shape)
            .background(if (dark) AppCardDark else Color.White)
            .border(
                width = 1.dp,
                color = if (dark) AppCardBorderDark else AppCardBorderLight,
                shape = shape
            )
            .padding(20.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = unit,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }
    }
}

@Composable
private fun MonthCalendarCard(state: StatsUiState, dark: Boolean) {
    val shape = RoundedCornerShape(24.dp)
    val shadowModifier = if (dark) {
        Modifier
    } else {
        Modifier.shadow(
            elevation = 12.dp,
            shape = shape,
            clip = false,
            ambientColor = Color.Black.copy(alpha = 0.04f),
            spotColor = Color.Black.copy(alpha = 0.08f)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .then(shadowModifier)
            .clip(shape)
            .background(if (dark) AppCardDark else Color.White)
            .border(
                width = 1.dp,
                color = if (dark) AppCardBorderDark else AppCardBorderLight,
                shape = shape
            )
            .padding(20.dp)
    ) {
        MonthCalendarView(
            month = YearMonth.from(state.endDate),
            counts = state.dailyCounts,
            taskColors = state.dailyTaskColors,
            taskIcons = state.dailyTaskIcons,
            taskNames = state.dailyTaskNames,
            today = state.endDate
        )
    }
}

/**
 * 当前自然月日历：
 * 1 个任务 → 整格任务色；2 个 → 左右对半；3-4 个 → 底色调 + 象限小圆点。
 * 点击格子弹出“图标 + 颜色”的极简菜单。
 */
@Composable
fun MonthCalendarView(
    month: YearMonth,
    counts: Map<LocalDate, Int>,
    taskColors: Map<LocalDate, List<Long>>,
    taskIcons: Map<LocalDate, List<String>>,
    taskNames: Map<LocalDate, List<String>>,
    today: LocalDate
) {
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val emptyColor = onSurface.copy(alpha = 0.06f)
    val cardColor = if (LocalDarkTheme.current) AppCardDark else Color.White

    val weekdayLabels = listOf("一", "二", "三", "四", "五", "六", "日")
    val firstDay = month.atDay(1)
    val daysInMonth = month.lengthOfMonth()
    val leadingBlanks = firstDay.dayOfWeek.value - DayOfWeek.MONDAY.value
    val totalCells = leadingBlanks + daysInMonth
    val rows = (totalCells + 6) / 7

    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var selectedTaskIndex by remember { mutableStateOf<Int?>(null) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "打卡日历",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = onSurface
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${month.year} 年 ${month.monthValue} 月",
                style = MaterialTheme.typography.labelMedium,
                color = onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            weekdayLabels.forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 11.sp,
                    color = onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            for (row in 0 until rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (col in 0 until 7) {
                        val cellIndex = row * 7 + col
                        val dayOfMonth = cellIndex - leadingBlanks + 1

                        if (dayOfMonth !in 1..daysInMonth) {
                            Spacer(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                            )
                        } else {
                            val date = month.atDay(dayOfMonth)
                            val colors = taskColors[date].orEmpty()
                            val icons = taskIcons[date].orEmpty()
                            val names = taskNames[date].orEmpty()
                            val isToday = date == today
                            val isFuture = date.isAfter(today)
                            val isSelected = selectedDate == date

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        width = when {
                                            isSelected -> 2.dp
                                            isToday -> 1.dp
                                            else -> 0.dp
                                        },
                                        color = when {
                                            isSelected -> primary
                                            isToday -> primary.copy(alpha = 0.5f)
                                            else -> Color.Transparent
                                        },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        selectedDate = if (isSelected) null else date
                                        selectedTaskIndex = null
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                // 单色深浅热力：按当天打卡数量取主题色 Alpha
                                val levelAlpha = when {
                                    colors.size >= 4 -> 1.0f
                                    colors.size in 2..3 -> 0.60f
                                    colors.size == 1 -> 0.30f
                                    else -> 0f
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight()
                                        .background(
                                            when {
                                                isFuture -> onSurface.copy(alpha = 0.03f)
                                                levelAlpha == 0f -> emptyColor
                                                else -> primary.copy(alpha = levelAlpha)
                                            }
                                        )
                                )

                                // 日期数字始终居中，深色底白字、浅色底黑字
                                Text(
                                    text = dayOfMonth.toString(),
                                    fontSize = 12.sp,
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                                    color = when {
                                        isFuture -> onSurfaceVariant.copy(alpha = 0.35f)
                                        levelAlpha >= 0.6f -> Color.White
                                        levelAlpha > 0f -> onSurface
                                        else -> onSurfaceVariant.copy(alpha = 0.85f)
                                    }
                                )

                                // 极简浮窗：图标 + 任务名（最多 8 字）
                                DropdownMenu(
                                    expanded = isSelected && colors.isNotEmpty(),
                                    onDismissRequest = {
                                        selectedDate = null
                                        selectedTaskIndex = null
                                    },
                                    containerColor = cardColor,
                                    shape = RoundedCornerShape(14.dp),
                                    tonalElevation = 0.dp,
                                    shadowElevation = 8.dp
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        colors.indices.forEach { index ->
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(30.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(Color(colors[index]))
                                                        .clickable {
                                                            selectedTaskIndex =
                                                                if (selectedTaskIndex == index) null else index
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = icons.getOrElse(index) { "•" },
                                                        fontSize = 15.sp
                                                    )
                                                }
                                                // 名字显示在图标下方
                                                if (selectedTaskIndex == index) {
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = truncateTaskName(
                                                            names.getOrElse(index) { "" }
                                                        ),
                                                        fontSize = 9.sp,
                                                        color = onSurface,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "少",
                style = MaterialTheme.typography.labelSmall,
                color = onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(6.dp))
            listOf(0f, 0.30f, 0.60f, 1.0f).forEach { alpha ->
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (alpha == 0f) emptyColor else primary.copy(alpha = alpha))
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = "多",
                style = MaterialTheme.typography.labelSmall,
                color = onSurfaceVariant
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "本月 ${counts.values.sum()} 次",
                style = MaterialTheme.typography.labelSmall,
                color = onSurfaceVariant,
                textAlign = TextAlign.End
            )
        }
    }
}

/** 名称最多保留 8 个字符，超出用省略号 */
private fun truncateTaskName(name: String): String =
    if (name.length <= 8) name else name.take(8) + "..."
