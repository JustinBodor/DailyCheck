package com.example.dailycheck.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.dailycheck.data.model.RepeatRule
import com.example.dailycheck.data.model.TaskType

/**
 * 打卡任务表。
 * 完成状态只记录“完成于哪一天”，跨天自动失效，天然支持每日重置。
 */
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val iconKey: String,
    val colorArgb: Long,
    val category: String?,
    val repeatRule: RepeatRule,
    val note: String?,
    val reminderEnabled: Boolean,
    /** 0..1439，reminderEnabled=false 时无意义 */
    val reminderMinutes: Int,
    val createdAt: Long,
    val completedOnEpochDay: Long?,
    /** COMPLETE / COUNT / TIMED */
    val type: TaskType = TaskType.COMPLETE,
    val targetCount: Int = 1,
    val targetDurationMinutes: Int = 25,
    /** 计时型目标总秒数（唯一数据源） */
    val targetDurationSeconds: Int = 1500
)
