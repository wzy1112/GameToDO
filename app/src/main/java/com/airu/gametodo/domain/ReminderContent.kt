package com.airu.gametodo.domain

import com.airu.gametodo.data.AppDatabase
import com.airu.gametodo.data.TaskType
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * 算「还有什么没做完」。全部做完了就返回 null，不打扰。
 * 返回 标题 to 正文，正文可能多行（按游戏一行）。
 */
object ReminderContent {

    suspend fun daily(db: AppDatabase, now: Long): Pair<String, String>? {
        val games = db.gameDao().getAll().filter { ReminderScheduler.participates(it) }
        if (games.isEmpty()) return null
        val tasks = db.taskDao().getAll().filter { it.type == TaskType.DAILY }
        if (tasks.isEmpty()) return null
        val logs = db.logDao().getAll()

        var total = 0
        val lines = mutableListOf<String>()
        games.forEach { game ->
            val key = Period.keyFor(TaskType.DAILY, game, now)
            val pending = tasks.count { t ->
                t.gameId == game.id && logs.none { it.taskId == t.id && it.periodKey == key }
            }
            if (pending > 0) {
                total += pending
                lines += "${game.name} · $pending 个"
            }
        }
        if (total == 0) return null
        return "今晚还有 $total 个日常没清" to lines.joinToString("\n")
    }

    suspend fun weekly(db: AppDatabase, now: Long): Pair<String, String>? {
        val games = db.gameDao().getAll().filter { ReminderScheduler.participates(it) }
        if (games.isEmpty()) return null
        val tasks = db.taskDao().getAll().filter { it.type == TaskType.WEEKLY }
        if (tasks.isEmpty()) return null
        val logs = db.logDao().getAll()

        var total = 0
        val lines = mutableListOf<String>()
        games.forEach { game ->
            val key = Period.keyFor(TaskType.WEEKLY, game, now)
            val pending = tasks.count { t ->
                t.gameId == game.id && logs.none { it.taskId == t.id && it.periodKey == key }
            }
            if (pending > 0) {
                total += pending
                lines += "${game.name} · $pending 个"
            }
        }
        if (total == 0) return null
        return "本周还有 $total 个周常没做" to lines.joinToString("\n")
    }

    /** 快结束的活动（结束日在 leadDays 天内）里，还没做的活动任务 */
    suspend fun activity(db: AppDatabase, now: Long, leadDays: Int): Pair<String, String>? {
        val today = LocalDate.now()
        val ending = db.activityDao().getAll().filter {
            val left = ChronoUnit.DAYS.between(today, LocalDate.ofEpochDay(it.endEpochDay))
            left in 0..leadDays.toLong()
        }
        if (ending.isEmpty()) return null

        val games = db.gameDao().getAll()
            .filter { ReminderScheduler.participates(it) }
            .associateBy { it.id }
        val onceTasks = db.taskDao().getAll().filter { it.type == TaskType.ONCE }
        val logs = db.logDao().getAll()

        val lines = mutableListOf<String>()
        ending.forEach { activity ->
            if (games[activity.gameId] == null) return@forEach
            val pending = onceTasks.count { t ->
                t.activityId == activity.id && logs.none { it.taskId == t.id }
            }
            if (pending > 0) {
                val left = ChronoUnit.DAYS.between(today, LocalDate.ofEpochDay(activity.endEpochDay))
                val whenText = if (left <= 0) "今天结束" else "还剩 $left 天"
                lines += "${activity.title}（$whenText）· $pending 个没做"
            }
        }
        if (lines.isEmpty()) return null
        return "版本活动快结束了" to lines.joinToString("\n")
    }
}
