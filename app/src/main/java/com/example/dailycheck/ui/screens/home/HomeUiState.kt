package com.example.dailycheck.ui.screens.home

import com.example.dailycheck.data.model.RepeatRule
import com.example.dailycheck.data.model.Task
import com.example.dailycheck.data.model.TaskType
import com.example.dailycheck.data.timer.QuietHours
import com.example.dailycheck.data.timer.TimerUiState
import com.example.dailycheck.ui.components.TaskStyleCatalog
import java.time.DayOfWeek
import java.time.LocalDate

/** 计时单位：界面输入用，最终统一换算成秒入库 */
enum class DurationUnit(val label: String, val seconds: Int) {
    SECONDS("秒", 1),
    MINUTES("分钟", 60),
    HOURS("小时", 3600),
    DAYS("天", 86400);

    companion object {
        /** 选出能整除给定秒数的最“大”单位 */
        fun bestFor(totalSeconds: Int): Pair<DurationUnit, Int> {
            val safe = totalSeconds.coerceAtLeast(1)
            val unit = when {
                safe % 86400 == 0 -> DAYS
                safe % 3600 == 0 -> HOURS
                safe % 60 == 0 -> MINUTES
                else -> SECONDS
            }
            return unit to (safe / unit.seconds).coerceAtLeast(1)
        }
    }
}

enum class RepeatType(val label: String) {
    DAILY("每天"),
    WEEKLY("每周"),
    MONTHLY("每月"),
    INTERVAL("每N天")
}

/** 编辑草稿：id > 0 表示编辑已有任务 */
data class TaskDraft(
    val id: Long = 0L,
    val name: String = "",
    val iconKey: String = TaskStyleCatalog.defaultIconKey,
    val colorArgb: Long = TaskStyleCatalog.defaultColorArgb,
    val category: String = "",
    val note: String = "",
    val repeatType: RepeatType = RepeatType.DAILY,
    val weeklyDays: Set<DayOfWeek> = setOf(LocalDate.now().dayOfWeek),
    val monthlyDay: Int = LocalDate.now().dayOfMonth,
    val intervalDays: Int = 2,
    val reminderEnabled: Boolean = false,
    val reminderMinutes: Int = 9 * 60,
    val nameError: Boolean = false,
    val type: TaskType = TaskType.COMPLETE,
    val targetCount: Int = 3,
    val targetDurationSeconds: Int = 300,
    val createdAt: Long = 0L,
    val completedOnEpochDay: Long? = null
) {

    fun toRepeatRule(anchorEpochDay: Long): RepeatRule = when (repeatType) {
        RepeatType.DAILY -> RepeatRule.Daily
        RepeatType.WEEKLY -> RepeatRule.Weekly(
            weeklyDays.ifEmpty { setOf(LocalDate.now().dayOfWeek) }
        )
        RepeatType.MONTHLY -> RepeatRule.Monthly(monthlyDay)
        RepeatType.INTERVAL -> RepeatRule.EveryNDays(intervalDays, anchorEpochDay)
    }

    fun toTask(anchorEpochDay: Long): Task = Task(
        id = id,
        name = name.trim(),
        iconKey = iconKey,
        colorArgb = colorArgb,
        category = category.trim().takeIf { it.isNotBlank() },
        repeatRule = toRepeatRule(anchorEpochDay),
        note = note.trim().takeIf { it.isNotBlank() },
        reminderMinutes = if (reminderEnabled) reminderMinutes else null,
        createdAt = if (createdAt > 0L) createdAt else System.currentTimeMillis(),
        completedOnEpochDay = completedOnEpochDay,
        type = type,
        targetCount = targetCount,
        targetDurationSeconds = targetDurationSeconds
    )

    companion object {
        fun from(task: Task): TaskDraft = TaskDraft(
            id = task.id,
            name = task.name,
            iconKey = task.iconKey,
            colorArgb = task.colorArgb,
            category = task.category.orEmpty(),
            note = task.note.orEmpty(),
            repeatType = when (task.repeatRule) {
                is RepeatRule.Daily -> RepeatType.DAILY
                is RepeatRule.Weekly -> RepeatType.WEEKLY
                is RepeatRule.Monthly -> RepeatType.MONTHLY
                is RepeatRule.EveryNDays -> RepeatType.INTERVAL
            },
            weeklyDays = (task.repeatRule as? RepeatRule.Weekly)?.daysOfWeek
                ?: setOf(LocalDate.now().dayOfWeek),
            monthlyDay = (task.repeatRule as? RepeatRule.Monthly)?.dayOfMonth
                ?: LocalDate.now().dayOfMonth,
            intervalDays = (task.repeatRule as? RepeatRule.EveryNDays)?.intervalDays ?: 2,
            reminderEnabled = task.reminderMinutes != null,
            reminderMinutes = task.reminderMinutes ?: (9 * 60),
            type = task.type,
            targetCount = task.targetCount,
            targetDurationSeconds = task.targetDurationSeconds,
            createdAt = task.createdAt,
            completedOnEpochDay = task.completedOnEpochDay
        )
    }
}

data class HomeUiState(
    val isLoading: Boolean = true,
    val activeTasks: List<Task> = emptyList(),
    val completedTasks: List<Task> = emptyList(),
    val draft: TaskDraft? = null,
    val timer: TimerUiState? = null,
    val allowMakeup: Boolean = true,
    val quietHours: QuietHours = QuietHours(),
    val undoableTask: Task? = null,
    val message: String? = null,
    /** 每成功新建一次 +1，用于触发彩带 */
    val createdFlash: Int = 0
)
