package com.example.dailycheck.core.util

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateTimeUtils {

    /** 免打扰默认区间：23:00 - 07:00（P0 只做默认值，设置页留占位） */
    const val QUIET_START_MINUTES: Int = 23 * 60
    const val QUIET_END_MINUTES: Int = 7 * 60

    private val todayFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.CHINA)

    fun today(): LocalDate = LocalDate.now()

    fun todayEpochDay(): Long = LocalDate.now().toEpochDay()

    fun toLocalDate(epochDay: Long): LocalDate = LocalDate.ofEpochDay(epochDay)

    fun formatToday(): String = LocalDate.now().format(todayFormatter)

    /** 分钟数 -> "HH:mm" */
    fun formatMinutes(minutesOfDay: Int): String {
        val dayMinutes = 24 * 60
        val normalized = ((minutesOfDay % dayMinutes) + dayMinutes) % dayMinutes
        return String.format(Locale.getDefault(), "%02d:%02d", normalized / 60, normalized % 60)
    }

    fun currentMinutesOfDay(): Int {
        val now = LocalTime.now()
        return now.hour * 60 + now.minute
    }

    fun isInQuietHours(minutesOfDay: Int): Boolean =
        minutesOfDay >= QUIET_START_MINUTES || minutesOfDay < QUIET_END_MINUTES
}
