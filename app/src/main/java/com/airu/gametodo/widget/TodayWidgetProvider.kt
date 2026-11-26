package com.airu.gametodo.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
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

private const val FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

class TodayWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        WidgetUpdater.updateAll(context)
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
                        // 小组件出错不该拖垮桌面，静默失败
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
        val ids = manager.getAppWidgetIds(ComponentName(app, TodayWidgetProvider::class.java))
        if (ids.isEmpty()) return
        val items = runCatching { runBlocking { WidgetData.loadItems(app) } }.getOrDefault(emptyList())
        ids.forEach { updateOne(app, manager, it, items.size) }
    }

    private fun updateOne(
        context: Context,
        manager: AppWidgetManager,
        widgetId: Int,
        pendingCount: Int
    ) {
        val pkg = context.packageName
        val rv = RemoteViews(pkg, R.layout.widget_main)

        rv.setTextViewText(
            R.id.wg_count,
            if (pendingCount == 0) "今日全清" else "还剩 $pendingCount 项"
        )
        rv.setOnClickPendingIntent(R.id.wg_header, openApp(context, widgetId))
        rv.setOnClickPendingIntent(R.id.wg_refresh, refresh(context, widgetId))

        // 列表数据交给 RemoteViewsService，滚动由桌面自己处理
        val listIntent = Intent(context, TodayWidgetService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
        }
        rv.setRemoteAdapter(android.R.id.list, listIntent)
        rv.setEmptyView(android.R.id.list, R.id.wg_empty)

        manager.updateAppWidget(widgetId, rv)
        manager.notifyAppWidgetViewDataChanged(widgetId, android.R.id.list)
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
