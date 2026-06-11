package com.productivity.app.data.dao

import androidx.room.*
import com.productivity.app.data.model.WeeklyGoal
import kotlinx.coroutines.flow.Flow

@Dao
interface WeeklyGoalDao {
    @Query("SELECT * FROM weekly_goal WHERE tracker_id = :trackerId AND week_start_date = :weekStart LIMIT 1")
    fun getGoalForTrackerAndWeek(trackerId: Long, weekStart: Long): Flow<WeeklyGoal?>

    @Query("SELECT * FROM weekly_goal WHERE tracker_id = :trackerId AND week_start_date = :weekStart LIMIT 1")
    suspend fun getGoalForTrackerAndWeekSync(trackerId: Long, weekStart: Long): WeeklyGoal?

    @Query("SELECT * FROM weekly_goal WHERE tracker_id = :trackerId ORDER BY week_start_date DESC")
    fun getGoalsForTracker(trackerId: Long): Flow<List<WeeklyGoal>>

    @Query("SELECT * FROM weekly_goal WHERE week_start_date = :weekStart")
    fun getCurrentWeekGoals(weekStart: Long): Flow<List<WeeklyGoal>>

    @Query("SELECT * FROM weekly_goal WHERE week_start_date = :weekStart")
    suspend fun getCurrentWeekGoalsSync(weekStart: Long): List<WeeklyGoal>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: WeeklyGoal): Long

    @Update
    suspend fun update(goal: WeeklyGoal)

    @Delete
    suspend fun delete(goal: WeeklyGoal)
}
