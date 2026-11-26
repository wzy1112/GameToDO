package com.airu.gametodo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.airu.gametodo.data.ActivityEntity
import com.airu.gametodo.data.GameEntity
import com.airu.gametodo.data.LogEntity
import com.airu.gametodo.data.TaskEntity
import com.airu.gametodo.ui.components.EmptyState
import com.airu.gametodo.vm.AppViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

private val WEEK_LABELS = listOf("一", "二", "三", "四", "五", "六", "日")

@Composable
fun CalendarScreen(vm: AppViewModel) {
    val games by vm.games.collectAsState()
    val tasks by vm.tasks.collectAsState()
    val logs by vm.logs.collectAsState()
    val activities by vm.activities.collectAsState()

    var yearMonth by remember { mutableStateOf(YearMonth.now()) }
    var selected by remember { mutableStateOf<LocalDate?>(null) }

    val gameById = remember(games) { games.associateBy { it.id } }
    val taskById = remember(tasks) { tasks.associateBy { it.id } }

    // 打卡记录按「实际完成日期」归档，日历上点哪天就是那天真勾掉的东西
    val doneByDate = remember(logs) {
        logs.groupBy {
            Instant.ofEpochMilli(it.doneAt).atZone(ZoneId.systemDefault()).toLocalDate()
        }
    }

    // 进行中 + 还没开始的都要露出来；只把本月之前就结束的挡掉
    val monthActivities = remember(activities, yearMonth) {
        val first = yearMonth.atDay(1).toEpochDay()
        activities.filter { it.endEpochDay >= first }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(Modifier.padding(top = 8.dp)) {
                Text(
                    text = "日历",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "打卡记录与版本活动进度",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 月份切换
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { yearMonth = yearMonth.minusMonths(1) }) {
                    Icon(
                        Icons.Rounded.ChevronLeft,
                        contentDescription = "上个月",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "${yearMonth.year} 年 ${yearMonth.monthValue} 月",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                IconButton(onClick = { yearMonth = yearMonth.plusMonths(1) }) {
                    Icon(
                        Icons.Rounded.ChevronRight,
                        contentDescription = "下个月",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.weight(1f))
                if (yearMonth != YearMonth.now()) {
                    Text(
                        text = "回到本月",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                yearMonth = YearMonth.now()
                                selected = null
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // 月历
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(12.dp)) {
                    Row(Modifier.fillMaxWidth()) {
                        WEEK_LABELS.forEach { label ->
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    MonthGrid(
                        yearMonth = yearMonth,
                        doneByDate = doneByDate,
                        taskById = taskById,
                        gameById = gameById,
                        selected = selected,
                        onSelect = { selected = if (selected == it) null else it }
                    )
                }
            }
        }

        // 选中日详情
        selected?.let { day ->
            item {
                DayDetailCard(
                    day = day,
                    logs = doneByDate[day].orEmpty(),
                    taskById = taskById,
                    gameById = gameById
                )
            }
        }

        // 版本活动
        item {
            Text(
                text = "版本活动",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        item {
            if (monthActivities.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    EmptyState(
                        title = "本月没有版本活动",
                        hint = "在游戏设置里给活动填上起止日期，\n这里会显示每个活动的进度"
                    )
                }
            } else {
                ActivityProgressCard(activities = monthActivities, gameById = gameById)
            }
        }
    }
}

@Composable
private fun MonthGrid(
    yearMonth: YearMonth,
    doneByDate: Map<LocalDate, List<LogEntity>>,
    taskById: Map<Long, TaskEntity>,
    gameById: Map<Long, GameEntity>,
    selected: LocalDate?,
    onSelect: (LocalDate) -> Unit
) {
    val first = yearMonth.atDay(1)
    val lead = first.dayOfWeek.value - 1
    val dayCount = yearMonth.lengthOfMonth()
    val today = LocalDate.now()
    val cellCount = ((lead + dayCount + 6) / 7) * 7

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        for (row in 0 until cellCount / 7) {
            Row(Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val index = row * 7 + col
                    val dayNum = index - lead + 1
                    if (dayNum in 1..dayCount) {
                        val date = yearMonth.atDay(dayNum)
                        DayCell(
                            date = date,
                            isToday = date == today,
                            isSelected = date == selected,
                            dots = dotColorsFor(date, doneByDate, taskById, gameById),
                            modifier = Modifier.weight(1f),
                            onClick = { onSelect(date) }
                        )
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

private fun dotColorsFor(
    date: LocalDate,
    doneByDate: Map<LocalDate, List<LogEntity>>,
    taskById: Map<Long, TaskEntity>,
    gameById: Map<Long, GameEntity>
): List<Color> {
    val entries = doneByDate[date] ?: return emptyList()
    return entries.mapNotNull { log ->
        val task = taskById[log.taskId] ?: return@mapNotNull null
        val game = gameById[task.gameId] ?: return@mapNotNull null
        Color(game.colorArgb)
    }.take(3)
}

@Composable
private fun DayCell(
    date: LocalDate,
    isToday: Boolean,
    isSelected: Boolean,
    dots: List<Color>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg = when {
        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
        isToday -> MaterialTheme.colorScheme.surfaceVariant
        else -> Color.Transparent
    }
    Column(
        modifier = modifier
            .height(46.dp)
            .padding(1.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .then(
                if (isSelected) Modifier.border(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    RoundedCornerShape(10.dp)
                ) else Modifier
            )
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isToday) FontWeight.SemiBold else FontWeight.Normal,
            color = when {
                isToday -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
        Spacer(Modifier.height(3.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            repeat(3) { i ->
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(if (i < dots.size) dots[i] else Color.Transparent)
                )
            }
        }
    }
}

@Composable
private fun DayDetailCard(
    day: LocalDate,
    logs: List<LogEntity>,
    taskById: Map<Long, TaskEntity>,
    gameById: Map<Long, GameEntity>
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${day.monthValue} 月 ${day.dayOfMonth} 日",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "完成 ${logs.size} 项",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            if (logs.isEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "这天没有打卡记录",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Spacer(Modifier.height(10.dp))
                logs.forEach { log ->
                    val task = taskById[log.taskId]
                    val game = task?.let { gameById[it.gameId] }
                    if (task != null && game != null) {
                        Row(
                            modifier = Modifier.padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(game.colorArgb))
                            )
                            Spacer(Modifier.width(9.dp))
                            Text(
                                text = task.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = game.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 活动进度列表：进行中画进度条，未开始只报天数，已结束置灰 */
@Composable
private fun ActivityProgressCard(
    activities: List<ActivityEntity>,
    gameById: Map<Long, GameEntity>
) {
    val today = LocalDate.now()
    val sorted = remember(activities) {
        activities.sortedWith(
            compareBy({ statusRank(it, today) }, { it.startEpochDay })
        )
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            sorted.forEachIndexed { index, activity ->
                if (index > 0) Spacer(Modifier.height(14.dp))
                ActivityRow(activity = activity, game = gameById[activity.gameId], today = today)
            }
        }
    }
}

/** 0=进行中 1=未开始 2=已结束 */
private fun statusRank(activity: ActivityEntity, today: LocalDate): Int {
    val start = LocalDate.ofEpochDay(activity.startEpochDay)
    val end = LocalDate.ofEpochDay(activity.endEpochDay)
    return when {
        today.isBefore(start) -> 1
        today.isAfter(end) -> 2
        else -> 0
    }
}

@Composable
private fun ActivityRow(activity: ActivityEntity, game: GameEntity?, today: LocalDate) {
    val color = Color(activity.colorArgb)
    val start = LocalDate.ofEpochDay(activity.startEpochDay)
    val end = LocalDate.ofEpochDay(activity.endEpochDay)
    val total = (activity.endEpochDay - activity.startEpochDay + 1).toInt().coerceAtLeast(1)
    val passed = (today.toEpochDay() - activity.startEpochDay + 1).toInt().coerceIn(0, total)
    val daysToStart = (activity.startEpochDay - today.toEpochDay()).toInt()
    val daysToEnd = (activity.endEpochDay - today.toEpochDay()).toInt()
    val ongoing = !today.isBefore(start) && !today.isAfter(end)
    val upcoming = today.isBefore(start)
    val accent = if (ongoing && daysToEnd <= 3) MaterialTheme.colorScheme.error else color

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .width(3.dp)
                    .height(30.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (upcoming) color.copy(alpha = 0.35f) else color)
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = activity.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (ongoing || upcoming) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildString {
                        if (game != null) append("${game.name} · ")
                        append("${start.monthValue}/${start.dayOfMonth} - ${end.monthValue}/${end.dayOfMonth}")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = when {
                    upcoming -> "$daysToStart 天后开始"
                    ongoing && daysToEnd <= 0 -> "今天截止"
                    ongoing -> "剩 $daysToEnd 天"
                    else -> "已结束"
                },
                style = MaterialTheme.typography.labelLarge,
                color = when {
                    upcoming -> MaterialTheme.colorScheme.primary
                    ongoing -> accent
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }

        if (ongoing) {
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { passed.toFloat() / total },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = accent,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "第 $passed / $total 天",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else if (upcoming) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "${start.monthValue} 月 ${start.dayOfMonth} 日开始",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
