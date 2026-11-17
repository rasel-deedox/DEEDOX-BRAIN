package com.example.data.repository

import com.example.data.local.HabitCompletionEntity
import com.example.data.local.HabitEntity

data class HabitGamificationSummary(
    val totalXp: Int,
    val level: Int,
    val xpIntoLevel: Int,
    val xpPerLevel: Int,
    val levelProgress: Float,
    val rankTitle: String,
    val bestStreak: Int,
    val activeStreaksCount: Int,
    val totalCompletions: Int,
    val streakBonusXp: Int
)

object HabitGamificationEngine {
    const val XP_PER_COMPLETION = 25
    const val XP_STREAK_BONUS = 50
    const val XP_PER_LEVEL = 250

    fun compute(
        habits: List<HabitEntity>,
        completions: List<HabitCompletionEntity>,
        todayDay: Long = WorkspaceRepository.todayEpochDay()
    ): HabitGamificationSummary {
        val activeHabitIds = habits.map { it.id }.toSet()
        val validCompletions = if (activeHabitIds.isEmpty()) {
            completions
        } else {
            completions.filter { it.habitId in activeHabitIds }
        }
        val completedDaysByHabit = validCompletions.groupBy({ it.habitId }, { it.epochDay })

        var bestStreak = 0
        var activeStreaksCount = 0
        var streakBonusXp = 0

        habits.forEach { habit ->
            val days = completedDaysByHabit[habit.id].orEmpty().toSet()
            val streak = WorkspaceRepository.computeHabitStreak(days, todayDay)
            if (streak > bestStreak) bestStreak = streak
            if (streak > 0) activeStreaksCount++
            // Award +50 XP milestone bonus for each 3-day streak tier achieved per habit
            val bonusTiers = streak / 3
            streakBonusXp += bonusTiers * XP_STREAK_BONUS
        }

        val totalCompletions = validCompletions.size
        val totalXp = (totalCompletions * XP_PER_COMPLETION) + streakBonusXp
        val level = 1 + (totalXp / XP_PER_LEVEL)
        val xpIntoLevel = totalXp % XP_PER_LEVEL
        val levelProgress = (xpIntoLevel.toFloat() / XP_PER_LEVEL.toFloat()).coerceIn(0f, 1f)

        val rankTitle = when {
            level >= 15 -> "Grand Architect"
            level >= 10 -> "Monk Master"
            level >= 6 -> "Deep Work Adept"
            level >= 3 -> "Consistency Builder"
            level >= 2 -> "Focus Apprentice"
            else -> "Seedling"
        }

        return HabitGamificationSummary(
            totalXp = totalXp,
            level = level,
            xpIntoLevel = xpIntoLevel,
            xpPerLevel = XP_PER_LEVEL,
            levelProgress = levelProgress,
            rankTitle = rankTitle,
            bestStreak = bestStreak,
            activeStreaksCount = activeStreaksCount,
            totalCompletions = totalCompletions,
            streakBonusXp = streakBonusXp
        )
    }
}
