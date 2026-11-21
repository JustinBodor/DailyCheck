package com.example.dailycheck.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.dailycheck.data.local.entity.CheckInRecordEntity
import com.example.dailycheck.data.local.model.DailyCheckInCount
import kotlinx.coroutines.flow.Flow

@Dao
interface CheckInRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: CheckInRecordEntity): Long

    @Query("DELETE FROM check_in_records WHERE taskId = :taskId AND epochDay = :epochDay")
    suspend fun delete(taskId: Long, epochDay: Long)

    @Query("DELETE FROM check_in_records WHERE taskId = :taskId")
    suspend fun deleteByTask(taskId: Long)

    @Query("SELECT * FROM check_in_records WHERE taskId = :taskId AND epochDay = :epochDay LIMIT 1")
    suspend fun find(taskId: Long, epochDay: Long): CheckInRecordEntity?

    @Query("SELECT * FROM check_in_records WHERE taskId = :taskId ORDER BY epochDay DESC")
    suspend fun recordsForTask(taskId: Long): List<CheckInRecordEntity>

    @Query("SELECT * FROM check_in_records ORDER BY epochDay DESC")
    fun observeAll(): Flow<List<CheckInRecordEntity>>

    // ---------- 统计 ----------

    @Query("SELECT COUNT(*) FROM check_in_records")
    fun observeTotalCheckIns(): Flow<Int>

    @Query(
        "SELECT epochDay, COUNT(*) AS count FROM check_in_records " +
            "WHERE epochDay BETWEEN :fromEpochDay AND :toEpochDay GROUP BY epochDay"
    )
    fun observeDailyCounts(fromEpochDay: Long, toEpochDay: Long): Flow<List<DailyCheckInCount>>

    @Query(
        "SELECT * FROM check_in_records WHERE epochDay BETWEEN :fromEpochDay AND :toEpochDay " +
            "ORDER BY completedAtMillis ASC"
    )
    fun observeRecordsBetween(
        fromEpochDay: Long,
        toEpochDay: Long
    ): Flow<List<CheckInRecordEntity>>

    @Query("SELECT DISTINCT epochDay FROM check_in_records ORDER BY epochDay DESC")
    fun observeAllDays(): Flow<List<Long>>
}
