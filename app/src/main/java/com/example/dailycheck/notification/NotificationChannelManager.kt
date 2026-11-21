package com.example.dailycheck.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationChannelManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
                    .apply { description = "每日打卡提醒" }
            )
        }

        if (manager.getNotificationChannel(TIMER_CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(TIMER_CHANNEL_ID, TIMER_CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW)
                    .apply {
                        description = "计时型任务倒计时"
                        setShowBadge(false)
                        enableVibration(false)
                    }
            )
        }
    }

    companion object {
        const val CHANNEL_ID = "daily_check_reminders"
        const val TIMER_CHANNEL_ID = "daily_check_timer"
        private const val CHANNEL_NAME = "打卡提醒"
        private const val TIMER_CHANNEL_NAME = "任务计时"
    }
}
