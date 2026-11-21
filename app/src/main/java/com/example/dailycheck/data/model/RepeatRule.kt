package com.example.dailycheck.data.model

import java.time.DayOfWeek

/**
 * P0 支持的四种重复规则。
 * EveryNDays 用 anchorEpochDay 作为“第 0 天”锚点，避免每次打开 App 都重置周期。
 */
sealed interface RepeatRule {

    data object Daily : RepeatRule

    data class Weekly(val daysOfWeek: Set<DayOfWeek>) : RepeatRule

    data class Monthly(val dayOfMonth: Int) : RepeatRule

    data class EveryNDays(val intervalDays: Int, val anchorEpochDay: Long) : RepeatRule
}
