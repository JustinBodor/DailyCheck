package com.example.dailycheck.data.model

import java.time.LocalDate

data class Task(
    val id: Long = 0L,
    val name: String,
    val iconKey: String,
    val colorArgb: Long,
    val category: String? = null,
    val repeatRule: RepeatRule = RepeatRule.Daily,
    val note: String? = null,
    /** 提醒时间（当日分钟数 0..1439），null 表示不提醒 */
    val reminderMinutes: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
    /** 只在“当天”有效，跨天自动视为未完成 */
    val completedOnEpochDay: Long? = null,
    val type: TaskType = TaskType.COMPLETE,
    /** 计数型目标次数 */
    val targetCount: Int = 1,
    /** 计时型目标总秒数（唯一数据源） */
    val targetDurationSeconds: Int = 1500,
    /** 由打卡记录推导出的连续天数（不入库） */
    val streak: Int = 0,
    /** 计数型当天的进度（不入库） */
    val todayCount: Int = 0,
    /** 今天这条打卡记录的完成时间毫秒（不入库，仅用于已完成列表排序） */
    val completedAtMillis: Long? = null
) {

    /** 展示用分钟数（向上取整，至少 1 分钟） */
    val targetDurationMinutes: Int get() = (targetDurationSeconds / 60).coerceAtLeast(1)

    fun isCompletedOn(epochDay: Long): Boolean = completedOnEpochDay == epochDay

    fun isCompletedToday(): Boolean =
        isCompletedOn(LocalDate.now().toEpochDay())
}
