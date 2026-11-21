package com.example.dailycheck.data.local.model

/** 按天聚合的打卡数量（Room 查询投影）。 */
data class DailyCheckInCount(
    val epochDay: Long,
    val count: Int
)
