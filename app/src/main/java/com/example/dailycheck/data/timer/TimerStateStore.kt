package com.example.dailycheck.data.timer

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** 计时器持久化状态：进程被杀后据此恢复。 */
data class TimerState(
    val taskId: Long,
    val targetSeconds: Int,
    /** 已经累计的秒数（不含当前运行段） */
    val accumulatedSeconds: Int,
    /** 当前运行段开始的时间戳；暂停时为 0 */
    val startedAtMillis: Long,
    val running: Boolean
) {

    fun elapsedSeconds(nowMillis: Long = System.currentTimeMillis()): Int =
        accumulatedSeconds + if (running && startedAtMillis > 0L) {
            ((nowMillis - startedAtMillis) / 1000L).toInt().coerceAtLeast(0)
        } else {
            0
        }

    fun remainingSeconds(nowMillis: Long = System.currentTimeMillis()): Int =
        (targetSeconds - elapsedSeconds(nowMillis)).coerceAtLeast(0)
}

/** 免打扰区间（默认 23:00–07:00，可在设置中改） */
data class QuietHours(
    val enabled: Boolean = true,
    val startMinutes: Int = 23 * 60,
    val endMinutes: Int = 7 * 60
) {
    fun contains(minutesOfDay: Int): Boolean {
        if (!enabled) return false
        return if (startMinutes <= endMinutes) {
            minutesOfDay in startMinutes until endMinutes
        } else {
            minutesOfDay >= startMinutes || minutesOfDay < endMinutes
        }
    }
}

private val Context.timerDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "daily_check_timer"
)

@Singleton
class TimerStateStore @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    private val store: DataStore<Preferences> get() = context.timerDataStore

    val state: Flow<TimerState?> = store.data.map { prefs ->
        val taskId = prefs[KEY_TASK_ID] ?: return@map null
        TimerState(
            taskId = taskId,
            targetSeconds = prefs[KEY_TARGET] ?: 0,
            accumulatedSeconds = prefs[KEY_ACCUMULATED] ?: 0,
            startedAtMillis = prefs[KEY_STARTED_AT] ?: 0L,
            running = prefs[KEY_RUNNING] ?: false
        )
    }

    suspend fun current(): TimerState? = state.first()

    suspend fun save(state: TimerState) {
        store.edit { prefs ->
            prefs[KEY_TASK_ID] = state.taskId
            prefs[KEY_TARGET] = state.targetSeconds
            prefs[KEY_ACCUMULATED] = state.accumulatedSeconds
            prefs[KEY_STARTED_AT] = state.startedAtMillis
            prefs[KEY_RUNNING] = state.running
        }
    }

    suspend fun clear() {
        store.edit { prefs ->
            prefs.remove(KEY_TASK_ID)
            prefs.remove(KEY_TARGET)
            prefs.remove(KEY_ACCUMULATED)
            prefs.remove(KEY_STARTED_AT)
            prefs.remove(KEY_RUNNING)
        }
    }

    /** 设置：允许补打卡，默认开启 */
    val allowMakeup: Flow<Boolean> = store.data.map { it[KEY_ALLOW_MAKEUP] ?: true }

    suspend fun setAllowMakeup(allow: Boolean) {
        store.edit { it[KEY_ALLOW_MAKEUP] = allow }
    }

    val quietHours: Flow<QuietHours> = store.data.map { prefs ->
        QuietHours(
            enabled = prefs[KEY_QUIET_ENABLED] ?: true,
            startMinutes = prefs[KEY_QUIET_START] ?: (23 * 60),
            endMinutes = prefs[KEY_QUIET_END] ?: (7 * 60)
        )
    }

    suspend fun setQuietEnabled(enabled: Boolean) {
        store.edit { it[KEY_QUIET_ENABLED] = enabled }
    }

    suspend fun setQuietStart(minutes: Int) {
        store.edit { it[KEY_QUIET_START] = minutes }
    }

    suspend fun setQuietEnd(minutes: Int) {
        store.edit { it[KEY_QUIET_END] = minutes }
    }

    /** 计数型任务当天进度：{ taskId -> count } */
    suspend fun readCounts(epochDay: Long): Map<Long, Int> {
        val prefs = store.data.first()
        return prefs.asMap()
            .mapNotNull { (key, value) ->
                val name = key.name
                if (!name.startsWith(COUNT_PREFIX)) return@mapNotNull null
                val taskId = name.removePrefix(COUNT_PREFIX).toLongOrNull() ?: return@mapNotNull null
                val raw = (value as? String) ?: return@mapNotNull null
                val parts = raw.split(":")
                val day = parts.getOrNull(0)?.toLongOrNull() ?: return@mapNotNull null
                val count = parts.getOrNull(1)?.toIntOrNull() ?: 0
                if (day == epochDay) taskId to count else null
            }
            .toMap()
    }

    suspend fun setCount(taskId: Long, epochDay: Long, count: Int) {
        store.edit { it[stringPreferencesKey(COUNT_PREFIX + taskId)] = "$epochDay:$count" }
    }

    suspend fun countFor(taskId: Long, epochDay: Long): Int = readCounts(epochDay)[taskId] ?: 0

    private companion object {
        val KEY_TASK_ID = longPreferencesKey("timer_task_id")
        val KEY_TARGET = intPreferencesKey("timer_target_seconds")
        val KEY_ACCUMULATED = intPreferencesKey("timer_accumulated_seconds")
        val KEY_STARTED_AT = longPreferencesKey("timer_started_at")
        val KEY_RUNNING = booleanPreferencesKey("timer_running")
        val KEY_ALLOW_MAKEUP = booleanPreferencesKey("settings_allow_makeup")
        val KEY_QUIET_ENABLED = booleanPreferencesKey("settings_quiet_enabled")
        val KEY_QUIET_START = intPreferencesKey("settings_quiet_start")
        val KEY_QUIET_END = intPreferencesKey("settings_quiet_end")
        const val COUNT_PREFIX = "count_"
    }
}
