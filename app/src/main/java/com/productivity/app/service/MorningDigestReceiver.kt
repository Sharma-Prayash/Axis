package com.productivity.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.productivity.app.data.db.AppDatabase
import com.productivity.app.data.preferences.PersonalManagerPreferences
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MorningDigestReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("MorningDigestReceiver", "Daily digest alarm triggered")
        
        val pendingResult = goAsync()
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstanceForWorker(context)
                val prefs = PersonalManagerPreferences(context)
                val notificationHelper = NotificationHelper(context)
                val alarmHelper = AlarmManagerHelper(context)
                
                // 1. Reschedule next daily digest alarm
                if (prefs.digestEnabled) {
                    alarmHelper.scheduleDailyDigest(prefs.digestHour, prefs.digestMinute)
                }
                
                // 2. Query data for digest
                val todayStart = getStartOfDayMillis()
                val todayEnd = getEndOfDayMillis()
                val weekStart = getStartOfWeekMillis()
                
                val reminders = db.reminderDao().getRemindersForDay(todayStart, todayEnd).first()
                val events = db.scheduleEventDao().getEventsForDay(todayStart, todayEnd).first()
                
                // Trackers
                val activeTrackers = db.progressTrackerDao().getActiveTrackers().first()
                val trackerSummaries = mutableListOf<String>()
                for (tracker in activeTrackers) {
                    val goal = db.weeklyGoalDao().getGoalForTrackerAndWeekSync(tracker.id, weekStart)
                    if (goal != null) {
                        trackerSummaries.add("${tracker.title}: ${goal.achievedCount}/${goal.targetCount} completed this week")
                    }
                }
                
                // Checklists
                val activeChecklists = db.checklistDao().getActiveChecklists().first()
                var pendingChecklistCount = 0
                for (checklist in activeChecklists) {
                    val items = db.checklistItemDao().getItemsForChecklist(checklist.id).first()
                    val hasUnchecked = items.any { !it.isChecked }
                    if (hasUnchecked) {
                        pendingChecklistCount++
                    }
                }
                
                // 3. Construct message summary
                val summaryBuilder = StringBuilder()
                
                if (reminders.isNotEmpty()) {
                    summaryBuilder.append("Reminders today (${reminders.size}):\n")
                    reminders.forEach { summaryBuilder.append("• ${it.title}\n") }
                } else {
                    summaryBuilder.append("No reminders for today.\n")
                }
                summaryBuilder.append("\n")
                
                if (events.isNotEmpty()) {
                    summaryBuilder.append("Events today (${events.size}):\n")
                    val timeFormatter = SimpleDateFormat("h:mm a", Locale.getDefault())
                    events.forEach { 
                        val timeStr = timeFormatter.format(Date(it.startDatetime))
                        summaryBuilder.append("• ${it.title} at $timeStr\n") 
                    }
                } else {
                    summaryBuilder.append("No events scheduled for today.\n")
                }
                summaryBuilder.append("\n")
                
                if (trackerSummaries.isNotEmpty()) {
                    summaryBuilder.append("Weekly Goals:\n")
                    trackerSummaries.forEach { summaryBuilder.append("• $it\n") }
                } else {
                    summaryBuilder.append("No weekly goals set.\n")
                }
                summaryBuilder.append("\n")
                
                if (pendingChecklistCount > 0) {
                    summaryBuilder.append("You have $pendingChecklistCount checklist(s) with pending items.\n")
                } else {
                    summaryBuilder.append("Checklists are all completed!\n")
                }
                
                val notificationText = summaryBuilder.toString().trim()
                notificationHelper.showMorningDigestNotification(notificationText)
                
            } catch (e: Exception) {
                Log.e("MorningDigestReceiver", "Error building daily digest", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
    
    private fun getStartOfDayMillis(): Long {
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun getEndOfDayMillis(): Long {
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 23)
            set(java.util.Calendar.MINUTE, 59)
            set(java.util.Calendar.SECOND, 59)
            set(java.util.Calendar.MILLISECOND, 999)
        }
        return cal.timeInMillis
    }
    
    private fun getStartOfWeekMillis(): Long {
        val cal = java.util.Calendar.getInstance().apply {
            firstDayOfWeek = java.util.Calendar.MONDAY
            set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }
}
