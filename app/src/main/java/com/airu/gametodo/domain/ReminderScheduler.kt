package com.airu.gametodo.domain

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.airu.gametodo.MainActivity
import com.airu.gametodo.data.GameEntity
import java.time.Instant
import java.time.ZoneId

/**
 * 统一提醒调度。三个独立闹钟：日常 / 周常 / 版本活动，时间都可在设置里改。
 * 用 AlarmManager 精确闹钟而非 WorkManager —— 后者最小 15 分钟且会被 Doze 推迟，卡不准点。
 */
object ReminderScheduler {

    const val CHANNEL_ID = "task_reminder"
    private const val LEGACY_CHANNEL_ID = "game_reset"

    const val EXTRA_KIND = "extra_kind"
    const val KIND_DAILY = "daily"
    const val KIND_WEEKLY = "weekly"
    const val KIND_ACTIVITY = "activity"

    private const val REQ_DAILY = 1001
    private const val REQ_WEEKLY = 1002
    private const val REQ_ACTIVITY = 1003

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "任务提醒",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "日常、周常和版本活动的统一提醒"
            }
            manager.createNotificationChannel(channel)
        }
        // 老版本按游戏排的「游戏刷新提醒」通道已经不用了，清掉免得设置里看着乱
        if (manager.getNotificationChannel(LEGACY_CHANNEL_ID) != null) {
            manager.deleteNotificationChannel(LEGACY_CHANNEL_ID)
        }
    }

    /** 按当前设置把三个闹钟全部重排一遍。App 启动、开机、改设置、提醒触发后都调它。 */
    fun rescheduleAll(context: Context, now: Long = System.currentTimeMillis()) {
        val s = ReminderPrefs.load(context)
        val zone = ZoneId.systemDefault()
        schedule(
            context, KIND_DAILY, REQ_DAILY, s.dailyEnabled,
            Period.nextDailyResetMillis(s.dailyMinute, now, zone)
        )
        schedule(
            context, KIND_WEEKLY, REQ_WEEKLY, s.weeklyEnabled,
            nextWeeklyMillis(s.weeklyDay, s.weeklyMinute, now, zone)
        )
        schedule(
            context, KIND_ACTIVITY, REQ_ACTIVITY, s.activityEnabled,
            Period.nextDailyResetMillis(s.activityMinute, now, zone)
        )
    }

    /** 某个非「时刻」周期（例如每周日晚 8 点）的下一次触发时间 */
    fun nextWeeklyMillis(
        dayOfWeek: Int,
        minuteOfDay: Int,
        nowMillis: Long,
        zone: ZoneId = ZoneId.systemDefault()
    ): Long {
        val now = Instant.ofEpochMilli(nowMillis).atZone(zone)
        var date = now.toLocalDate()
        var guard = 0
        while (date.dayOfWeek.value != dayOfWeek && guard < 7) {
            date = date.plusDays(1)
            guard++
        }
        var at = date.atStartOfDay(zone).plusMinutes(minuteOfDay.toLong())
        if (at.toInstant().toEpochMilli() <= nowMillis) at = at.plusWeeks(1)
        return at.toInstant().toEpochMilli()
    }

    private fun schedule(context: Context, kind: String, req: Int, enabled: Boolean, triggerAt: Long) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val pending = pendingIntent(context, kind, req)
        if (!enabled) {
            alarmManager.cancel(pending)
            return
        }
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarmManager.canScheduleExactAlarms()
        try {
            if (canExact) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            }
        } catch (e: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    private fun pendingIntent(context: Context, kind: String, req: Int): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = "com.airu.gametodo.action.REMIND"
            putExtra(EXTRA_KIND, kind)
        }
        return PendingIntent.getBroadcast(
            context,
            req,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun contentIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            99001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** 游戏级通知开关改成「是否参与统一提醒」，这里只读不排 */
    fun participates(game: GameEntity): Boolean = game.reminderEnabled
}
