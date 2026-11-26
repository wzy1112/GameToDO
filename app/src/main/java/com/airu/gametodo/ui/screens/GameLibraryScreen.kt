package com.airu.gametodo.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.airu.gametodo.data.GameEntity
import com.airu.gametodo.domain.AppLauncher
import com.airu.gametodo.domain.InstalledApp
import com.airu.gametodo.domain.Period
import com.airu.gametodo.ui.components.EmptyState
import com.airu.gametodo.ui.components.GameAvatar
import com.airu.gametodo.ui.theme.GameColors
import com.airu.gametodo.vm.AppViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun GameLibraryScreen(
    vm: AppViewModel,
    onOpenGame: (Long) -> Unit
) {
    val context = LocalContext.current
    val games by vm.games.collectAsState()
    val tasks by vm.tasks.collectAsState()
    val activities by vm.activities.collectAsState()
    var showPicker by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Column(Modifier.padding(top = 8.dp, bottom = 4.dp)) {
                    Text(
                        text = "游戏库",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "绑定手机里的游戏，设好刷新时间",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (games.isEmpty()) {
                item {
                    EmptyState(
                        title = "还没有游戏",
                        hint = "点右下角的 + ，从手机已安装的应用里挑一个"
                    )
                }
            }

            items(games, key = { it.id }) { game ->
                val gameTasks = tasks.filter { it.gameId == game.id }
                val daily = gameTasks.count { it.type == com.airu.gametodo.data.TaskType.DAILY }
                val weekly = gameTasks.count { it.type == com.airu.gametodo.data.TaskType.WEEKLY }
                val activeCount = activities.count {
                    val end = java.time.LocalDate.ofEpochDay(it.endEpochDay)
                    it.gameId == game.id && !end.isBefore(java.time.LocalDate.now())
                }
                val accent = Color(game.colorArgb)

                Surface(
                    onClick = { onOpenGame(game.id) },
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GameAvatar(icon = game.icon, accent = accent, name = game.name, size = 48.dp)
                        Spacer(Modifier.width(13.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = game.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = buildString {
                                    append("日常 $daily · 周常 $weekly")
                                    if (activeCount > 0) append(" · 活动 $activeCount")
                                    append(" · ${Period.formatHourMinute(game.resetMinuteOfDay)} 刷新")
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
                }
            }
        }

        FloatingActionButton(
            onClick = { showPicker = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "添加游戏")
        }
    }

    if (showPicker) {
        AppPickerDialog(
            onDismiss = { showPicker = false },
            onPick = { app ->
                showPicker = false
                vm.saveGame(
                    GameEntity(
                        name = app.label,
                        packageName = app.packageName,
                        icon = app.icon,
                        colorArgb = GameColors.random().toArgb()
                    )
                ) { id -> onOpenGame(id) }
            },
            onManual = { name ->
                showPicker = false
                vm.saveGame(
                    GameEntity(
                        name = name,
                        colorArgb = GameColors.random().toArgb()
                    )
                ) { id -> onOpenGame(id) }
            }
        )
    }
}

@Composable
private fun AppPickerDialog(
    onDismiss: () -> Unit,
    onPick: (InstalledApp) -> Unit,
    onManual: (String) -> Unit
) {
    val context = LocalContext.current
    var apps by remember { mutableStateOf<List<InstalledApp>?>(null) }
    var query by remember { mutableStateOf("") }
    var manualName by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) { AppLauncher.listLaunchable(context) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(560.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = "选择游戏",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("搜索应用") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))

                val filtered = remember(apps, query) {
                    val list = apps ?: return@remember emptyList()
                    if (query.isBlank()) list else list.filter {
                        it.label.contains(query, ignoreCase = true)
                    }
                }

                Box(Modifier.weight(1f)) {
                    when {
                        apps == null -> Box(
                            Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "正在扫描已安装的应用…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        filtered.isEmpty() -> Box(
                            Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "没找到匹配的应用",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            items(filtered, key = { it.packageName }) { app ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onPick(app) }
                                        .padding(vertical = 9.dp, horizontal = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    GameAvatar(
                                        icon = app.icon,
                                        accent = MaterialTheme.colorScheme.primary,
                                        name = app.label,
                                        size = 38.dp
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            text = app.label,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = app.packageName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 手动添加：绑不到应用时也能建一个纯记录的条目
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = manualName,
                        onValueChange = { manualName = it },
                        placeholder = { Text("手动输入名字") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        onClick = { if (manualName.isNotBlank()) onManual(manualName.trim()) },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "创建",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)
                        )
                    }
                }
            }
        }
    }
}
