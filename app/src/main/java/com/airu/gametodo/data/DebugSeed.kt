package com.airu.gametodo.data

import android.content.Context
import java.time.LocalDate

/** 仅用于本地截图自检，验证完会删掉 */
object DebugSeed {
    suspend fun seed(context: Context) {
        val db = AppDatabase.get(context)
        val today = LocalDate.now()
        val gameId = db.gameDao().insert(
            GameEntity(
                name = "原神",
                packageName = "com.miHoYo.Yuanshen",
                resetMinuteOfDay = 240,
                weekResetDay = 1,
                colorArgb = 0xFF4DD0E1.toInt()
            )
        )
        db.taskDao().insert(TaskEntity(gameId = gameId, title = "每日委托", type = TaskType.DAILY, sortOrder = 0))
        db.taskDao().insert(TaskEntity(gameId = gameId, title = "清树脂", type = TaskType.DAILY, sortOrder = 1))
        db.taskDao().insert(TaskEntity(gameId = gameId, title = "周本全刷", type = TaskType.WEEKLY, sortOrder = 2))

        val summerId = db.activityDao().insert(
            ActivityEntity(
                gameId = gameId,
                title = "夏日海岛",
                startEpochDay = today.minusDays(10).toEpochDay(),
                endEpochDay = today.plusDays(4).toEpochDay(),
                colorArgb = 0xFFFF8A65.toInt()
            )
        )
        db.taskDao().insert(
            TaskEntity(gameId = gameId, title = "收集海螺", type = TaskType.ONCE, activityId = summerId, sortOrder = 0)
        )
        db.taskDao().insert(
            TaskEntity(gameId = gameId, title = "深境螺旋满星", type = TaskType.ONCE, activityId = summerId, sortOrder = 1)
        )

        db.activityDao().insert(
            ActivityEntity(
                gameId = gameId,
                title = "新版本前瞻",
                startEpochDay = today.plusDays(6).toEpochDay(),
                endEpochDay = today.plusDays(30).toEpochDay(),
                colorArgb = 0xFFBA68C8.toInt()
            )
        )
        db.activityDao().insert(
            ActivityEntity(
                gameId = gameId,
                title = "旧活动",
                startEpochDay = today.minusDays(40).toEpochDay(),
                endEpochDay = today.minusDays(6).toEpochDay(),
                colorArgb = 0xFF90A4AE.toInt()
            )
        )
    }
}
