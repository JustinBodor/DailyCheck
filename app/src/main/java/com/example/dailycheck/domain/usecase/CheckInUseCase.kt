package com.example.dailycheck.domain.usecase

import com.example.dailycheck.data.repository.TaskRepository
import javax.inject.Inject
import javax.inject.Singleton

/** 打卡相关的统一入口：完成 / 撤销 / 补昨天 / 计数。 */
@Singleton
class CheckInUseCase @Inject constructor(
    private val repository: TaskRepository
) {

    suspend fun complete(taskId: Long, elapsedSeconds: Int = 0, source: String = "MANUAL") {
        repository.completeTask(taskId, elapsedSeconds, source)
    }

    suspend fun undo(taskId: Long) {
        repository.undoComplete(taskId)
    }

    suspend fun makeupYesterday(taskId: Long): Boolean = repository.makeupYesterday(taskId)

    suspend fun incrementCount(taskId: Long): Int = repository.incrementCount(taskId)
}
