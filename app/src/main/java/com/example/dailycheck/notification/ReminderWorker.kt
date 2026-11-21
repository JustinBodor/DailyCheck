package com.example.dailycheck.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.dailycheck.data.repository.TaskRepository
import com.example.dailycheck.data.timer.TimerStateStore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.ZonedDateTime
import kotlinx.coroutines.flow.first

@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val taskRepository: TaskRepository,
    private val notificationHelper: NotificationHelper,
    private val reminderScheduler: ReminderScheduler,
    private val timerStateStore: TimerStateStore
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val taskId = inputData.getLong(KEY_TASK_ID, -1L)
        if (taskId <= 0L) return Result.success()

        val task = taskRepository.getTask(taskId) ?: return Result.success()
        if (task.reminderMinutes == null) return Result.success()

        val now = ZonedDateTime.now()
        val currentMinutes = now.hour * 60 + now.minute
        val quiet = timerStateStore.quietHours.first()

        // 免打扰（默认 23:00–07:00，设置里可改可关）
        if (!quiet.contains(currentMinutes)) {
            notificationHelper.showReminder(task)
        }

        // 排下一次
        reminderScheduler.schedule(task)
        return Result.success()
    }

    companion object {
        const val KEY_TASK_ID = "key_task_id"
    }
}
