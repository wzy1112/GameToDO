package com.airu.gametodo.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.airu.gametodo.data.ActivityEntity
import com.airu.gametodo.data.AppDatabase
import com.airu.gametodo.data.GameEntity
import com.airu.gametodo.data.LogEntity
import com.airu.gametodo.data.TaskEntity
import com.airu.gametodo.data.TaskType
import com.airu.gametodo.domain.Period
import com.airu.gametodo.widget.WidgetRefresh
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.get(app)

    val games: StateFlow<List<GameEntity>> = db.gameDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val tasks: StateFlow<List<TaskEntity>> = db.taskDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val activities: StateFlow<List<ActivityEntity>> = db.activityDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val logs: StateFlow<List<LogEntity>> = db.logDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ---------- 打卡 ----------

    fun toggleTask(task: TaskEntity, game: GameEntity, now: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            val key = Period.keyFor(task, game, now)
            val existing = db.logDao().find(task.id, key)
            if (existing != null) {
                db.logDao().delete(task.id, key)
            } else {
                db.logDao().insert(LogEntity(taskId = task.id, periodKey = key, doneAt = now))
            }
            WidgetRefresh.trigger(getApplication())
        }
    }

    fun isDone(task: TaskEntity, game: GameEntity, logsNow: List<LogEntity>, now: Long): Boolean {
        val key = Period.keyFor(task, game, now)
        return logsNow.any { it.taskId == task.id && it.periodKey == key }
    }

    // ---------- 游戏 ----------

    fun saveGame(game: GameEntity, onSaved: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = if (game.id == 0L) {
                db.gameDao().insert(game)
            } else {
                db.gameDao().update(game)
                game.id
            }
            WidgetRefresh.trigger(getApplication())
            onSaved(id)
        }
    }

    fun deleteGame(game: GameEntity) {
        viewModelScope.launch {
            db.gameDao().delete(game)
            WidgetRefresh.trigger(getApplication())
        }
    }

    // ---------- 任务 ----------

    fun saveTask(task: TaskEntity) {
        viewModelScope.launch {
            if (task.id == 0L) db.taskDao().insert(task) else db.taskDao().update(task)
            WidgetRefresh.trigger(getApplication())
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            db.taskDao().delete(task)
            WidgetRefresh.trigger(getApplication())
        }
    }

    fun quickAddTask(gameId: Long, title: String, type: TaskType) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val existing = db.taskDao().getAll().filter { it.gameId == gameId }
            db.taskDao().insert(
                TaskEntity(
                    gameId = gameId,
                    title = title.trim(),
                    type = type,
                    sortOrder = existing.size
                )
            )
            WidgetRefresh.trigger(getApplication())
        }
    }

    // ---------- 活动 ----------

    fun saveActivity(activity: ActivityEntity) {
        viewModelScope.launch {
            if (activity.id == 0L) db.activityDao().insert(activity)
            else db.activityDao().update(activity)
            WidgetRefresh.trigger(getApplication())
        }
    }

    fun deleteActivity(id: Long) {
        viewModelScope.launch {
            // 子任务没有外键约束，手动清掉
            db.taskDao().deleteByActivity(id)
            db.activityDao().deleteById(id)
            WidgetRefresh.trigger(getApplication())
        }
    }

    /** 活动连同它的子任务一起保存：新增的插入、删掉的删除、改名的更新 */
    fun saveActivityWithTasks(
        activity: ActivityEntity,
        drafts: List<Pair<Long, String>>
    ) {
        viewModelScope.launch {
            val activityId = if (activity.id == 0L) {
                db.activityDao().insert(activity)
            } else {
                db.activityDao().update(activity)
                activity.id
            }
            val existing = db.taskDao().getAll().filter { it.activityId == activityId }
            val keepIds = drafts.map { it.first }.filter { it > 0L }.toSet()
            existing.filter { it.id !in keepIds }.forEach { db.taskDao().delete(it) }
            drafts.forEachIndexed { index, (taskId, title) ->
                if (taskId == 0L) {
                    db.taskDao().insert(
                        TaskEntity(
                            gameId = activity.gameId,
                            title = title,
                            type = TaskType.ONCE,
                            activityId = activityId,
                            sortOrder = index
                        )
                    )
                } else {
                    existing.firstOrNull { it.id == taskId }?.let {
                        if (it.title != title) db.taskDao().update(it.copy(title = title, sortOrder = index))
                    }
                }
            }
            WidgetRefresh.trigger(getApplication())
        }
    }
}
