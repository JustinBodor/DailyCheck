package com.example.dailycheck.notification

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.dailycheck.core.util.RepeatRuleUtils
import com.example.dailycheck.data.model.Task
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 每个任务一个唯一 OneTimeWork：触发后由 Worker 自己排下一次。
 * 这样每日 / 每周 / 每月 / 每 N 天都能用同一套逻辑。
 */
@Singleton
class ReminderScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    fun schedule(task: Task) {
        val minutesOfDay = task.reminderMinutes
        if (minutesOfDay == null || task.id <= 0L) {
            cancel(task.id)
            return
        }

        val triggerAt = RepeatRuleUtils.nextTrigger(task.repeatRule, minutesOfDay, ZonedDateTime.now())
        val delayMillis = (triggerAt.toInstant().toEpochMilli() - System.currentTimeMillis())
            .coerceAtLeast(0L)

        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(ReminderWorker.KEY_TASK_ID to task.id))
            .addTag(tagFor(task.id))
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(uniqueName(task.id), ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel(taskId: Long) {
        if (taskId <= 0L) return
        WorkManager.getInstance(context).cancelUniqueWork(uniqueName(taskId))
    }

    companion object {
        fun uniqueName(taskId: Long): String = "task_reminder_$taskId"
        fun tagFor(taskId: Long): String = "reminder_tag_$taskId"
    }
}
