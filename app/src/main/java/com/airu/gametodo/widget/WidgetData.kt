package com.airu.gametodo.widget

import android.content.Context
import com.airu.gametodo.data.AppDatabase
import com.airu.gametodo.data.LogEntity
import com.airu.gametodo.data.TaskType
import com.airu.gametodo.domain.Period
import java.time.LocalDate

data class WidgetItem(
    val taskId: Long,
    val title: String,
    val gameName: String,
    val colorArgb: Int,
    val packageName: String?,
    val type: TaskType
)

/** 小组件和 App 共用的取数逻辑，避免两边算法漂移 */
object WidgetData {

    /** 今天还没勾的活儿，日常 → 周常 → 活动，同类型按游戏顺序 */
    suspend fun loadItems(context: Context): List<WidgetItem> {
        val db = AppDatabase.get(context)
        val games = db.gameDao().getAll()
        val tasks = db.taskDao().getAll()
        val logs = db.logDao().getAll()
        val now = System.currentTimeMillis()
        val today = LocalDate.now()

        // 活动任务只在活动期间露出来，活动结束自动消失
        val activeActivityIds = db.activityDao().getAll().filter {
            !today.isBefore(LocalDate.ofEpochDay(it.startEpochDay)) &&
                !today.isAfter(LocalDate.ofEpochDay(it.endEpochDay))
        }.map { it.id }.toSet()

        val pending = mutableListOf<Triple<Int, WidgetItem, Int>>()
        games.forEachIndexed { gameIndex, game ->
            tasks.filter { it.gameId == game.id }.forEach { task ->
                if (task.type == TaskType.ONCE && task.activityId !in activeActivityIds) return@forEach
                val key = Period.keyFor(task, game, now)
                if (logs.any { it.taskId == task.id && it.periodKey == key }) return@forEach
                val rank = when (task.type) {
                    TaskType.DAILY -> 0
                    TaskType.WEEKLY -> 1
                    TaskType.ONCE -> 2
                }
                pending += Triple(
                    rank,
                    WidgetItem(
                        task.id, task.title, game.name, game.colorArgb, game.packageName, task.type
                    ),
                    gameIndex
                )
            }
        }
        return pending.sortedWith(compareBy({ it.first }, { it.third })).map { it.second }
    }

    /** 勾选/取消勾选，跟 App 里点圆圈是同一套周期标记 */
    suspend fun toggle(context: Context, taskId: Long) {
        if (taskId <= 0L) return
        val db = AppDatabase.get(context)
        val task = db.taskDao().getById(taskId) ?: return
        val game = db.gameDao().getById(task.gameId) ?: return
        val now = System.currentTimeMillis()
        val key = Period.keyFor(task, game, now)
        val existing = db.logDao().find(taskId, key)
        if (existing == null) {
            db.logDao().insert(LogEntity(taskId = taskId, periodKey = key, doneAt = now))
        } else {
            db.logDao().delete(taskId, key)
        }
    }
}
