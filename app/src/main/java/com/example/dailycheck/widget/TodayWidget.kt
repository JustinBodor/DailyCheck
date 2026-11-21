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
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.dailycheck.MainActivity
import com.example.dailycheck.data.repository.TaskRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first

/** 从 Hilt 单例里取数据源（Glance 无法直接用 ViewModel） */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun taskRepository(): TaskRepository
}

/** 2x2 极简小组件：今日待打卡数量 + 鼓励语，点击进首页 */
class TodayWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val pending = loadPendingCount(context)

        provideContent {
            WidgetContent(pending = pending)
        }
    }

    private suspend fun loadPendingCount(context: Context): Int {
        return runCatching {
            val repository = EntryPointAccessors
                .fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
                .taskRepository()
            repository.observeTodayTasks().first().count { !it.isCompletedToday() }
        }.getOrDefault(0)
    }
}

@Composable
private fun WidgetContent(pending: Int) {
    val context = LocalContext.current
    val dark = (context.resources.configuration.uiMode and
        Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    val background = if (dark) Color(0xFF1A1B1F) else Color(0xFFF7F8FA)
    val card = if (dark) Color(0xFF2A2B30) else Color.White
    val primaryText = if (dark) Color(0xFFF2F3F5) else Color(0xFF10131A)
    val secondaryText = if (dark) Color(0xFFA9ABB3) else Color(0xFF6B7280)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(background))
            .padding(10.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(card))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = pending.toString(),
                style = TextStyle(
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(primaryText)
                )
            )
            Text(
                text = "项待打卡",
                style = TextStyle(
                    fontSize = 12.sp,
                    color = ColorProvider(secondaryText)
                )
            )

            Spacer(modifier = GlanceModifier.height(8.dp))

            Text(
                text = encouragement(pending),
                style = TextStyle(
                    fontSize = 11.sp,
                    color = ColorProvider(secondaryText)
                )
            )
        }
    }
}

private fun encouragement(pending: Int): String = when {
    pending == 0 -> "今天全部完成，太棒了"
    pending <= 2 -> "就差一点，加油"
    pending <= 5 -> "一步一步来"
    else -> "慢慢来，先做一件"
}

/** 桌面组件接收器 */
class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWidget()
}
