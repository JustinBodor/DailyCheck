package com.example.dailycheck.ui.screens.timer

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dailycheck.data.model.Task
import com.example.dailycheck.data.repository.TaskRepository
import com.example.dailycheck.data.timer.TimerBus
import com.example.dailycheck.data.timer.TimerStateStore
import com.example.dailycheck.domain.usecase.CheckInUseCase
import com.example.dailycheck.service.TaskTimerService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 专注计时抽屉的状态机。
 * 真正的倒计时在前台服务里跑，这里只负责展示与转发指令，
 * 因此退到后台、锁屏都不会中断。
 */
@HiltViewModel
class TimerViewModel @Inject constructor(
    private val store: TimerStateStore,
    private val repository: TaskRepository,
    private val checkInUseCase: CheckInUseCase
) : ViewModel() {

    data class UiState(
        val taskId: Long = -1L,
        val taskName: String = "",
        val targetSeconds: Int = 0,
        val remainingSeconds: Int = 0,
        val running: Boolean = false,
        val paused: Boolean = false,
        val finished: Boolean = false
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var boundTaskId: Long = -1L

    init {
        viewModelScope.launch {
            TimerBus.state.collect { timer ->
                if (timer == null) {
                    // 倒计时归零（服务完成打卡）或取消
                    if (_uiState.value.running) {
                        _uiState.update {
                            it.copy(
                                running = false,
                                paused = false,
                                remainingSeconds = 0,
                                finished = true
                            )
                        }
                    }
                } else if (timer.taskId == boundTaskId) {
                    _uiState.update {
                        it.copy(
                            taskName = timer.taskName,
                            remainingSeconds = timer.remainingSeconds,
                            running = timer.running,
                            paused = timer.paused,
                            finished = false
                        )
                    }
                }
            }
        }
    }

    /** 打开抽屉时绑定任务，并恢复可能存在的未完成计时 */
    fun bind(task: Task) {
        // 每次打开抽屉都重置，避免上一次的 finished 状态残留
        boundTaskId = task.id

        _uiState.value = UiState(
            taskId = task.id,
            taskName = task.name,
            targetSeconds = task.targetDurationSeconds,
            remainingSeconds = task.targetDurationSeconds
        )

        viewModelScope.launch {
            val saved = store.current()
            if (saved != null && saved.taskId == task.id) {
                _uiState.update {
                    it.copy(
                        targetSeconds = saved.targetSeconds,
                        remainingSeconds = saved.remainingSeconds(),
                        running = saved.running,
                        paused = !saved.running
                    )
                }
            } else {
                // 任务可能被外部修改了目标时长
                repository.getTask(task.id)?.let { latest ->
                    if (latest.targetDurationSeconds != _uiState.value.targetSeconds) {
                        _uiState.update {
                            it.copy(
                                targetSeconds = latest.targetDurationSeconds,
                                remainingSeconds = latest.targetDurationSeconds
                            )
                        }
                    }
                }
            }
        }
    }

    fun start(context: Context, task: Task) {
        _uiState.update {
            it.copy(
                taskId = task.id,
                taskName = task.name,
                targetSeconds = task.targetDurationSeconds,
                remainingSeconds = task.targetDurationSeconds,
                running = true,
                paused = false,
                finished = false
            )
        }
        TaskTimerService.start(context, task)
    }

    fun pause(context: Context) {
        TaskTimerService.pause(context)
    }

    fun resume(context: Context) {
        TaskTimerService.resume(context)
    }

    /** 结束并完成：停服务、清计时状态、写一次打卡记录 */
    fun finishAndComplete(context: Context, task: Task) {
        val state = _uiState.value
        val elapsed = (state.targetSeconds - state.remainingSeconds).coerceAtLeast(1)

        // 1) 立即停掉前台服务与通知
        TaskTimerService.cancel(context)

        // 2) UI 立刻归零
        _uiState.update {
            it.copy(
                running = false,
                paused = false,
                remainingSeconds = 0,
                finished = true
            )
        }

        // 3) 清 DataStore 计时状态，确保后台不再跳动
        viewModelScope.launch {
            store.clear()
            TimerBus.publish(null)
            checkInUseCase.complete(task.id, elapsedSeconds = elapsed, source = "TIMER")
        }
    }

    /** 结束（放弃本次专注） */
    fun stop(context: Context) {
        _uiState.update {
            it.copy(
                remainingSeconds = it.targetSeconds,
                running = false,
                paused = false,
                finished = false
            )
        }
        TaskTimerService.cancel(context)
    }
}
