package com.example.dailycheck.ui.screens.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dailycheck.data.repository.StatsRepository
import com.example.dailycheck.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class StatsUiState(
    val totalCheckIns: Int = 0,
    val currentStreak: Int = 0,
    /** 当前自然月：日期 -> 当天完成次数 */
    val dailyCounts: Map<LocalDate, Int> = emptyMap(),
    /** 当前自然月：日期 -> 当天完成任务的配色（按完成先后） */
    val dailyTaskColors: Map<LocalDate, List<Long>> = emptyMap(),
    /** 当前自然月：日期 -> 当天完成任务的图标（与颜色一一对应） */
    val dailyTaskIcons: Map<LocalDate, List<String>> = emptyMap(),
    /** 当前自然月：日期 -> 当天完成任务的名称（与颜色一一对应） */
    val dailyTaskNames: Map<LocalDate, List<String>> = emptyMap(),
    val startDate: LocalDate = LocalDate.now().with(TemporalAdjusters.firstDayOfMonth()),
    val endDate: LocalDate = LocalDate.now()
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    repository: StatsRepository,
    taskRepository: TaskRepository
) : ViewModel() {

    private val today: LocalDate = LocalDate.now()
    private val startDate: LocalDate = today.with(TemporalAdjusters.firstDayOfMonth())
    private val fromEpochDay: Long = startDate.toEpochDay()
    private val toEpochDay: Long = today.toEpochDay()

    val uiState: StateFlow<StatsUiState> = combine(
        repository.observeTotalCheckIns(),
        repository.observeDailyCounts(fromEpochDay, toEpochDay),
        repository.observeAllDays(),
        repository.observeRecordsBetween(fromEpochDay, toEpochDay),
        taskRepository.observeTasks()
    ) { total, counts, allDays, records, tasks ->
        val tasksById = tasks.associateBy { it.id }

        val colorsByDay = mutableMapOf<LocalDate, MutableList<Long>>()
        val iconsByDay = mutableMapOf<LocalDate, MutableList<String>>()
        val namesByDay = mutableMapOf<LocalDate, MutableList<String>>()
        records.forEach { record ->
            val task = tasksById[record.taskId] ?: return@forEach
            val date = LocalDate.ofEpochDay(record.epochDay)
            colorsByDay.getOrPut(date) { mutableListOf() }.add(task.colorArgb)
            iconsByDay.getOrPut(date) { mutableListOf() }.add(task.iconKey)
            namesByDay.getOrPut(date) { mutableListOf() }.add(task.name)
        }

        StatsUiState(
            totalCheckIns = total,
            currentStreak = currentStreak(allDays, today),
            dailyCounts = counts.associate { LocalDate.ofEpochDay(it.epochDay) to it.count },
            dailyTaskColors = colorsByDay,
            dailyTaskIcons = iconsByDay,
            dailyTaskNames = namesByDay,
            startDate = startDate,
            endDate = today
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StatsUiState(startDate = startDate, endDate = today)
    )

    /** 以今天（或昨天）为终点，向前数连续打卡天数 */
    private fun currentStreak(days: List<Long>, today: LocalDate): Int {
        if (days.isEmpty()) return 0

        val daySet = days.map { LocalDate.ofEpochDay(it) }.toSortedSet(reverseOrder())
        val newest = daySet.first()
        val todayEpoch = today.toEpochDay()

        val cursor: LocalDate = when (newest.toEpochDay()) {
            todayEpoch -> today
            todayEpoch - 1 -> newest
            else -> return 0
        }

        var streak = 0
        var check: LocalDate = cursor
        while (daySet.contains(check)) {
            streak++
            check = check.minusDays(1)
        }
        return streak
    }
}
