package com.productivity.app.domain.reminder

import java.util.Calendar

/**
 * Lightweight recurrence model for reminders. We deliberately use a small set
 * of human-meaningful rules stored as a plain token in [com.productivity.app
 * .data.model.Reminder.recurrenceRule] rather than full iCal RRULE strings —
 * it covers everything a personal-assistant reminder needs and is trivial to
 * reason about.
 */
object Recurrence {
    const val NONE = ""
    const val DAILY = "DAILY"
    const val WEEKDAYS = "WEEKDAYS" // Monday–Friday
    const val WEEKLY = "WEEKLY"
    const val MONTHLY = "MONTHLY"

    /** Ordered options for pickers (value to display label). */
    val OPTIONS: List<Pair<String, String>> = listOf(
        NONE to "Does not repeat",
        DAILY to "Every day",
        WEEKDAYS to "Weekdays (Mon–Fri)",
        WEEKLY to "Every week",
        MONTHLY to "Every month"
    )

    fun label(rule: String?): String =
        OPTIONS.firstOrNull { it.first == (rule ?: NONE) }?.second ?: "Does not repeat"

    fun isRecurring(rule: String?): Boolean = !rule.isNullOrEmpty()

    /**
     * The next occurrence strictly after [after], preserving the original
     * time-of-day of [base]. Returns null for non-recurring reminders.
     *
     * @param base  The reminder's current scheduled time (defines time-of-day
     *              and, for monthly, the day-of-month).
     * @param after The instant the next occurrence must be later than
     *              (defaults to now).
     */
    fun nextOccurrence(rule: String?, base: Long, after: Long = System.currentTimeMillis()): Long? {
        if (!isRecurring(rule)) return null
        val cal = Calendar.getInstance().apply { timeInMillis = base }
        // Advance at least once, then keep going until strictly in the future.
        var guard = 0
        do {
            advance(cal, rule!!)
            guard++
        } while (cal.timeInMillis <= after && guard < 1000)
        return cal.timeInMillis
    }

    private fun advance(cal: Calendar, rule: String) {
        when (rule) {
            DAILY -> cal.add(Calendar.DAY_OF_YEAR, 1)
            WEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, 1)
            MONTHLY -> cal.add(Calendar.MONTH, 1)
            WEEKDAYS -> {
                cal.add(Calendar.DAY_OF_YEAR, 1)
                while (cal.get(Calendar.DAY_OF_WEEK).let { it == Calendar.SATURDAY || it == Calendar.SUNDAY }) {
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            else -> cal.add(Calendar.DAY_OF_YEAR, 1)
        }
    }
}
