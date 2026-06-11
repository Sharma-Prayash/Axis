package com.productivity.app.data.repository

import com.productivity.app.data.dao.WeeklyGoalDao
import com.productivity.app.data.model.WeeklyGoal
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface WeeklyGoalRepository {
    fun getGoalForTrackerAndWeek(trackerId: Long, weekStart: Long): Flow<WeeklyGoal?>
    suspend fun getGoalForTrackerAndWeekSync(trackerId: Long, weekStart: Long): WeeklyGoal?
    fun getGoalsForTracker(trackerId: Long): Flow<List<WeeklyGoal>>
    fun getCurrentWeekGoals(weekStart: Long): Flow<List<WeeklyGoal>>
    suspend fun getCurrentWeekGoalsSync(weekStart: Long): List<WeeklyGoal>
    suspend fun insertGoal(goal: WeeklyGoal): Long
    suspend fun updateGoal(goal: WeeklyGoal)
    suspend fun deleteGoal(goal: WeeklyGoal)
}

@Singleton
class WeeklyGoalRepositoryImpl @Inject constructor(
    private val weeklyGoalDao: WeeklyGoalDao
) : WeeklyGoalRepository {
    override fun getGoalForTrackerAndWeek(trackerId: Long, weekStart: Long) =
        weeklyGoalDao.getGoalForTrackerAndWeek(trackerId, weekStart)

    override suspend fun getGoalForTrackerAndWeekSync(trackerId: Long, weekStart: Long) =
        weeklyGoalDao.getGoalForTrackerAndWeekSync(trackerId, weekStart)

    override fun getGoalsForTracker(trackerId: Long) =
        weeklyGoalDao.getGoalsForTracker(trackerId)

    override fun getCurrentWeekGoals(weekStart: Long) =
        weeklyGoalDao.getCurrentWeekGoals(weekStart)

    override suspend fun getCurrentWeekGoalsSync(weekStart: Long) =
        weeklyGoalDao.getCurrentWeekGoalsSync(weekStart)

    override suspend fun insertGoal(goal: WeeklyGoal) =
        weeklyGoalDao.insert(goal)

    override suspend fun updateGoal(goal: WeeklyGoal) =
        weeklyGoalDao.update(goal)

    override suspend fun deleteGoal(goal: WeeklyGoal) =
        weeklyGoalDao.delete(goal)
}
