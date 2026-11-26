package com.airu.gametodo.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.RemoteViews
import com.airu.gametodo.MainActivity
import com.airu.gametodo.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

const val ACTION_TOGGLE = "com.airu.gametodo.widget.TOGGLE"
const val ACTION_REFRESH = "com.airu.gametodo.widget.REFRESH"
const val EXTRA_TASK_ID = "task_id"

private const val TAG = "GameTodoWidget"
private const val FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

class TodayWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        // 查库不能在主线程 runBlocking，onUpdate 直接返回，更新在 IO 里做完
        val app = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch { WidgetUpdater.updateAll(app) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_TOGGLE, ACTION_REFRESH -> {
                // 广播里查库要异步，goAsync 保证进程别被提前回收
                val pending = goAsync()
                val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
                val isToggle = intent.action == ACTION_TOGGLE
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        if (isToggle) WidgetData.toggle(context, taskId)
                        WidgetUpdater.updateAll(context)
                    } catch (e: Exception) {
                        Log.w(TAG, "toggle/refresh failed", e)
                    } finally {
                        pending.finish()
                    }
                }
            }

            else -> super.onReceive(context, intent)
        }
    }
}

object WidgetUpdater {

    fun updateAll(context: Context) {
        val app = context.applicationContext
        val manager = AppWidgetManager.getInstance(app)
        val comp = ComponentName(app, TodayWidgetProvider::class.java)
        val ids = manager.getAppWidgetIds(comp)
        if (ids.isEmpty()) return

        val items = loadItems(app) ?: return
        // 内容指纹当列表版本：没变就不重建列表，滚动位置能留住
        val version = items.joinToString("|") { "${it.taskId}:${it.title}" }.hashCode()
        ids.forEach { applyWidget(app, manager, it, items.size, version) }
        Log.d(TAG, "update count=${items.size} v=$version ids=${ids.toList()}")
    }

    private fun loadItems(context: Context): List<WidgetItem>? =
        runCatching { runBlocking { WidgetData.loadItems(context) } }
            .onFailure { Log.w(TAG, "loadItems failed", it) }
            .getOrNull()

    private fun applyWidget(
        context: Context,
        manager: AppWidgetManager,
        widgetId: Int,
        pendingCount: Int,
        version: Int
    ) {
        try {
            manager.updateAppWidget(widgetId, buildViews(context, widgetId, pendingCount, version))
        } catch (e: Exception) {
            Log.w(TAG, "update widget $widgetId failed", e)
        }
    }

    private fun buildViews(
        context: Context,
        widgetId: Int,
        pendingCount: Int,
        version: Int
    ): RemoteViews {
        val pkg = context.packageName
        val rv = RemoteViews(pkg, R.layout.widget_main)

        rv.setTextViewText(
            R.id.wg_count,
            if (pendingCount == 0) "今日全清" else "还剩 $pendingCount 项"
        )
        rv.setOnClickPendingIntent(R.id.wg_header, openApp(context, widgetId))
        rv.setOnClickPendingIntent(R.id.wg_refresh, refresh(context, widgetId))

        // 列表数据交给 RemoteViewsService，滚动由桌面自己处理。
        // 只靠 notifyAppWidgetViewDataChanged 时 MIUI 桌面会把通知丢掉，头部更新了列表还是旧的
        // （或者反过来）。把版本塞进 data，桌面就会在同一次 update 里重建列表。
        val listIntent = Intent(context, TodayWidgetService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            data = Uri.parse("gametodo://widget/$widgetId/$version")
        }
        rv.setRemoteAdapter(android.R.id.list, listIntent)
        rv.setEmptyView(android.R.id.list, R.id.wg_empty)
        return rv
    }

    private fun openApp(context: Context, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
        return PendingIntent.getActivity(context, requestCode, intent, FLAGS)
    }

    private fun refresh(context: Context, widgetId: Int): PendingIntent {
        val intent = Intent(context, TodayWidgetProvider::class.java).apply {
            action = ACTION_REFRESH
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        }
        return PendingIntent.getBroadcast(context, 200_000 + widgetId, intent, FLAGS)
    }
}
