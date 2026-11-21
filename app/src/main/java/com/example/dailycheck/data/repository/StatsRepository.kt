package com.example.dailycheck.data.repository

import com.example.dailycheck.data.local.entity.CheckInRecordEntity
import com.example.dailycheck.data.local.model.DailyCheckInCount
import kotlinx.coroutines.flow.Flow

/** 统计页数据源：全部来自 Room 的 Flow。 */
interface StatsRepository {

    fun observeTotalCheckIns(): Flow<Int>

    fun observeDailyCounts(fromEpochDay: Long, toEpochDay: Long): Flow<List<DailyCheckInCount>>

    /** 所有有打卡记录的日期（降序） */
    fun observeAllDays(): Flow<List<Long>>

    /** 区间内的原始打卡记录（按完成时间升序），用于按任务着色 */
    fun observeRecordsBetween(
        fromEpochDay: Long,
        toEpochDay: Long
    ): Flow<List<CheckInRecordEntity>>
}
