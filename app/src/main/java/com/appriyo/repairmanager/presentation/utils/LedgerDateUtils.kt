// app/src/main/java/com/appriyo/repairmanager/presentation/utils/LedgerDateUtils.kt
package com.appriyo.repairmanager.presentation.utils

import com.appriyo.repairmanager.presentation.state.LedgerDateFilter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Pure date-math and formatting helpers for the Daily Work Ledger.
 * No Android/Firestore dependencies - safe to reuse from both the
 * Repository (local filtering) and the ViewModel/UI (labels).
 */
object LedgerDateUtils {

    private const val ONE_DAY_MILLIS = 24L * 60 * 60 * 1000

    fun startOfDay(date: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }

    fun endOfDay(date: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.time
    }

    fun addDays(date: Date, days: Int): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.add(Calendar.DAY_OF_MONTH, days)
        return calendar.time
    }

    fun startOfWeek(date: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.time = date
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        return startOfDay(calendar.time)
    }

    fun endOfWeek(date: Date): Date {
        val start = startOfWeek(date)
        return endOfDay(addDays(start, 6))
    }

    fun startOfMonth(date: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        return startOfDay(calendar.time)
    }

    fun endOfMonth(date: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        return endOfDay(calendar.time)
    }

    fun isSameDay(d1: Date, d2: Date): Boolean {
        val c1 = Calendar.getInstance().apply { time = d1 }
        val c2 = Calendar.getInstance().apply { time = d2 }
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
    }

    fun daysBetween(from: Date, to: Date): Int {
        val start = startOfDay(from).time
        val end = startOfDay(to).time
        return ((end - start) / ONE_DAY_MILLIS).toInt()
    }

    /** Converts a UTC-midnight millis value from a Material3 DatePicker into a local noon Date. */
    fun utcMillisToLocalDate(utcMillis: Long): Date {
        val utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        utcCalendar.timeInMillis = utcMillis
        val localCalendar = Calendar.getInstance()
        localCalendar.set(
            utcCalendar.get(Calendar.YEAR),
            utcCalendar.get(Calendar.MONTH),
            utcCalendar.get(Calendar.DAY_OF_MONTH),
            12, 0, 0
        )
        localCalendar.set(Calendar.MILLISECOND, 0)
        return localCalendar.time
    }

    /** "Today • 3:45 PM" / "Yesterday • 5:12 PM" / "Mon • 1:20 PM" / "24 Jun • 1:20 PM" */
    fun formatEntryTimestamp(date: Date?): String {
        val safeDate = date ?: Date()
        val now = Date()
        val timeText = SimpleDateFormat("h:mm a", Locale.getDefault()).format(safeDate)
        val dayDiff = daysBetween(safeDate, now)

        val dayLabel = when {
            isSameDay(safeDate, now) -> "Today"
            dayDiff == 1 -> "Yesterday"
            dayDiff in 2..6 -> SimpleDateFormat("EEE", Locale.getDefault()).format(safeDate)
            else -> {
                val sameYear = Calendar.getInstance().apply { time = safeDate }.get(Calendar.YEAR) ==
                        Calendar.getInstance().apply { time = now }.get(Calendar.YEAR)
                val pattern = if (sameYear) "d MMM" else "d MMM yyyy"
                SimpleDateFormat(pattern, Locale.getDefault()).format(safeDate)
            }
        }
        return "$dayLabel • $timeText"
    }

    fun formatFilterHeader(
        filter: LedgerDateFilter,
        customDate: Date,
        rangeStart: Date?,
        rangeEnd: Date?
    ): String {
        val fullDate = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
        val shortDate = SimpleDateFormat("d MMM", Locale.getDefault())
        val monthYear = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

        return when (filter) {
            LedgerDateFilter.TODAY -> "Today • ${fullDate.format(Date())}"
            LedgerDateFilter.YESTERDAY -> "Yesterday • ${fullDate.format(addDays(Date(), -1))}"
            LedgerDateFilter.THIS_WEEK -> {
                val start = startOfWeek(Date())
                val end = endOfWeek(Date())
                "This Week • ${shortDate.format(start)} - ${shortDate.format(end)}"
            }
            LedgerDateFilter.THIS_MONTH -> "This Month • ${monthYear.format(Date())}"
            LedgerDateFilter.CUSTOM_DATE -> fullDate.format(customDate)
            LedgerDateFilter.CUSTOM_RANGE -> {
                val start = rangeStart ?: customDate
                val end = rangeEnd ?: customDate
                "${shortDate.format(start)} - ${fullDate.format(end)}"
            }
        }
    }
}