package com.example.dailycheck.notification

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.dailycheck.MainActivity
import com.example.dailycheck.R
import com.example.dailycheck.data.model.Task
import com.example.dailycheck.permission.NotificationPermissionHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val channelManager: NotificationChannelManager
) {

    fun showReminder(task: Task) {
        channelManager.ensureChannel()
        if (!NotificationPermissionHelper.hasPermission(context)) return

        val body = task.note?.takeIf { it.isNotBlank() } ?: DEFAULT_BODY

        val notification = NotificationCompat.Builder(context, NotificationChannelManager.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_check)
            .setContentTitle(task.name)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentIntent(task.id))
            .build()

        NotificationManagerCompat.from(context).notify(task.id.toInt(), notification)
    }

    /** 前台服务常驻通知：任务名 · 剩余 mm:ss */
    fun buildTimerNotification(
        taskId: Long,
        taskName: String,
        remainingSeconds: Int,
        paused: Boolean
    ): Notification {
        channelManager.ensureChannel()
        val text = if (paused) "已暂停 · 剩余 ${format(remainingSeconds)}"
        else "剩余 ${format(remainingSeconds)}"
        return NotificationCompat.Builder(context, NotificationChannelManager.TIMER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_check)
            .setContentTitle(taskName)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setContentIntent(contentIntent(taskId))
            .build()
    }

    fun notifyTimerFinished(taskName: String, taskId: Long) {
        channelManager.ensureChannel()
        val notification = NotificationCompat.Builder(context, NotificationChannelManager.TIMER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_check)
            .setContentTitle(taskName)
            .setContentText("计时结束，已自动打卡 ✅")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentIntent(taskId))
            .build()
        if (NotificationPermissionHelper.hasPermission(context)) {
            NotificationManagerCompat.from(context).notify(TIMER_NOTIFICATION_ID, notification)
        }
    }

    private fun contentIntent(taskId: Long): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TASK_ID, taskId)
        }
        return PendingIntent.getActivity(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun format(seconds: Int): String {
        val safe = seconds.coerceAtLeast(0)
        return String.format(java.util.Locale.US, "%02d:%02d", safe / 60, safe % 60)
    }

    companion object {
        const val EXTRA_TASK_ID = "com.example.dailycheck.extra.TASK_ID"
        const val TIMER_NOTIFICATION_ID = 0x7A11
        private const val DEFAULT_BODY = "该打卡啦，完成后记得勾选一下 ✨"
    }
}
