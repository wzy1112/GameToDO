package com.airu.gametodo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.airu.gametodo.data.ActivityEntity
import com.airu.gametodo.data.GameEntity
import com.airu.gametodo.data.TaskEntity
import com.airu.gametodo.data.TaskType
import com.airu.gametodo.domain.AppLauncher
import com.airu.gametodo.domain.Period
import com.airu.gametodo.ui.components.GameAvatar
import com.airu.gametodo.ui.theme.GameColors
import com.airu.gametodo.vm.AppViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private val WEEK_NAMES = listOf("一", "二", "三", "四", "五", "六", "日")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameEditScreen(
    vm: AppViewModel,
    gameId: Long,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val games by vm.games.collectAsState()
    val tasks by vm.tasks.collectAsState()
    val activities by vm.activities.collectAsState()

    val game = games.firstOrNull { it.id == gameId }

    var draft by remember(gameId) { mutableStateOf<GameEntity?>(null) }
    LaunchedEffect(game) {
        if (draft == null && game != null) draft = game
    }

    var showTimePicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showActivityDialog by remember { mutableStateOf(false) }
    var editingActivity by remember { mutableStateOf<ActivityEntity?>(null) }

    val current = draft
    if (current == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("读取中…", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val accent = Color(current.colorArgb)
    val gameTasks = tasks.filter { it.gameId == gameId }
    val gameActivities = activities.filter { it.gameId == gameId }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 40.dp)
    ) {
        // 顶栏
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 12.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Rounded.ArrowBack,
                    contentDescription = "返回",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "游戏设置",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "保存",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        vm.saveGame(current) { onBack() }
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }

        Column(
            Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 基本信息
            CardBlock {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GameAvatar(
                        icon = current.icon,
                        accent = accent,
                        name = current.name,
                        size = 52.dp
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = current.name,
                            onValueChange = { draft = current.copy(name = it) },
                            label = { Text("游戏名") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = if (current.packageName.isNullOrBlank()) "未绑定应用"
                            else current.packageName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (!current.packageName.isNullOrBlank()) {
                        Surface(
                            onClick = { AppLauncher.launch(context, current.packageName) },
                            shape = RoundedCornerShape(10.dp),
                            color = accent.copy(alpha = 0.14f)
                        ) {
                            Row(
                                Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(Modifier.width(3.dp))
                                Text("启动", color = accent, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            // 刷新规则
            CardBlock {
                SectionLabel("刷新规则")
                Spacer(Modifier.height(10.dp))

                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showTimePicker = true }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "日常刷新时间",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "这个点之前算前一天",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = Period.formatHourMinute(current.resetMinuteOfDay),
                        style = MaterialTheme.typography.titleMedium,
                        color = accent
                    )
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    "周常刷新日",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    WEEK_NAMES.forEachIndexed { index, label ->
                        val selected = current.weekResetDay == index + 1
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (selected) accent else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { draft = current.copy(weekResetDay = index + 1) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (selected) Color(0xFF06121A)
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "加入提醒",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "统一提醒里包含这个游戏",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = current.reminderEnabled,
                        onCheckedChange = { draft = current.copy(reminderEnabled = it) }
                    )
                }
            }

            // 主题色
            CardBlock {
                SectionLabel("主题色")
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GameColors.forEach { color ->
                        val argb = color.toArgb()
                        val selected = current.colorArgb == argb
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(color)
                                .then(
                                    if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    else Modifier
                                )
                                .clickable { draft = current.copy(colorArgb = argb) }
                        )
                    }
                }
            }

            // 任务
            CardBlock {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel("任务清单")
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "增删即时生效",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(8.dp))
                TaskSection(
                    title = "日常",
                    type = TaskType.DAILY,
                    tasks = gameTasks.filter { it.type == TaskType.DAILY },
                    accent = accent,
                    onAdd = { title -> vm.quickAddTask(gameId, title, TaskType.DAILY) },
                    onDelete = { vm.deleteTask(it) }
                )
                Spacer(Modifier.height(10.dp))
                TaskSection(
                    title = "周常",
                    type = TaskType.WEEKLY,
                    tasks = gameTasks.filter { it.type == TaskType.WEEKLY },
                    accent = accent,
                    onAdd = { title -> vm.quickAddTask(gameId, title, TaskType.WEEKLY) },
                    onDelete = { vm.deleteTask(it) }
                )
            }

            // 活动
            CardBlock {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel("版本活动")
                    Spacer(Modifier.weight(1f))
                    Surface(
                        onClick = { editingActivity = null; showActivityDialog = true },
                        shape = RoundedCornerShape(9.dp),
                        color = accent.copy(alpha = 0.14f)
                    ) {
                        Row(
                            Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.Add,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(2.dp))
                            Text("新活动", color = accent, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (gameActivities.isEmpty()) {
                    Text(
                        "填上起止日期，点进去可以加这个活动的任务",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    gameActivities.forEach { activity ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { editingActivity = activity; showActivityDialog = true }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier
                                    .width(3.dp)
                                    .height(30.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color(activity.colorArgb))
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = activity.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${LocalDate.ofEpochDay(activity.startEpochDay)} → ${LocalDate.ofEpochDay(activity.endEpochDay)}" +
                                        " · ${gameTasks.count { it.activityId == activity.id }} 个任务",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { vm.deleteActivity(activity.id) },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = "删除活动",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 危险操作
            Surface(
                onClick = { showDeleteConfirm = true },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.10f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(vertical = 13.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "删除这个游戏",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }

    // 时间选择
    if (showTimePicker) {
        val state = rememberTimePickerState(
            initialHour = current.resetMinuteOfDay / 60,
            initialMinute = current.resetMinuteOfDay % 60,
            is24Hour = true
        )
        Dialog(
            onDismissRequest = { showTimePicker = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.padding(horizontal = 20.dp)
            ) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "日常刷新时间",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(14.dp))
                    TimePicker(state = state)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showTimePicker = false }) { Text("取消") }
                        TextButton(onClick = {
                            draft = current.copy(
                                resetMinuteOfDay = state.hour * 60 + state.minute
                            )
                            showTimePicker = false
                        }) { Text("确定") }
                    }
                }
            }
        }
    }

    // 删除确认
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除「${current.name}」？") },
            text = { Text("它的任务、活动和打卡记录会一起删掉，不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    vm.deleteGame(current)
                    onBack()
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
            }
        )
    }

    // 活动编辑
    if (showActivityDialog) {
        ActivityDialog(
            initial = editingActivity,
            initialTasks = editingActivity
                ?.let { act -> tasks.filter { it.activityId == act.id } }
                .orEmpty(),
            onDismiss = { showActivityDialog = false },
            onConfirm = { title, start, end, drafts ->
                val existing = editingActivity
                vm.saveActivityWithTasks(
                    ActivityEntity(
                        id = existing?.id ?: 0,
                        gameId = gameId,
                        title = title,
                        startEpochDay = start.toEpochDay(),
                        endEpochDay = end.toEpochDay(),
                        colorArgb = existing?.colorArgb ?: accent.toArgb()
                    ),
                    drafts
                )
                showActivityDialog = false
            }
        )
    }
}

@Composable
private fun CardBlock(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) { content() }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun TaskSection(
    title: String,
    type: TaskType,
    tasks: List<TaskEntity>,
    accent: Color,
    onAdd: (String) -> Unit,
    onDelete: (TaskEntity) -> Unit
) {
    var adding by remember { mutableStateOf(false) }
    var input by remember { mutableStateOf("") }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "${tasks.size}",
            style = MaterialTheme.typography.labelSmall,
            color = accent
        )
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.14f))
                .clickable { adding = !adding },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Add,
                contentDescription = "添加$title",
                tint = accent,
                modifier = Modifier.size(15.dp)
            )
        }
    }

    tasks.forEach { task ->
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
            Spacer(Modifier.width(9.dp))
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            IconButton(
                onClick = { onDelete(task) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = "删除任务",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }

    if (adding) {
        Row(
            Modifier.padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text("任务名", fontSize = 13.sp) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "确定",
                style = MaterialTheme.typography.labelLarge,
                color = accent,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        if (input.isNotBlank()) {
                            onAdd(input)
                            input = ""
                            adding = false
                        }
                    }
                    .padding(horizontal = 10.dp, vertical = 10.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActivityDialog(
    initial: ActivityEntity?,
    initialTasks: List<TaskEntity>,
    onDismiss: () -> Unit,
    onConfirm: (String, LocalDate, LocalDate, List<Pair<Long, String>>) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var start by remember {
        mutableStateOf(
            initial?.let { LocalDate.ofEpochDay(it.startEpochDay) } ?: LocalDate.now()
        )
    }
    var end by remember {
        mutableStateOf(
            initial?.let { LocalDate.ofEpochDay(it.endEpochDay) } ?: LocalDate.now().plusDays(14)
        )
    }
    var picking by remember { mutableStateOf<Int?>(null) }
    var drafts by remember {
        mutableStateOf(initialTasks.map { it.id to it.title })
    }
    var newTask by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    text = if (initial == null) "新建活动" else "编辑活动",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("活动名") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DateField(
                        label = "开始",
                        date = start,
                        modifier = Modifier.weight(1f),
                        onClick = { picking = 0 }
                    )
                    DateField(
                        label = "结束",
                        date = end,
                        modifier = Modifier.weight(1f),
                        onClick = { picking = 1 }
                    )
                }

                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "活动任务",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "${drafts.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        "只在活动期间出现",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(6.dp))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 170.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (drafts.isEmpty()) {
                        Text(
                            "还没有任务，在下面加一条",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                    drafts.forEachIndexed { index, (taskId, name) ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondary)
                            )
                            Spacer(Modifier.width(9.dp))
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            IconButton(
                                onClick = {
                                    drafts = drafts.filterIndexed { i, _ -> i != index }
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = "删除任务",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newTask,
                        onValueChange = { newTask = it },
                        placeholder = { Text("任务名", fontSize = 13.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "添加",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                if (newTask.isNotBlank()) {
                                    drafts = drafts + (0L to newTask.trim())
                                    newTask = ""
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 10.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    TextButton(
                        onClick = {
                            if (title.isNotBlank()) {
                                onConfirm(
                                    title.trim(),
                                    start,
                                    if (end.isBefore(start)) start else end,
                                    drafts
                                )
                            }
                        }
                    ) { Text("保存") }
                }
            }
        }
    }

    picking?.let { which ->
        val initDate = if (which == 0) start else end
        val dpState = rememberDatePickerState(
            initialSelectedDateMillis = initDate.toEpochDay() * 86_400_000L
        )
        DatePickerDialog(
            onDismissRequest = { picking = null },
            confirmButton = {
                TextButton(onClick = {
                    dpState.selectedDateMillis?.let { millis ->
                        val picked = Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC).toLocalDate()
                        if (which == 0) start = picked else end = picked
                    }
                    picking = null
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { picking = null }) { Text("取消") }
            }
        ) {
            DatePicker(state = dpState)
        }
    }
}

@Composable
private fun DateField(
    label: String,
    date: LocalDate,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${date.monthValue}/${date.dayOfMonth}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
