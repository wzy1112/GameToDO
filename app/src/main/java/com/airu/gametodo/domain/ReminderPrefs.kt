package com.airu.gametodo.domain

import android.content.Context

/**
 * 统一提醒设置。7 个标量而已，不值得上 DataStore，SharedPreferences 够用。
 * 时间统一用「当天第几分钟」存，240 = 04:00。
 */
object ReminderPrefs {

    data class Settings(
        val dailyEnabled: Boolean = true,
        val dailyMinute: Int = 20 * 60,
        val weeklyEnabled: Boolean = true,
        /** 1=周一 … 7=周日 */
        val weeklyDay: Int = 7,
        val weeklyMinute: Int = 20 * 60,
        val activityEnabled: Boolean = true,
        /** 活动结束前多少天开始提醒 */
        val activityLeadDays: Int = 3,
        val activityMinute: Int = 20 * 60
    )

    private const val FILE = "reminder_prefs"

    fun load(context: Context): Settings {
        val p = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val d = Settings()
        return Settings(
            dailyEnabled = p.getBoolean("dailyEnabled", d.dailyEnabled),
            dailyMinute = p.getInt("dailyMinute", d.dailyMinute),
            weeklyEnabled = p.getBoolean("weeklyEnabled", d.weeklyEnabled),
            weeklyDay = p.getInt("weeklyDay", d.weeklyDay),
            weeklyMinute = p.getInt("weeklyMinute", d.weeklyMinute),
            activityEnabled = p.getBoolean("activityEnabled", d.activityEnabled),
            activityLeadDays = p.getInt("activityLeadDays", d.activityLeadDays),
            activityMinute = p.getInt("activityMinute", d.activityMinute)
        )
    }

    fun save(context: Context, s: Settings) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().apply {
            putBoolean("dailyEnabled", s.dailyEnabled)
            putInt("dailyMinute", s.dailyMinute)
            putBoolean("weeklyEnabled", s.weeklyEnabled)
            putInt("weeklyDay", s.weeklyDay)
            putInt("weeklyMinute", s.weeklyMinute)
            putBoolean("activityEnabled", s.activityEnabled)
            putInt("activityLeadDays", s.activityLeadDays)
            putInt("activityMinute", s.activityMinute)
        }.apply()
    }
}
