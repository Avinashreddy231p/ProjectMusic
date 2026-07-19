package com.mardous.projectmusic.data.local.database.analytics

import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

object TemporalColumns {

    private val monthNames = arrayOf(
        "", "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    private val dayNames = arrayOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")

    data class TemporalResult(
        val timezoneId: String,
        val timezoneOffsetMinutes: Int,
        val startDate: String,
        val startTimeOnly: String,
        val dayOfWeek: String,
        val dayOfMonth: Int,
        val dayOfYear: Int,
        val weekOfYear: Int,
        val month: Int,
        val monthName: String,
        val quarter: Int,
        val year: Int,
        val yearMonth: String,
        val yearWeek: String,
        val hour: Int,
        val minute: Int,
        val second: Int,
        val timePeriod: String,
        val isWeekend: Boolean
    )

    fun compute(epochMs: Long): TemporalResult {
        val zone = ZoneId.systemDefault()
        val instant = Instant.ofEpochMilli(epochMs)
        val zonedDateTime = instant.atZone(zone)
        val offset: ZoneOffset = zone.rules.getOffset(instant)

        val year = zonedDateTime.year
        val month = zonedDateTime.monthValue
        val dayOfMonth = zonedDateTime.dayOfMonth
        val dayOfYear = zonedDateTime.dayOfYear
        val hour = zonedDateTime.hour
        val minute = zonedDateTime.minute
        val second = zonedDateTime.second
        val dayOfWeekValue = zonedDateTime.dayOfWeek.value % 7

        val weekOfYear = zonedDateTime.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear())

        val timePeriod = when (hour) {
            in 0..5 -> "Early Morning"
            in 6..11 -> "Morning"
            in 12..16 -> "Afternoon"
            in 17..20 -> "Evening"
            else -> "Night"
        }

        val isWeekend = dayOfWeekValue == 0 || dayOfWeekValue == 6

        val startDate = "%04d-%02d-%02d".format(year, month, dayOfMonth)
        val startTimeOnly = "%02d:%02d:%02d".format(hour, minute, second)
        val yearMonth = "%04d-%02d".format(year, month)
        val yearWeek = "%04d-W%02d".format(year, weekOfYear)

        return TemporalResult(
            timezoneId = zone.id,
            timezoneOffsetMinutes = offset.totalSeconds / 60,
            startDate = startDate,
            startTimeOnly = startTimeOnly,
            dayOfWeek = dayNames[dayOfWeekValue],
            dayOfMonth = dayOfMonth,
            dayOfYear = dayOfYear,
            weekOfYear = weekOfYear,
            month = month,
            monthName = monthNames[month],
            quarter = (month - 1) / 3 + 1,
            year = year,
            yearMonth = yearMonth,
            yearWeek = yearWeek,
            hour = hour,
            minute = minute,
            second = second,
            timePeriod = timePeriod,
            isWeekend = isWeekend
        )
    }
}
