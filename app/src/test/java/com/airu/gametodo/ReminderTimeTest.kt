package com.airu.gametodo

import com.airu.gametodo.domain.Period
import com.airu.gametodo.domain.ReminderScheduler
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

/** 统一提醒的触发时刻：每周固定日 + 每天固定时刻 */
class ReminderTimeTest {

    private val zone = ZoneId.of("Asia/Shanghai")

    private fun ts(y: Int, m: Int, d: Int, h: Int, min: Int): Long =
        LocalDateTime.of(y, m, d, h, min).atZone(zone).toInstant().toEpochMilli()

    private fun fmt(millis: Long): LocalDateTime =
        java.time.Instant.ofEpochMilli(millis).atZone(zone).toLocalDateTime()

    @Test
    fun `周四下午_下一个周日晚8点`() {
        // 2026-11-26 是周四
        val next = ReminderScheduler.nextWeeklyMillis(7, 20 * 60, ts(2026, 11, 26, 13, 0), zone)
        assertEquals(LocalDateTime.of(2026, 11, 29, 20, 0), fmt(next))
    }

    @Test
    fun `周日晚7点_当天晚8点`() {
        val next = ReminderScheduler.nextWeeklyMillis(7, 20 * 60, ts(2026, 11, 29, 19, 0), zone)
        assertEquals(LocalDateTime.of(2026, 11, 29, 20, 0), fmt(next))
    }

    @Test
    fun `周日晚9点已过_排到下周日`() {
        val next = ReminderScheduler.nextWeeklyMillis(7, 20 * 60, ts(2026, 11, 29, 21, 0), zone)
        assertEquals(LocalDateTime.of(2026, 12, 6, 20, 0), fmt(next))
    }

    @Test
    fun `日常提醒当天已过点_排到明天`() {
        val next = Period.nextDailyResetMillis(20 * 60, ts(2026, 11, 26, 21, 30), zone)
        assertEquals(LocalDateTime.of(2026, 11, 27, 20, 0), fmt(next))
    }
}
