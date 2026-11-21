package com.example.dailycheck.data.local.converter

import androidx.room.TypeConverter
import com.example.dailycheck.data.model.RepeatRule
import com.example.dailycheck.data.model.TaskType
import java.time.DayOfWeek

/**
 * 重复规则的本地序列化格式：
 * daily / weekly:1,3,5 / monthly:15 / interval:3:20395
 */
class Converters {

    @TypeConverter
    fun repeatRuleToString(rule: RepeatRule): String = when (rule) {
        is RepeatRule.Daily -> "daily"
        is RepeatRule.Weekly -> "weekly:" + rule.daysOfWeek
            .sortedBy { it.value }
            .joinToString(",") { it.value.toString() }
        is RepeatRule.Monthly -> "monthly:${rule.dayOfMonth}"
        is RepeatRule.EveryNDays -> "interval:${rule.intervalDays}:${rule.anchorEpochDay}"
    }

    @TypeConverter
    fun stringToRepeatRule(raw: String): RepeatRule {
        val parts = raw.split(":")
        return when (parts.firstOrNull()) {
            "weekly" -> {
                val days = parts.getOrNull(1).orEmpty()
                    .split(",")
                    .mapNotNull { it.toIntOrNull() }
                    .mapNotNull { value -> DayOfWeek.entries.firstOrNull { it.value == value } }
                    .toSet()
                RepeatRule.Weekly(days)
            }

            "monthly" -> RepeatRule.Monthly(parts.getOrNull(1)?.toIntOrNull() ?: 1)

            "interval" -> RepeatRule.EveryNDays(
                intervalDays = parts.getOrNull(1)?.toIntOrNull()?.coerceAtLeast(1) ?: 1,
                anchorEpochDay = parts.getOrNull(2)?.toLongOrNull() ?: 0L
            )

            else -> RepeatRule.Daily
        }
    }

    @TypeConverter
    fun taskTypeToString(type: TaskType): String = type.name

    @TypeConverter
    fun stringToTaskType(raw: String): TaskType =
        TaskType.entries.firstOrNull { it.name == raw } ?: TaskType.COMPLETE
}
