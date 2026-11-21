package com.example.dailycheck.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.dailycheck.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<TaskEntity>>

    @Query(
        "SELECT DISTINCT category FROM tasks " +
            "WHERE category IS NOT NULL AND TRIM(category) != '' ORDER BY category ASC"
    )
    fun observeCategories(): Flow<List<String>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: Long): TaskEntity?

    @Insert
    suspend fun insert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Query("UPDATE tasks SET completedOnEpochDay = :epochDay WHERE id = :id")
    suspend fun setCompleted(id: Long, epochDay: Long?)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: Long)
}
