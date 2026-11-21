package com.example.dailycheck.data.repository

import com.example.dailycheck.data.local.dao.CheckInRecordDao
import com.example.dailycheck.data.local.entity.CheckInRecordEntity
import com.example.dailycheck.data.local.model.DailyCheckInCount
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class StatsRepositoryImpl @Inject constructor(
    private val checkInRecordDao: CheckInRecordDao
) : StatsRepository {

    override fun observeTotalCheckIns(): Flow<Int> =
        checkInRecordDao.observeTotalCheckIns()

    override fun observeDailyCounts(
        fromEpochDay: Long,
        toEpochDay: Long
    ): Flow<List<DailyCheckInCount>> =
        checkInRecordDao.observeDailyCounts(fromEpochDay, toEpochDay)

    override fun observeAllDays(): Flow<List<Long>> =
        checkInRecordDao.observeAllDays()

    override fun observeRecordsBetween(
        fromEpochDay: Long,
        toEpochDay: Long
    ): Flow<List<CheckInRecordEntity>> =
        checkInRecordDao.observeRecordsBetween(fromEpochDay, toEpochDay)
}
