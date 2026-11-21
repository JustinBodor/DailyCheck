package com.example.dailycheck.widget

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.dailycheck.MainActivity
import com.example.dailycheck.data.local.dao.CheckInRecordDao
import com.example.dailycheck.data.model.Task
import com.example.dailycheck.data.repository.TaskRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.first

@EntryPoint
@InstallIn(SingletonComponent::class)
interface DailyCheckWidgetEntryPoint {
    fun taskRepository(): TaskRepository
    fun checkInRecordDao(): CheckInRecordDao
}

private data class WidgetData(
    val pendingCount: Int = 0,
    val tasks: List<Task> = emptyList(),
    val month: YearMonth = YearMonth.now(),
    val checkedCounts: Map<Long, Int> = emptyMap(),
    val dark: Boolean = false
)

/** 4x2：左侧完整月历，右侧标题栏 + 轻量任务条 */
class DailyCheckWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadData(context)
        provideContent {
            WidgetContent(data = data)
        }
    }

    private suspend fun loadData(context: Context): WidgetData = runCatching {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            DailyCheckWidgetEntryPoint::class.java
        )

        val pending = entryPoint.taskRepository()
            .observeTodayTasks()
            .first()
            .filter { !it.isCompletedToday() }

        val month = YearMonth.now()
        val checkedCounts = entryPoint.checkInRecordDao()
            .observeAll()
            .first()
            .asSequence()
            .map { LocalDate.ofEpochDay(it.epochDay) }
            .filter { YearMonth.from(it) == month }
            .groupingBy { it.dayOfMonth.toLong() }
            .eachCount()

        // 只认系统（或 AppCompat 写入的 uiMode）配置
        val dark = (context.resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        WidgetData(
            pendingCount = pending.size,
            tasks = pending.take(3),
            month = month,
            checkedCounts = checkedCounts,
            dark = dark
        )
    }.getOrDefault(WidgetData())
}

@Composable
private fun WidgetContent(data: WidgetData) {
    val dark = data.dark

    val surface = if (dark) {
        Color(0xFF1E1E1E).copy(alpha = 0.92f)
    } else {
        Color.White.copy(alpha = 0.92f)
    }
    val primaryText = if (dark) Color(0xFFF2F3F5) else Color(0xFF10131A)
    val secondaryText = if (dark) Color(0xFFA9ABB3) else Color(0xFF6B7280)
    // 柔和低饱和紫
    val accent = if (dark) Color(0xFFB9AEFF) else Color(0xFF7C6FE0)
    val outsideText = Color.Gray.copy(alpha = 0.30f)
    val capsule = if (dark) {
        Color(0xFF2C2C2E)
    } else {
        Color(0xFFF2F2F2)
    }

    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(28.dp)
            .background(ColorProvider(surface))
            .padding(horizontal = 6.dp, vertical = 6.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧：约 45% 宽度（Glance 只有等分权重，用右栏 padding 微调比例）
        Column(
            modifier = GlanceModifier
                .defaultWeight()
                .fillMaxHeight()
                .padding(start = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MiniCalendar(
                month = data.month,
                checkedCounts = data.checkedCounts,
                today = LocalDate.now(),
                accent = accent,
                primaryText = primaryText,
                secondaryText = secondaryText,
                outsideText = outsideText
            )
        }

        Spacer(modifier = GlanceModifier.width(4.dp))

        // 右侧：约 55% 宽度（左侧留一点 padding 收窄视觉）
        Column(
            modifier = GlanceModifier
                .defaultWeight()
                .fillMaxHeight()
                .padding(start = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 一整块灰色信息卡：标题 + 居中大数字 + 小图标
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .cornerRadius(28.dp)
                    .background(ColorProvider(capsule))
                    .clickable(actionStartActivity<MainActivity>())
            ) {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    // 顶部呼吸空间 + 标题
                    Spacer(modifier = GlanceModifier.height(6.dp))

                    Text(
                        text = if (data.pendingCount == 0) "今日已全清" else "今日任务",
                        style = TextStyle(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(primaryText)
                        )
                    )

                    // 巨大数字：水平 + 垂直居中
                    Box(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .height(84.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = data.pendingCount.toString(),
                            style = TextStyle(
                                fontSize = 64.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorProvider(primaryText)
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniCalendar(
    month: YearMonth,
    checkedCounts: Map<Long, Int>,
    today: LocalDate,
    accent: Color,
    primaryText: Color,
    secondaryText: Color,
    outsideText: Color
) {
    val firstDay = month.atDay(1)
    val leadingBlanks = firstDay.dayOfWeek.value - DayOfWeek.MONDAY.value
    val daysInMonth = month.lengthOfMonth()
    val rows = (leadingBlanks + daysInMonth + 6) / 7

    Column(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "${month.year}/${month.monthValue.toString().padStart(2, '0')}",
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = ColorProvider(primaryText)
            ),
            modifier = GlanceModifier.width(126.dp)
        )

        // 固定间距，紧凑排布
        Spacer(modifier = GlanceModifier.height(2.dp))

        // 星期表头：固定 126dp，与日期/月份同一左起点
        Row(modifier = GlanceModifier.width(126.dp)) {
            listOf("一", "二", "三", "四", "五", "六", "日").forEach { label ->
                Box(
                    modifier = GlanceModifier.width(18.dp).height(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = TextStyle(
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(secondaryText)
                        )
                    )
                }
            }
        }

        Spacer(modifier = GlanceModifier.height(2.dp))

        for (row in 0 until rows) {
            Row(modifier = GlanceModifier.width(126.dp).padding(bottom = 1.dp)) {
                for (col in 0 until 7) {
                    val dayIndex = row * 7 + col - leadingBlanks + 1
                    val date: LocalDate? = when {
                        dayIndex in 1..daysInMonth -> month.atDay(dayIndex)
                        dayIndex < 1 ->
                            month.minusMonths(1).atEndOfMonth().plusDays(dayIndex.toLong())
                        else ->
                            month.plusMonths(1).atDay(dayIndex - daysInMonth)
                    }
                    val isCurrentMonth = dayIndex in 1..daysInMonth
                    val isToday = isCurrentMonth && date == today
                    val checkCount = if (isCurrentMonth) {
                        checkedCounts[dayIndex.toLong()] ?: 0
                    } else {
                        0
                    }

                    // 打卡次数越多，紫色越深（0.22 → 0.65）
                    val cellBg = if (checkCount > 0) {
                        accent.copy(alpha = (0.22f + 0.15f * checkCount).coerceAtMost(0.65f))
                    } else {
                        Color.Transparent
                    }
                    val numberColor = when {
                        checkCount > 0 -> Color.White
                        isCurrentMonth -> secondaryText
                        else -> outsideText
                    }

                    Box(
                        modifier = GlanceModifier
                            .width(18.dp)
                            .height(16.dp)
                            .padding(end = 1.dp)
                            .cornerRadius(4.dp)
                            .background(ColorProvider(cellBg)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = date?.dayOfMonth?.toString() ?: "",
                                style = TextStyle(
                                    fontSize = 9.sp,
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                                    color = ColorProvider(numberColor)
                                )
                            )
                            Spacer(modifier = GlanceModifier.height(1.dp))
                            // 所有日期都预留同样的圆点位，数字才能完全在同一水平线
                            Box(
                                modifier = GlanceModifier
                                    .width(3.dp)
                                    .height(3.dp)
                                    .cornerRadius(2.dp)
                                    .background(
                                        ColorProvider(if (isToday) accent else Color.Transparent)
                                    )
                            ) {}
                        }
                    }
                }
            }
        }
    }
}

/** App 内数据变化后调用：显式刷新所有小组件实例 */
suspend fun refreshTodayWidget(context: Context) {
    runCatching { TodayWidget().updateAll(context) }

    val manager = GlanceAppWidgetManager(context)
    val glanceIds = manager.getGlanceIds(DailyCheckWidget::class.java)
    if (glanceIds.isNotEmpty()) {
        DailyCheckWidget().updateAll(context)
    }
}
