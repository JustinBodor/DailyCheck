package com.example.dailycheck.service

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.dailycheck.data.model.Task
import com.example.dailycheck.data.repository.TaskRepository
import com.example.dailycheck.data.timer.TimerBus
import com.example.dailycheck.data.timer.TimerState
import com.example.dailycheck.data.timer.TimerStateStore
import com.example.dailycheck.data.timer.TimerUiState
import com.example.dailycheck.domain.usecase.CheckInUseCase
import com.example.dailycheck.notification.NotificationHelper
import com.example.dailycheck.permission.NotificationPermissionHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 计时型任务前台服务（specialUse）。
 * 每秒由协程更新通知并向 UI 推一次状态；UI 不轮询。
 */
@AndroidEntryPoint
class TaskTimerService : Service() {

    @Inject
    lateinit var store: TimerStateStore

    @Inject
    lateinit var repository: TaskRepository

    @Inject
    lateinit var useCase: CheckInUseCase

    @Inject
    lateinit var notificationHelper: NotificationHelper

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var ticker: Job? = null

    private var active: TimerState? = null
    private var currentTaskId: Long = -1L
    private var currentName: String = "计时任务"

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY

        intent.getLongExtra(EXTRA_TASK_ID, -1L).takeIf { it > 0L }?.let { currentTaskId = it }
        intent.getStringExtra(EXTRA_TASK_NAME)?.takeIf { it.isNotBlank() }?.let { currentName = it }

        // 无论哪个 action，先起来一个前台通知，满足 5 秒规则
        startForegroundCompat(buildNotification(active?.remainingSeconds() ?: 0, paused = true))

        when (action) {
            ACTION_START -> handleStart(intent)
            ACTION_RESUME -> handleResume()
            ACTION_PAUSE -> handlePause()
            ACTION_CANCEL -> handleCancel()
            ACTION_RESTORE -> handleRestore()
        }
        return START_NOT_STICKY
    }

    private fun handleStart(intent: Intent) {
        val target = intent.getIntExtra(EXTRA_TARGET_SECONDS, 0).coerceAtLeast(1)
        active = TimerState(
            taskId = currentTaskId,
            targetSeconds = target,
            accumulatedSeconds = 0,
            startedAtMillis = System.currentTimeMillis(),
            running = true
        )
        persist(active!!)
        startTicker()
    }

    private fun handleResume() {
        scope.launch {
            val state = active ?: store.current() ?: return@launch
            if (state.remainingSeconds() <= 0) {
                finish(state.taskId)
                return@launch
            }
            val resumed = state.copy(
                accumulatedSeconds = state.elapsedSeconds(),
                startedAtMillis = System.currentTimeMillis(),
                running = true
            )
            active = resumed
            persist(resumed)
            startTicker()
        }
    }

    private fun handlePause() {
        scope.launch {
            val state = active ?: store.current() ?: return@launch
            val paused = state.copy(
                accumulatedSeconds = state.elapsedSeconds(),
                startedAtMillis = 0L,
                running = false
            )
            active = paused
            persist(paused)
            ticker?.cancel()
            TimerBus.publish(
                TimerUiState(
                    taskId = paused.taskId,
                    taskName = currentName,
                    remainingSeconds = paused.remainingSeconds(),
                    running = false,
                    paused = true
                )
            )
            updateNotification(paused.remainingSeconds(), paused = true)
        }
    }

    private fun handleCancel() {
        ticker?.cancel()
        scope.launch {
            store.clear()
            active = null
            TimerBus.publish(null)
            stopForegroundCompat()
            stopSelf()
        }
    }

    private fun handleRestore() {
        scope.launch {
            val state = store.current() ?: run {
                stopSelf()
                return@launch
            }
            currentTaskId = state.taskId
            repository.getTask(state.taskId)?.let { currentName = it.name }

            if (state.remainingSeconds() <= 0) {
                finish(state.taskId)
                return@launch
            }
            active = state
            if (state.running) {
                startTicker()
            } else {
                TimerBus.publish(
                    TimerUiState(
                        taskId = state.taskId,
                        taskName = currentName,
                        remainingSeconds = state.remainingSeconds(),
                        running = false,
                        paused = true
                    )
                )
                updateNotification(state.remainingSeconds(), paused = true)
            }
        }
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = scope.launch {
            while (isActive) {
                val state = active ?: break
                val remaining = state.remainingSeconds()
                if (remaining <= 0) {
                    finish(state.taskId)
                    break
                }
                TimerBus.publish(
                    TimerUiState(
                        taskId = state.taskId,
                        taskName = currentName,
                        remainingSeconds = remaining,
                        running = true
                    )
                )
                updateNotification(remaining, paused = false)
                delay(1000)
            }
        }
    }

    /** 到点：写一条 TIMER 来源的打卡记录，发通知，清理状态 */
    private fun finish(taskId: Long) {
        ticker?.cancel()
        scope.launch {
            val task = repository.getTask(taskId)
            val elapsed = task?.targetDurationSeconds ?: active?.targetSeconds ?: 0
            useCase.complete(taskId, elapsedSeconds = elapsed, source = "TIMER")
            vibrateFinished()
            store.clear()
            active = null
            TimerBus.publish(null)
            notificationHelper.notifyTimerFinished(task?.name ?: currentName, taskId)
            stopForegroundCompat()
            stopSelf()
        }
    }

    /** 专注完成：短促震动提示 */
    private fun vibrateFinished() {
        val vibrator = getSystemService(Vibrator::class.java) ?: return
        if (!vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        }
    }

    private fun persist(state: TimerState) {
        scope.launch { store.save(state) }
    }

    private fun buildNotification(remaining: Int, paused: Boolean): Notification =
        notificationHelper.buildTimerNotification(
            taskId = currentTaskId,
            taskName = currentName,
            remainingSeconds = remaining,
            paused = paused
        )

    private fun updateNotification(remaining: Int, paused: Boolean) {
        if (!NotificationPermissionHelper.hasPermission(this)) return
        runCatching {
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, buildNotification(remaining, paused))
        }
    }

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, 0)
        }
    }

    private fun stopForegroundCompat() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
    }

    override fun onDestroy() {
        ticker?.cancel()
        // 注意：不清 DataStore，进程被杀后由 HomeViewModel 恢复；
        // 只有取消/到点才会显式 clear()。
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.example.dailycheck.action.TIMER_START"
        const val ACTION_PAUSE = "com.example.dailycheck.action.TIMER_PAUSE"
        const val ACTION_RESUME = "com.example.dailycheck.action.TIMER_RESUME"
        const val ACTION_CANCEL = "com.example.dailycheck.action.TIMER_CANCEL"
        const val ACTION_RESTORE = "com.example.dailycheck.action.TIMER_RESTORE"

        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_NAME = "extra_task_name"
        const val EXTRA_TARGET_SECONDS = "extra_target_seconds"
        private const val NOTIFICATION_ID = 0x7A10

        fun start(context: Context, task: Task) = send(
            context,
            Intent(context, TaskTimerService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_TASK_ID, task.id)
                putExtra(EXTRA_TASK_NAME, task.name)
                putExtra(EXTRA_TARGET_SECONDS, task.targetDurationSeconds)
            }
        )

        fun pause(context: Context) = send(context, intent(context, ACTION_PAUSE))

        fun resume(context: Context) = send(context, intent(context, ACTION_RESUME))

        fun cancel(context: Context) = send(context, intent(context, ACTION_CANCEL))

        fun restore(context: Context) = send(context, intent(context, ACTION_RESTORE))

        private fun intent(context: Context, action: String) =
            Intent(context, TaskTimerService::class.java).apply { this.action = action }

        private fun send(context: Context, intent: Intent) {
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
