package com.airu.gametodo

import com.airu.gametodo.data.GameEntity
import com.airu.gametodo.data.TaskType
import com.airu.gametodo.domain.Period
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class PeriodTest {

    private val zone = ZoneId.of("Asia/Shanghai")

    private fun ts(y: Int, m: Int, d: Int, h: Int, min: Int): Long =
        LocalDateTime.of(y, m, d, h, min).atZone(zone).toInstant().toEpochMilli()

    private val game = GameEntity(id = 1, name = "测试", resetMinuteOfDay = 240, weekResetDay = 1)

    // ---- 游戏日：刷新时刻之前算前一天 ----

    @Test
    fun `凌晨2点30在4点刷新前_算前一天`() {
        val day = Period.gameDay(ts(2026, 11, 26, 2, 30), 240, zone)
        assertEquals(LocalDate.of(2026, 11, 25), day)
    }

    @Test
    fun `早上5点已过4点刷新_算当天`() {
        val day = Period.gameDay(ts(2026, 11, 26, 5, 0), 240, zone)
        assertEquals(LocalDate.of(2026, 11, 26), day)
    }

    @Test
    fun `正好在4点整_算当天`() {
        val day = Period.gameDay(ts(2026, 11, 26, 4, 0), 240, zone)
        assertEquals(LocalDate.of(2026, 11, 26), day)
    }

    @Test
    fun `刷新设为0点_就是自然日`() {
        val day = Period.gameDay(ts(2026, 11, 26, 2, 30), 0, zone)
        assertEquals(LocalDate.of(2026, 11, 26), day)
    }

    @Test
    fun `刷新设在凌晨5点_4点59仍算前一天`() {
        val day = Period.gameDay(ts(2026, 11, 26, 4, 59), 300, zone)
        assertEquals(LocalDate.of(2026, 11, 25), day)
    }

    // ---- 游戏周 ----

    @Test
    fun `周四往前推到周一`() {
        val start = Period.weekStart(LocalDate.of(2026, 11, 26), 1)
        assertEquals(LocalDate.of(2026, 11, 23), start)
    }

    @Test
    fun `周刷新日就是当天_取当天`() {
        val start = Period.weekStart(LocalDate.of(2026, 11, 26), 4)
        assertEquals(LocalDate.of(2026, 11, 26), start)
    }

    @Test
    fun `周刷新日设为周日_周四前推到上周日`() {
        val start = Period.weekStart(LocalDate.of(2026, 11, 26), 7)
        assertEquals(LocalDate.of(2026, 11, 22), start)
    }

    // ---- period key ----

    @Test
    fun `日常key按游戏日生成`() {
        val key = Period.keyFor(TaskType.DAILY, game, ts(2026, 11, 26, 2, 30))
        assertEquals("D2026-11-25", key)
    }

    @Test
    fun `周常key按周起始日生成_同一周内一致`() {
        val a = Period.keyFor(TaskType.WEEKLY, game, ts(2026, 11, 24, 10, 0))
        val b = Period.keyFor(TaskType.WEEKLY, game, ts(2026, 11, 29, 23, 0))
        assertEquals("W2026-11-23", a)
        assertEquals(a, b)
    }

    @Test
    fun `周常在刷新日跨周后变成新key`() {
        val before = Period.keyFor(TaskType.WEEKLY, game, ts(2026, 11, 23, 3, 0))
        val after = Period.keyFor(TaskType.WEEKLY, game, ts(2026, 11, 23, 5, 0))
        assertEquals("W2026-11-16", before)
        assertEquals("W2026-11-23", after)
    }

    @Test
    fun `一次性任务key固定`() {
        val key = Period.keyFor(TaskType.ONCE, game, ts(2026, 11, 26, 12, 0))
        assertEquals(Period.ONCE_KEY, key)
    }

    // ---- 下次刷新时刻 ----

    @Test
    fun `未到刷新点_下次刷新是今天4点`() {
        val next = Period.nextDailyResetMillis(240, ts(2026, 11, 26, 2, 0), zone)
        assertEquals(ts(2026, 11, 26, 4, 0), next)
    }

    @Test
    fun `已过刷新点_下次刷新是明天4点`() {
        val next = Period.nextDailyResetMillis(240, ts(2026, 11, 26, 5, 0), zone)
        assertEquals(ts(2026, 11, 27, 4, 0), next)
    }

    // ---- 文案 ----

    @Test
    fun `剩余时长文案`() {
        assertEquals("2小时15分", Period.formatRemain((2 * 60 + 15) * 60_000L))
        assertEquals("3天5小时", Period.formatRemain(((3 * 24 + 5) * 60) * 60_000L))
        assertEquals("42分", Period.formatRemain(42 * 60_000L))
    }

    @Test
    fun `刷新时刻格式化`() {
        assertEquals("04:00", Period.formatHourMinute(240))
        assertEquals("00:00", Period.formatHourMinute(0))
        assertEquals("23:30", Period.formatHourMinute(23 * 60 + 30))
    }
}
