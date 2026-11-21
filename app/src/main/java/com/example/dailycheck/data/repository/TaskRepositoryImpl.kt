package com.example.dailycheck.data.repository

import com.example.dailycheck.core.util.DateTimeUtils
import com.example.dailycheck.core.util.RepeatRuleUtils
import com.example.dailycheck.data.local.dao.CheckInRecordDao
import com.example.dailycheck.data.local.dao.TaskDao
import com.example.dailycheck.data.local.entity.CheckInRecordEntity
import com.example.dailycheck.data.local.entity.TaskEntity
import com.example.dailycheck.data.model.Task
import com.example.dailycheck.data.timer.TimerStateStore
import com.example.dailycheck.notification.ReminderScheduler
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao,
    private val checkInRecordDao: CheckInRecordDao,
    private val timerStateStore: TimerStateStore,
    private val reminderScheduler: ReminderScheduler
) : TaskRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val counts = MutableStateFlow<Map<Long, Int>>(emptyMap())

    private val reminderFirstComparator: Comparator<Task> =
        compareBy<Task> { it.reminderMinutes ?: Int.MAX_VALUE }
            .thenBy { it.createdAt }

    init {
        scope.launch {
            counts.value = timerStateStore.readCounts(DateTimeUtils.todayEpochDay())
        }
    }

    override fun observeTasks(): Flow<List<Task>> = combine(
        taskDao.observeAll(),
        checkInRecordDao.observeAll(),
        counts
    ) { tasks, records, countMap ->
        val today = DateTimeUtils.todayEpochDay()
        val daysByTask: Map<Long, Set<Long>> = records
            .groupBy { it.taskId }
            .mapValues { (_, list) -> list.map { it.epochDay }.toSet() }

        // 今天各任务的打卡时间（用于已完成列表按完成先后排序）
        val completedAtByTask: Map<Long, Long> = records
            .filter { it.epochDay == today }
            .associate { it.taskId to it.completedAtMillis }

        tasks.map { entity ->
            val days = daysByTask[entity.id].orEmpty()
            entity.toDomain(
                streak = computeStreak(days, today),
                todayCount = if (entity.completedOnEpochDay == today) {
                    entity.targetCount
                } else {
                    countMap[entity.id] ?: 0
                },
                completedAtMillis = completedAtByTask[entity.id]
            )
        }
    }

    override fun observeTodayTasks(): Flow<List<Task>> =
        observeTasks().map { tasks ->
            val today = DateTimeUtils.today()
            tasks.asSequence()
                .filter { RepeatRuleUtils.isActiveOn(it.repeatRule, today) }
                .sortedWith(reminderFirstComparator)
                .toList()
        }

    override fun observeCategories(): Flow<List<String>> = taskDao.observeCategories()

    override suspend fun getTask(id: Long): Task? {
        val entity = taskDao.getById(id) ?: return null
        val days = checkInRecordDao.recordsForTask(id).map { it.epochDay }.toSet()
        val today = DateTimeUtils.todayEpochDay()
        val count = timerStateStore.countFor(id, today)
        return entity.toDomain(
            streak = computeStreak(days, today),
            todayCount = count,
            completedAtMillis = checkInRecordDao.find(id, today)?.completedAtMillis
        )
    }

    override suspend fun createTask(task: Task): Long {
        val newId = taskDao.insert(task.toEntity())
        val saved = task.copy(id = newId)
        if (saved.reminderMinutes != null) {
            reminderScheduler.schedule(saved)
        } else {
            reminderScheduler.cancel(newId)
        }
        return newId
    }

    override suspend fun updateTask(task: Task) {
        taskDao.update(task.toEntity())
        reminderScheduler.cancel(task.id)
        if (task.reminderMinutes != null) {
            reminderScheduler.schedule(task)
        }
    }

    override suspend fun deleteTask(taskId: Long) {
        reminderScheduler.cancel(taskId)
        checkInRecordDao.deleteByTask(taskId)
        taskDao.deleteById(taskId)
    }

    override suspend fun completeTask(taskId: Long, elapsedSeconds: Int, source: String) {
        val today = DateTimeUtils.todayEpochDay()
        checkInRecordDao.insert(
            CheckInRecordEntity(
                taskId = taskId,
                epochDay = today,
                completedAtMillis = System.currentTimeMillis(),
                elapsedSeconds = elapsedSeconds,
                source = source
            )
        )
        taskDao.setCompleted(taskId, today)
        counts.update { it + (taskId to (it[taskId] ?: 0)) }
    }

    override suspend fun undoComplete(taskId: Long) {
        checkInRecordDao.delete(taskId, DateTimeUtils.todayEpochDay())
        taskDao.setCompleted(taskId, null)
    }

    override suspend fun makeupYesterday(taskId: Long): Boolean {
        if (!allowMakeup()) return false
        val yesterday = DateTimeUtils.todayEpochDay() - 1
        if (checkInRecordDao.find(taskId, yesterday) != null) return false
        checkInRecordDao.insert(
            CheckInRecordEntity(
                taskId = taskId,
                epochDay = yesterday,
                completedAtMillis = System.currentTimeMillis(),
                elapsedSeconds = 0,
                source = "MAKEUP"
            )
        )
        return true
    }

    override suspend fun incrementCount(taskId: Long): Int {
        val today = DateTimeUtils.todayEpochDay()
        val next = timerStateStore.countFor(taskId, today) + 1
        timerStateStore.setCount(taskId, today, next)
        counts.update { it + (taskId to next) }
        val task = getTask(taskId)
        if (task != null && next >= task.targetCount) {
            completeTask(taskId, elapsedSeconds = 0, source = "COUNT")
        }
        return next
    }

    override suspend fun allowMakeup(): Boolean = timerStateStore.allowMakeup.first()

    override suspend fun setAllowMakeup(allow: Boolean) = timerStateStore.setAllowMakeup(allow)

    private fun computeStreak(days: Set<Long>, today: Long): Int {
        if (days.isEmpty()) return 0
        var cursor = if (days.contains(today)) today else today - 1
        var streak = 0
        while (days.contains(cursor)) {
            streak++
            cursor--
        }
        return streak
    }
}

private fun TaskEntity.toDomain(
    streak: Int,
    todayCount: Int,
    completedAtMillis: Long? = null
): Task = Task(
    id = id,
    name = name,
    iconKey = iconKey,
    colorArgb = colorArgb,
    category = category,
    repeatRule = repeatRule,
    note = note,
    reminderMinutes = if (reminderEnabled) reminderMinutes else null,
    createdAt = createdAt,
    completedOnEpochDay = completedOnEpochDay,
    type = type,
    targetCount = targetCount,
    targetDurationSeconds = targetDurationSeconds,
    streak = streak,
    todayCount = todayCount,
    completedAtMillis = completedAtMillis
)

private fun Task.toEntity(): TaskEntity = TaskEntity(
    id = id,
    name = name,
    iconKey = iconKey,
    colorArgb = colorArgb,
    category = category,
    repeatRule = repeatRule,
    note = note,
    reminderEnabled = reminderMinutes != null,
    reminderMinutes = reminderMinutes ?: 0,
    createdAt = createdAt,
    completedOnEpochDay = completedOnEpochDay,
    type = type,
    targetCount = targetCount,
    targetDurationMinutes = targetDurationSeconds / 60,
    targetDurationSeconds = targetDurationSeconds
)
