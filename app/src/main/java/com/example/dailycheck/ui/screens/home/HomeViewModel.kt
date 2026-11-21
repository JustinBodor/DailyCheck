package com.example.dailycheck.ui.screens.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dailycheck.core.util.DateTimeUtils
import com.example.dailycheck.data.model.Task
import com.example.dailycheck.data.model.TaskType
import com.example.dailycheck.data.repository.TaskRepository
import com.example.dailycheck.data.timer.TimerBus
import com.example.dailycheck.data.timer.TimerStateStore
import com.example.dailycheck.domain.usecase.CheckInUseCase
import com.example.dailycheck.service.TaskTimerService
import com.example.dailycheck.widget.refreshTodayWidget
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val checkInUseCase: CheckInUseCase,
    private val timerStateStore: TimerStateStore,
    @param:ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        taskRepository.observeTodayTasks()
            .onEach { tasks ->
                val today = DateTimeUtils.todayEpochDay()
                val active = tasks.filter { !it.isCompletedOn(today) }

                // 已完成：按完成时间倒序 —— 后打卡的紧挨未完成区，先打卡的沉到最底
                val completed = tasks
                    .filter { it.isCompletedOn(today) }
                    .sortedByDescending { it.completedAtMillis ?: Long.MIN_VALUE }

                // 任务一旦进入已完成，立刻停掉它残留的计时（服务 + DataStore + UI 状态）
                val staleTimer = _uiState.value.timer
                    ?.takeIf { timer -> completed.any { it.id == timer.taskId } }
                if (staleTimer != null) {
                    TaskTimerService.cancel(appContext)
                    TimerBus.publish(null)
                    viewModelScope.launch { timerStateStore.clear() }
                }

                _uiState.update {
                    it.copy(isLoading = false, activeTasks = active, completedTasks = completed)
                }
                // Room 数据变化（打卡 / 撤销 / 增删）即刷新桌面小组件
                refreshWidget()
            }
            .launchIn(viewModelScope)

        timerStateStore.allowMakeup
            .onEach { allow -> _uiState.update { it.copy(allowMakeup = allow) } }
            .launchIn(viewModelScope)

        timerStateStore.quietHours
            .onEach { quiet -> _uiState.update { it.copy(quietHours = quiet) } }
            .launchIn(viewModelScope)

        TimerBus.state
            .onEach { timer -> _uiState.update { it.copy(timer = timer) } }
            .launchIn(viewModelScope)

        restoreTimerIfNeeded()
    }

    /** 进程被杀后恢复：未到点重启 Service，已到点直接补完成 */
    private fun restoreTimerIfNeeded() {
        viewModelScope.launch {
            val saved = timerStateStore.current() ?: return@launch
            if (saved.remainingSeconds() <= 0) {
                checkInUseCase.complete(saved.taskId, saved.targetSeconds, "TIMER")
                timerStateStore.clear()
                _uiState.update { it.copy(message = "计时已到点，已自动打卡") }
            } else {
                TaskTimerService.restore(appContext)
            }
        }
    }

    // ---------- 新建 / 编辑 / 删除 ----------

    fun startCreate() {
        _uiState.update { state ->
            if (state.draft != null) state else state.copy(draft = TaskDraft())
        }
    }

    fun startEdit(task: Task) {
        _uiState.update { it.copy(draft = TaskDraft.from(task)) }
    }

    fun cancelCreate() {
        _uiState.update { it.copy(draft = null) }
    }

    fun updateDraft(draft: TaskDraft) {
        _uiState.update { state ->
            if (state.draft == null) state else state.copy(draft = draft)
        }
    }

    fun saveDraft() {
        val draft = _uiState.value.draft ?: return
        if (draft.name.isBlank()) {
            _uiState.update { it.copy(draft = draft.copy(nameError = true)) }
            return
        }
        viewModelScope.launch {
            val task = draft.toTask(DateTimeUtils.todayEpochDay())
            if (draft.id > 0L) {
                taskRepository.updateTask(task)
                refreshWidget()
                _uiState.update { it.copy(draft = null) }
            } else {
                taskRepository.createTask(task)
                refreshWidget()
                _uiState.update { it.copy(draft = null, createdFlash = it.createdFlash + 1) }
            }
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            if (_uiState.value.timer?.taskId == task.id) {
                TaskTimerService.cancel(appContext)
            }
            taskRepository.deleteTask(task.id)
            refreshWidget()
            _uiState.update {
                it.copy(draft = null, message = "已删除「${task.name}」及其记录")
            }
        }
    }

    private fun refreshWidget() {
        viewModelScope.launch { refreshTodayWidget(appContext) }
    }

    // ---------- 打卡 / 撤销 ----------

    fun toggleComplete(task: Task, completed: Boolean) {
        // 计时型任务不允许直接打勾 / 撤销，只能走计时流程
        if (task.type == TaskType.TIMED) return
        viewModelScope.launch {
            if (completed) {
                checkInUseCase.complete(task.id, 0, "MANUAL")
                _uiState.update { it.copy(undoableTask = task) }
                refreshWidget()
            } else {
                checkInUseCase.undo(task.id)
            }
            refreshWidget()
        }
    }

    fun undoLastCheckIn() {
        val task = _uiState.value.undoableTask ?: return
        viewModelScope.launch {
            checkInUseCase.undo(task.id)
            _uiState.update { it.copy(undoableTask = null) }
            refreshWidget()
        }
    }

    fun onSnackbarConsumed() {
        _uiState.update { it.copy(undoableTask = null) }
    }

    /** 撤销今天的打卡（圆圈再次点击 / 左滑 / 长按菜单都走这里） */
    fun undoCheckIn(task: Task) {
        undoTask(task)
    }

    fun undoTask(task: Task) {
        viewModelScope.launch {
            checkInUseCase.undo(task.id)
            _uiState.update { it.copy(message = "已撤销「${task.name}」") }
            refreshWidget()
        }
    }

    // ---------- 补打卡 ----------

    fun makeupYesterday(task: Task) {
        // 补打卡仅针对完成型任务
        if (task.type != TaskType.COMPLETE) return
        viewModelScope.launch {
            val ok = checkInUseCase.makeupYesterday(task.id)
            _uiState.update {
                it.copy(message = if (ok) "已补上昨天「${task.name}」" else "补打卡失败：已补过或设置已关闭")
            }
        }
    }

    fun setAllowMakeup(allow: Boolean) {
        viewModelScope.launch {
            taskRepository.setAllowMakeup(allow)
            _uiState.update { it.copy(message = if (allow) "已允许补打卡" else "已关闭补打卡") }
        }
    }

    // ---------- 免打扰 ----------

    fun setQuietEnabled(enabled: Boolean) {
        viewModelScope.launch { timerStateStore.setQuietEnabled(enabled) }
    }

    fun setQuietStart(minutes: Int) {
        viewModelScope.launch { timerStateStore.setQuietStart(minutes) }
    }

    fun setQuietEnd(minutes: Int) {
        viewModelScope.launch { timerStateStore.setQuietEnd(minutes) }
    }

    // ---------- 计时 ----------

    fun startTimer(task: Task) = TaskTimerService.start(appContext, task)

    fun pauseTimer() = TaskTimerService.pause(appContext)

    fun resumeTimer() = TaskTimerService.resume(appContext)

    fun cancelTimer() = TaskTimerService.cancel(appContext)

    /** 已完成的计时型任务：撤销打卡、清空旧状态并立刻重新开始 */
    fun restartTimer(task: Task) {
        viewModelScope.launch {
            checkInUseCase.undo(task.id)
            TaskTimerService.cancel(appContext)
            timerStateStore.clear()
            TaskTimerService.start(appContext, task)
        }
    }

    // ---------- 计数 ----------

    fun incrementCount(task: Task) {
        viewModelScope.launch {
            checkInUseCase.incrementCount(task.id)
            refreshWidget()
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
