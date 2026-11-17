package com.example.data.repository

import androidx.compose.runtime.Immutable
import com.example.data.local.TeamMemberEntity
import com.example.data.local.TeamSharedTaskEntity
import kotlin.math.roundToInt

@Immutable
data class MemberPerformanceStat(
    val uid: String,
    val displayName: String,
    val photoUrl: String,
    val roleTag: String,
    val customStatus: String,
    val assignedCount: Int,
    val completedCount: Int,
    val inProgressCount: Int,
    val todoCount: Int,
    val completionRatio: Float
)

@Immutable
data class DayVelocityStat(
    val dayOfWeek: String,
    val totalCount: Int,
    val doneCount: Int,
    val inProgressCount: Int,
    val todoCount: Int,
    val completionRatio: Float
)

@Immutable
data class TeamAnalyticsSummary(
    val totalTasks: Int = 0,
    val doneTasks: Int = 0,
    val inProgressTasks: Int = 0,
    val todoTasks: Int = 0,
    val backlogTasks: Int = 0,
    val reviewTasks: Int = 0,
    val completionPercent: Int = 0,
    val completionRatio: Float = 0f,
    val memberStats: List<MemberPerformanceStat> = emptyList(),
    val weeklyVelocity: List<DayVelocityStat> = emptyList()
)

object TeamAnalyticsCalculator {

    val TEAM_WEEK_DAYS = listOf("Sat", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri")
    val KANBAN_STATUSES = listOf("backlog", "todo", "in_progress", "review", "done")

    fun statusLabel(status: String): String = when (status) {
        "backlog" -> "Backlog"
        "todo" -> "To Do"
        "in_progress" -> "In Progress"
        "review" -> "Review"
        "done" -> "Done"
        else -> "To Do"
    }

    fun resolvedStatus(task: TeamSharedTaskEntity): String {
        return when {
            task.status == "done" || task.isCompleted -> "done"
            task.status == "review" -> "review"
            task.status == "in_progress" -> "in_progress"
            task.status == "backlog" -> "backlog"
            else -> "todo"
        }
    }

    fun assignedUids(task: TeamSharedTaskEntity): List<String> {
        val parsed = task.assignedToUidsCsv.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
        if (parsed.isNotEmpty()) return parsed.distinct()
        return listOfNotNull(task.assigneeUid.trim().takeIf { it.isNotBlank() })
    }

    fun assignedNames(task: TeamSharedTaskEntity): List<String> {
        val parsed = task.assignedToNamesCsv.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
        if (parsed.isNotEmpty()) return parsed
        return task.assigneeName.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    fun compute(
        members: List<TeamMemberEntity>,
        tasks: List<TeamSharedTaskEntity>
    ): TeamAnalyticsSummary {
        var doneCount = 0
        var inProgressCount = 0
        var todoCount = 0
        var backlogCount = 0
        var reviewCount = 0

        val memberCounters = linkedMapOf<String, IntArray>() // [assigned, done, inProgress, todo]
        members.forEach { memberCounters[it.uid] = IntArray(4) }

        val dayCounters = linkedMapOf<String, IntArray>() // [total, done, inProgress, todo]
        TEAM_WEEK_DAYS.forEach { dayCounters[it] = IntArray(4) }

        tasks.forEach { task ->
            val st = resolvedStatus(task)
            when (st) {
                "done" -> doneCount++
                "review" -> {
                    inProgressCount++
                    reviewCount++
                }
                "in_progress" -> inProgressCount++
                "backlog" -> {
                    todoCount++
                    backlogCount++
                }
                else -> todoCount++
            }

            val dayKey = if (task.dayOfWeek in TEAM_WEEK_DAYS) task.dayOfWeek else "Mon"
            val dArr = dayCounters.getOrPut(dayKey) { IntArray(4) }
            dArr[0]++
            when (st) {
                "done" -> dArr[1]++
                "in_progress", "review" -> dArr[2]++
                else -> dArr[3]++
            }

            assignedUids(task).forEach { uid ->
                val mArr = memberCounters.getOrPut(uid) { IntArray(4) }
                mArr[0]++
                when (st) {
                    "done" -> mArr[1]++
                    "in_progress", "review" -> mArr[2]++
                    else -> mArr[3]++
                }
            }
        }

        val total = tasks.size
        val ratio = if (total > 0) (doneCount.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
        val pct = (ratio * 100f).roundToInt().coerceIn(0, 100)

        val memberByUid = members.associateBy { it.uid }
        val memberStats = memberCounters.entries.mapNotNull { (uid, counts) ->
            val member = memberByUid[uid] ?: return@mapNotNull null
            val assigned = counts[0]
            val done = counts[1]
            MemberPerformanceStat(
                uid = uid,
                displayName = member.displayName,
                photoUrl = member.photoUrl,
                roleTag = member.roleTag.ifBlank { if (member.role == "OWNER") "Team Lead" else "Member" },
                customStatus = member.customStatus.ifBlank { "Available" },
                assignedCount = assigned,
                completedCount = done,
                inProgressCount = counts[2],
                todoCount = counts[3],
                completionRatio = if (assigned > 0) (done.toFloat() / assigned.toFloat()).coerceIn(0f, 1f) else 0f
            )
        }.sortedWith(
            compareByDescending<MemberPerformanceStat> { it.completedCount }
                .thenByDescending { it.assignedCount }
                .thenBy { it.displayName.lowercase() }
        )

        val weeklyVelocity = TEAM_WEEK_DAYS.map { day ->
            val counts = dayCounters[day] ?: IntArray(4)
            val dayTotal = counts[0]
            val dayDone = counts[1]
            DayVelocityStat(
                dayOfWeek = day,
                totalCount = dayTotal,
                doneCount = dayDone,
                inProgressCount = counts[2],
                todoCount = counts[3],
                completionRatio = if (dayTotal > 0) (dayDone.toFloat() / dayTotal.toFloat()).coerceIn(0f, 1f) else 0f
            )
        }

        return TeamAnalyticsSummary(
            totalTasks = total,
            doneTasks = doneCount,
            inProgressTasks = inProgressCount,
            todoTasks = todoCount,
            backlogTasks = backlogCount,
            reviewTasks = reviewCount,
            completionPercent = pct,
            completionRatio = ratio,
            memberStats = memberStats,
            weeklyVelocity = weeklyVelocity
        )
    }
}
