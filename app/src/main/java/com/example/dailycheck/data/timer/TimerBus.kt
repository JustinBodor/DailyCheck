package com.example.dailycheck.data.timer

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** UI 侧计时状态（由 Service 每秒推送，UI 不轮询）。 */
data class TimerUiState(
    val taskId: Long,
    val taskName: String,
    val remainingSeconds: Int,
    val running: Boolean,
    val paused: Boolean = false
)

/**
 * Service 与 UI 同进程，用一个单例 StateFlow 做单向通知即可，
 * 不引入广播/Binder，避免 P1 过度设计。
 */
object TimerBus {

    private val _state = MutableStateFlow<TimerUiState?>(null)
    val state: StateFlow<TimerUiState?> = _state.asStateFlow()

    fun publish(state: TimerUiState?) {
        _state.value = state
    }
}
