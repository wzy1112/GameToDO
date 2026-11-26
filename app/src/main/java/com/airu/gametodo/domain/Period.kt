package com.airu.gametodo.domain

import com.airu.gametodo.data.GameEntity
import com.airu.gametodo.data.TaskEntity
import com.airu.gametodo.data.TaskType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * 「游戏日 / 游戏周」换算。
 *
 * 核心思路：不存「今天做没做」的布尔值，而是给每条完成记录打一个周期 key。
 * 日 key = D2026-11-26，周 key = W2026-11-23（该周起始日），一次性 = ONCE。
 * 这样历史天然可回溯，日历直接读表即可。
 */
object Period {

    const val ONCE_KEY = "ONCE"

    /** 把真实时间折算成游戏日：减掉刷新时刻的偏移后取日期。
     *  例：刷新 04:00，11/26 凌晨 02:30 → 11/25 的游戏日。 */
    fun gameDay(
        nowMillis: Long,
        resetMinuteOfDay: Int,
        zone: ZoneId = ZoneId.systemDefault()
    ): LocalDate {
        val ldt = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDateTime()
        return ldt.minusMinutes(resetMinuteOfDay.toLong()).toLocalDate()
    }

    /** 从某天往前推到所在游戏周的起始日（周一/周二…由 weekResetDay 决定） */
    fun weekStart(day: LocalDate, weekResetDay: Int): LocalDate {
        var d = day
        var guard = 0
        while (d.dayOfWeek.value != weekResetDay && guard < 7) {
            d = d.minusDays(1)
            guard++
        }
        return d
    }

    fun dayKey(day: LocalDate): String = "D$day"

    fun weekKey(weekStartDay: LocalDate): String = "W$weekStartDay"

    /** 活动任务的 key 带上活动 id，免得 A 活动的任务影响 B 活动 */
    fun onceKey(activityId: Long?): String =
        if (activityId == null) ONCE_KEY else "ONCE#$activityId"

    fun keyFor(
        type: TaskType,
        game: GameEntity,
        nowMillis: Long,
        activityId: Long? = null
    ): String = when (type) {
        TaskType.DAILY -> dayKey(gameDay(nowMillis, game.resetMinuteOfDay))
        TaskType.WEEKLY -> weekKey(
            weekStart(gameDay(nowMillis, game.resetMinuteOfDay), game.weekResetDay)
        )
        TaskType.ONCE -> onceKey(activityId)
    }

    fun keyFor(task: TaskEntity, game: GameEntity, nowMillis: Long): String =
        keyFor(task.type, game, nowMillis, task.activityId)

    /** 下一个日刷新时刻 */
    fun nextDailyResetMillis(
        resetMinuteOfDay: Int,
        nowMillis: Long,
        zone: ZoneId = ZoneId.systemDefault()
    ): Long {
        val now = Instant.ofEpochMilli(nowMillis).atZone(zone)
        val todayReset = now.toLocalDate().atStartOfDay(zone).plusMinutes(resetMinuteOfDay.toLong())
        val target = if (todayReset.toInstant().toEpochMilli() > nowMillis) todayReset
        else todayReset.plusDays(1)
        return target.toInstant().toEpochMilli()
    }

    /** 下一个周刷新时刻（周起始日的刷新点） */
    fun nextWeeklyResetMillis(
        weekResetDay: Int,
        resetMinuteOfDay: Int,
        nowMillis: Long,
        zone: ZoneId = ZoneId.systemDefault()
    ): Long {
        val today = gameDay(nowMillis, resetMinuteOfDay, zone)
        val nextStart = weekStart(today, weekResetDay).plusWeeks(1)
        return nextStart.atStartOfDay(zone)
            .plusMinutes(resetMinuteOfDay.toLong())
            .toInstant().toEpochMilli()
    }

    /** 毫秒 → 「2小时15分」这种口吻 */
    fun formatRemain(millis: Long): String {
        if (millis <= 0) return "已刷新"
        val totalMinutes = millis / 60_000
        val days = totalMinutes / (60 * 24)
        val hours = (totalMinutes % (60 * 24)) / 60
        val minutes = totalMinutes % 60
        return when {
            days > 0 -> "${days}天${hours}小时"
            hours > 0 -> "${hours}小时${minutes}分"
            else -> "${minutes}分"
        }
    }

    fun formatHourMinute(minuteOfDay: Int): String {
        val h = (minuteOfDay / 60) % 24
        val m = minuteOfDay % 60
        return String.format("%02d:%02d", h, m)
    }
}
