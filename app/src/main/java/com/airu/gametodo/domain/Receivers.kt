package com.airu.gametodo.domain

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.airu.gametodo.R
import com.airu.gametodo.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** 统一提醒触发：算还有啥没做，有剩才发通知，然后把下一次排好 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val kind = intent.getStringExtra(ReminderScheduler.EXTRA_KIND) ?: return
        val pending = goAsync()
        val app = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.get(app)
                val prefs = ReminderPrefs.load(app)
                val now = System.currentTimeMillis()
                val payload = when (kind) {
                    ReminderScheduler.KIND_DAILY -> ReminderContent.daily(db, now)
                    ReminderScheduler.KIND_WEEKLY -> ReminderContent.weekly(db, now)
                    ReminderScheduler.KIND_ACTIVITY ->
                        ReminderContent.activity(db, now, prefs.activityLeadDays)
                    else -> null
                }
                payload?.let { (title, body) -> notify(app, kind, title, body) }
                // 无论有没有发通知，都要把下一次排上，否则只响一次
                ReminderScheduler.rescheduleAll(app)
            } catch (e: Exception) {
                // 提醒失败不能影响下一次调度
            } finally {
                pending.finish()
            }
        }
    }

    private fun notify(context: Context, kind: String, title: String, body: String) {
        ReminderScheduler.ensureChannel(context)
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val notification = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(ReminderScheduler.contentIntent(context))
            .build()
        val id = when (kind) {
            ReminderScheduler.KIND_DAILY -> 100
            ReminderScheduler.KIND_WEEKLY -> 200
            else -> 300
        }
        try {
            manager.notify(id, notification)
        } catch (e: SecurityException) {
            // 没给通知权限就算了
        }
    }
}

/** 开机 / 应用更新后重排所有闹钟（AlarmManager 的闹钟不是持久化的） */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val pending = goAsync()
        val app = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ReminderScheduler.ensureChannel(app)
                ReminderScheduler.rescheduleAll(app)
            } catch (e: Exception) {
                // 忽略
            } finally {
                pending.finish()
            }
        }
    }
}
