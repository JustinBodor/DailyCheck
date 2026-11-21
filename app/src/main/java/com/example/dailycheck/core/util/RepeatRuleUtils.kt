package com.example.dailycheck.core.util

import com.example.dailycheck.data.model.RepeatRule
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZonedDateTime

object RepeatRuleUtils {

    /** 该规则在指定日期是否需要打卡 */
    fun isActiveOn(rule: RepeatRule, date: LocalDate): Boolean = when (rule) {
        is RepeatRule.Daily -> true

        is RepeatRule.Weekly -> rule.daysOfWeek.contains(date.dayOfWeek)

        is RepeatRule.Monthly -> {
            // 31 日的规则在 2 月会落到当月最后一天
            val targetDay = rule.dayOfMonth.coerceIn(1, date.lengthOfMonth())
            date.dayOfMonth == targetDay
        }

        is RepeatRule.EveryNDays -> {
            val interval = rule.intervalDays.coerceAtLeast(1)
            val diff = date.toEpochDay() - rule.anchorEpochDay
            diff >= 0 && diff % interval == 0L
        }
    }

    /** 计算下一次提醒触发时刻（严格晚于 now） */
    fun nextTrigger(rule: RepeatRule, minutesOfDay: Int, now: ZonedDateTime): ZonedDateTime {
        val safeMinutes = minutesOfDay.coerceIn(0, 24 * 60 - 1)
        val time = LocalTime.of(safeMinutes / 60, safeMinutes % 60)
        val today = now.toLocalDate()

        val todayCandidate = LocalDateTime.of(today, time).atZone(now.zone)
        if (todayCandidate.isAfter(now) && isActiveOn(rule, today)) {
            return todayCandidate
        }

        for (offset in 1..400) {
            val date = today.plusDays(offset.toLong())
            if (!isActiveOn(rule, date)) continue
            val candidate = LocalDateTime.of(date, time).atZone(now.zone)
            if (candidate.isAfter(now)) return candidate
        }

        // 兜底：明天同一时刻
        return LocalDateTime.of(today.plusDays(1), time).atZone(now.zone)
    }

    /** 规则的人类可读描述 */
    fun describe(rule: RepeatRule): String = when (rule) {
        is RepeatRule.Daily -> "每天"

        is RepeatRule.Weekly -> {
            val labels = rule.daysOfWeek.sortedBy { it.value }.map { dayLabel(it) }
            if (labels.size >= 7) "每天" else "每周 " + labels.joinToString("、")
        }

        is RepeatRule.Monthly -> "每月 ${rule.dayOfMonth} 日"

        is RepeatRule.EveryNDays -> "每 ${rule.intervalDays} 天"
    }

    private fun dayLabel(day: DayOfWeek): String = when (day) {
        DayOfWeek.MONDAY -> "一"
        DayOfWeek.TUESDAY -> "二"
        DayOfWeek.WEDNESDAY -> "三"
        DayOfWeek.THURSDAY -> "四"
        DayOfWeek.FRIDAY -> "五"
        DayOfWeek.SATURDAY -> "六"
        DayOfWeek.SUNDAY -> "日"
    }
}
