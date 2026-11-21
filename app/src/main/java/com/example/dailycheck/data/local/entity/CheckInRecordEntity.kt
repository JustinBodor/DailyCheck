package com.example.dailycheck.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 打卡记录。一次完成 = 一条记录，补打卡也写在这里。
 * (taskId, epochDay) 唯一，避免同一天重复打卡。
 */
@Entity(
    tableName = "check_in_records",
    indices = [Index(value = ["taskId", "epochDay"], unique = true)]
)
data class CheckInRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val taskId: Long,
    val epochDay: Long,
    val completedAtMillis: Long,
    /** 计时型任务本次实际计时秒数，其他类型为 0 */
    val elapsedSeconds: Int = 0,
    /** MANUAL / TIMER / COUNT / MAKEUP */
    val source: String = "MANUAL"
)
