package com.airu.gametodo.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** 任务类型：日常每天重置 / 周常每周重置 / 一次性（多用于版本活动） */
enum class TaskType { DAILY, WEEKLY, ONCE }

@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** 绑定的应用包名，为空表示只记录不启动 */
    val packageName: String? = null,
    /** 应用图标 PNG 字节，避免每次从 PackageManager 取 */
    val icon: ByteArray? = null,
    /** 游戏刷新时刻，用「当天第几分钟表示」，240 = 04:00 */
    val resetMinuteOfDay: Int = 240,
    /** 周常刷新的星期几，1=周一 … 7=周日 */
    val weekResetDay: Int = 1,
    val colorArgb: Int = 0xFF4DD0E1.toInt(),
    val sortOrder: Int = 0,
    val reminderEnabled: Boolean = true
) {
    override fun equals(other: Any?): Boolean = other is GameEntity && other.id == id &&
        other.name == name && other.packageName == packageName &&
        other.resetMinuteOfDay == resetMinuteOfDay && other.weekResetDay == weekResetDay &&
        other.colorArgb == colorArgb && other.sortOrder == sortOrder &&
        other.reminderEnabled == reminderEnabled

    override fun hashCode(): Int = id.hashCode()
}

@Entity(
    tableName = "tasks",
    foreignKeys = [ForeignKey(
        entity = GameEntity::class,
        parentColumns = ["id"],
        childColumns = ["gameId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("gameId")]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gameId: Long,
    val title: String,
    val type: TaskType = TaskType.DAILY,
    val note: String = "",
    /** ONCE 型任务所属的活动，可为空 */
    val activityId: Long? = null,
    val sortOrder: Int = 0,
    val archived: Boolean = false
)

@Entity(
    tableName = "activities",
    foreignKeys = [ForeignKey(
        entity = GameEntity::class,
        parentColumns = ["id"],
        childColumns = ["gameId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("gameId")]
)
data class ActivityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gameId: Long,
    val title: String,
    /** 起始日，用 epochDay 存，避开时区问题 */
    val startEpochDay: Long,
    val endEpochDay: Long,
    val colorArgb: Int = 0xFFFF8A65.toInt(),
    val note: String = ""
)

@Entity(
    tableName = "logs",
    foreignKeys = [ForeignKey(
        entity = TaskEntity::class,
        parentColumns = ["id"],
        childColumns = ["taskId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [
        Index("taskId"),
        Index(value = ["taskId", "periodKey"], unique = true)
    ]
)
data class LogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    /** 周期 key，例如 D2026-11-26 / W2026-11-23 / ONCE */
    val periodKey: String,
    val doneAt: Long = System.currentTimeMillis()
)
