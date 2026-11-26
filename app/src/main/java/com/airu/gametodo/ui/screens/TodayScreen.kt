package com.airu.gametodo.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airu.gametodo.data.ActivityEntity
import com.airu.gametodo.data.GameEntity
import com.airu.gametodo.data.LogEntity
import com.airu.gametodo.data.TaskEntity
import com.airu.gametodo.data.TaskType
import com.airu.gametodo.domain.AppLauncher
import com.airu.gametodo.domain.Period
import com.airu.gametodo.ui.components.EmptyState
import com.airu.gametodo.ui.components.GameAvatar
import com.airu.gametodo.ui.components.Tag
import com.airu.gametodo.vm.AppViewModel
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DAY_FMT = DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.CHINA)

@Composable
fun TodayScreen(
    vm: AppViewModel,
    onOpenGame: (Long) -> Unit,
    onOpenLibrary: () -> Unit
) {
    val games by vm.games.collectAsState()
    val tasks by vm.tasks.collectAsState()
    val logs by vm.logs.collectAsState()
    val activities by vm.activities.collectAsState()

    // 每 20 秒刷新一次时间，让倒计时和刷新点实时
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(20_000L)
        }
    }

    val realToday = remember(now) { LocalDate.now() }

    // 全部日常完成情况
    val dailyTasks = tasks.filter { it.type == TaskType.DAILY }
    val doneDaily = dailyTasks.count { t ->
        val g = games.firstOrNull { it.id == t.gameId } ?: return@count false
        vm.isDone(t, g, logs, now)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(Modifier.padding(top = 8.dp, bottom = 2.dp)) {
                Text(
                    text = "今日",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = realToday.format(DAY_FMT),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (games.isEmpty()) {
            item {
                EmptyState(
                    title = "还没有添加游戏",
                    hint = "去「游戏库」把手机里的游戏绑进来，\n设置好它的刷新时间，就能开始打卡了"
                )
            }
            item {
                BigOutlineButton(text = "添加游戏", onClick = onOpenLibrary)
            }
        } else {
            item {
                OverviewCard(done = doneDaily, total = dailyTasks.size)
            }
            items(games, key = { it.id }) { game ->
                GameTaskCard(
                    game = game,
                    tasks = tasks.filter { it.gameId == game.id },
                    activities = activities.filter { it.gameId == game.id },
                    logs = logs,
                    now = now,
                    vm = vm,
                    onOpenGame = { onOpenGame(game.id) }
                )
            }
        }
    }
}

@Composable
private fun BigOutlineButton(text: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(text, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun OverviewCard(done: Int, total: Int) {
    val ratio = if (total == 0) 0f else done.toFloat() / total
    val animated by animateFloatAsState(targetValue = ratio, label = "progress")
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (total == 0) "还没有日常任务" else "今日日常",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "$done / $total",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (total > 0 && done == total) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { animated },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (total > 0 && done == total) MaterialTheme.colorScheme.tertiary
                else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            if (total > 0 && done == total) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "全部清完了，收工",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
private fun GameTaskCard(
    game: GameEntity,
    tasks: List<TaskEntity>,
    activities: List<ActivityEntity>,
    logs: List<LogEntity>,
    now: Long,
    vm: AppViewModel,
    onOpenGame: () -> Unit
) {
    val context = LocalContext.current
    val accent = Color(game.colorArgb)
    val daily = tasks.filter { it.type == TaskType.DAILY }
    val weekly = tasks.filter { it.type == TaskType.WEEKLY }
    val once = tasks.filter { it.type == TaskType.ONCE }

    val gameDay = remember(now, game.resetMinuteOfDay) {
        Period.gameDay(now, game.resetMinuteOfDay)
    }
    val nextReset = remember(now, game.resetMinuteOfDay) {
        Period.nextDailyResetMillis(game.resetMinuteOfDay, now)
    }
    val remainText = remember(nextReset, now) { Period.formatRemain(nextReset - now) }

    val realToday = LocalDate.now()
    val lagging = gameDay != realToday

    // 只显示进行中的活动，未开始 / 已结束的不占今日页
    val ongoing = activities.filter { a ->
        !realToday.isBefore(LocalDate.ofEpochDay(a.startEpochDay)) &&
            !realToday.isAfter(LocalDate.ofEpochDay(a.endEpochDay))
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            // 游戏头
            Row(verticalAlignment = Alignment.CenterVertically) {
                GameAvatar(icon = game.icon, accent = accent, name = game.name, size = 44.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = game.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.clickable { onOpenGame() }
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (lagging) "游戏日 ${gameDay.monthValue}/${gameDay.dayOfMonth}"
                            else "今天",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (lagging) accent else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = " · $remainText 后刷新",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (!game.packageName.isNullOrBlank()) {
                    Surface(
                        onClick = { AppLauncher.launch(context, game.packageName) },
                        shape = CircleShape,
                        color = accent.copy(alpha = 0.14f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.PlayArrow,
                                contentDescription = "启动",
                                tint = accent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            if (ongoing.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                ongoing.forEach { activity ->
                    ActivityProgressRow(
                        activity = activity,
                        today = realToday,
                        modifier = Modifier.clickable { onOpenGame() }
                    )
                    once.filter { it.activityId == activity.id }.forEach { task ->
                        TaskRow(
                            task = task,
                            done = vm.isDone(task, game, logs, now),
                            accent = Color(activity.colorArgb),
                            onToggle = { vm.toggleTask(task, game, now) }
                        )
                    }
                }
            }

            val visibleActivityTasks = ongoing.flatMap { a -> once.filter { it.activityId == a.id } }
            if (daily.isEmpty() && weekly.isEmpty() && visibleActivityTasks.isEmpty() && ongoing.isEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "还没有任务，点游戏名进去加",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { onOpenGame() }
                )
            }

            if (daily.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "日常",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${daily.count { t -> logs.any { it.taskId == t.id && it.periodKey == Period.keyFor(t, game, now) } }}/${daily.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = accent
                    )
                }
                Spacer(Modifier.height(4.dp))
                daily.forEach { task ->
                    TaskRow(
                        task = task,
                        done = vm.isDone(task, game, logs, now),
                        accent = accent,
                        onToggle = { vm.toggleTask(task, game, now) }
                    )
                }
            }

            if (weekly.isNotEmpty()) {
                val weekStart = Period.weekStart(gameDay, game.weekResetDay)
                val weekKey = Period.weekKey(weekStart)
                val doneCount = weekly.count { t -> logs.any { it.taskId == t.id && it.periodKey == weekKey } }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "本周",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "$doneCount/${weekly.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = accent
                    )
                    Spacer(Modifier.weight(1f))
                    val nextWeek = remember(now, game.weekResetDay, game.resetMinuteOfDay) {
                        Period.nextWeeklyResetMillis(game.weekResetDay, game.resetMinuteOfDay, now)
                    }
                    Text(
                        text = "${Period.formatRemain(nextWeek - now)}后重置",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(4.dp))
                weekly.forEach { task ->
                    TaskRow(
                        task = task,
                        done = vm.isDone(task, game, logs, now),
                        accent = accent,
                        onToggle = { vm.toggleTask(task, game, now) }
                    )
                }
            }

        }
    }
}

@Composable
private fun ActivityProgressRow(
    activity: ActivityEntity,
    today: LocalDate,
    modifier: Modifier = Modifier
) {
    val color = Color(activity.colorArgb)
    val total = (activity.endEpochDay - activity.startEpochDay + 1).toInt().coerceAtLeast(1)
    val passed = (today.toEpochDay() - activity.startEpochDay + 1).toInt().coerceIn(0, total)
    val remaining = (activity.endEpochDay - today.toEpochDay()).toInt()
    val progress = passed.toFloat() / total
    val urgent = remaining <= 3

    Column(modifier.padding(bottom = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Tag("活动", color)
            Spacer(Modifier.width(6.dp))
            Text(
                text = activity.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (remaining <= 0) "今天截止" else "剩 $remaining 天",
                style = MaterialTheme.typography.labelSmall,
                color = if (urgent) MaterialTheme.colorScheme.error else color
            )
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = if (urgent) MaterialTheme.colorScheme.error else color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
private fun TaskRow(
    task: TaskEntity,
    done: Boolean,
    accent: Color,
    onToggle: () -> Unit,
    indent: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onToggle() }
            .padding(vertical = 7.dp, horizontal = if (indent) 8.dp else 0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(if (done) accent else Color.Transparent)
                .then(
                    if (!done) Modifier.border(1.5.dp, accent.copy(alpha = 0.5f), CircleShape)
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            if (done) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    tint = Color(0xFF06121A),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (done) TextDecoration.LineThrough else null
            )
            if (task.note.isNotBlank()) {
                Text(
                    text = task.note,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
