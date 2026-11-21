package com.example.dailycheck.data.repository

import com.example.dailycheck.data.model.Task
import kotlinx.coroutines.flow.Flow

interface TaskRepository {

    fun observeTasks(): Flow<List<Task>>

    /** 今日列表：按重复规则过滤 + 提醒时间升序（无提醒排后） */
    fun observeTodayTasks(): Flow<List<Task>>

    /** 历史分类（去重） */
    fun observeCategories(): Flow<List<String>>

    suspend fun getTask(id: Long): Task?

    suspend fun createTask(task: Task): Long

    suspend fun updateTask(task: Task)

    suspend fun deleteTask(taskId: Long)

    /** 完成打卡（写打卡记录 + 标记当天完成） */
    suspend fun completeTask(taskId: Long, elapsedSeconds: Int = 0, source: String = "MANUAL")

    /** 撤销今天的打卡（删记录 + 清标记） */
    suspend fun undoComplete(taskId: Long)

    /** 补昨天：成功返回 true（重复补/开关关闭返回 false） */
    suspend fun makeupYesterday(taskId: Long): Boolean

    /** 计数型 +1，返回累加后的次数；到达目标自动完成 */
    suspend fun incrementCount(taskId: Long): Int

    suspend fun allowMakeup(): Boolean

    suspend fun setAllowMakeup(allow: Boolean)
}
