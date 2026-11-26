package com.airu.gametodo.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.airu.gametodo.MainActivity
import com.airu.gametodo.data.TaskType
import com.airu.gametodo.R
import kotlinx.coroutines.runBlocking

private const val FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

/** 桌面的列表由系统的 ListView 渲染，所以能真滚动、能 fling */
class TodayWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        TodayFactory(applicationContext)
}

class TodayFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    private var items: List<WidgetItem> = emptyList()

    override fun onCreate() = Unit

    override fun onDataSetChanged() {
        items = runCatching {
            runBlocking { WidgetData.loadItems(context) }
        }.getOrDefault(emptyList())
    }

    override fun onDestroy() = Unit

    override fun getCount(): Int = items.size

    override fun getViewAt(position: Int): RemoteViews {
        val layout = R.layout.widget_item
        val item = items.getOrNull(position)
            ?: return RemoteViews(context.packageName, layout)

        val rv = RemoteViews(context.packageName, layout)
        rv.setTextViewText(R.id.wg_task, item.title)
        rv.setTextViewText(R.id.wg_game, item.gameName)

        // 每种任务开一组：分组头带标题和分隔线，第一行也要显示。
        // 复用行时两个分支都要显式设，不然会留着上一行的分组头。
        val isGroupStart = position == 0 || items[position - 1].type != item.type
        if (isGroupStart) {
            rv.setViewVisibility(R.id.wg_group_header, View.VISIBLE)
            rv.setTextViewText(
                R.id.wg_group_title,
                when (item.type) {
                    TaskType.DAILY -> "日常"
                    TaskType.WEEKLY -> "周常"
                    TaskType.ONCE -> "活动"
                }
            )
        } else {
            rv.setViewVisibility(R.id.wg_group_header, View.GONE)
        }

        // 游戏图标：走 content:// 让桌面自己取，避免复用行时沿用上一张图
        val iconUri = WidgetIcons.uri(context, item.packageName)
        if (iconUri == null) {
            rv.setViewVisibility(R.id.wg_icon, View.GONE)
        } else {
            rv.setViewVisibility(R.id.wg_icon, View.VISIBLE)
            rv.setImageViewUri(R.id.wg_icon, iconUri)
        }

        // 勾选框：默认空心，颜色跟着游戏走
        rv.setInt(R.id.wg_check_icon, "setColorFilter", item.colorArgb)

        // 勾选按钮挂自己的 PendingIntent，点它不会串到整行的"打开 App"
        val toggle = Intent(context, TodayWidgetProvider::class.java).apply {
            action = ACTION_TOGGLE
            putExtra(EXTRA_TASK_ID, item.taskId)
        }
        val togglePi = PendingIntent.getBroadcast(context, item.taskId.toInt(), toggle, FLAGS)
        rv.setOnClickPendingIntent(R.id.wg_check, togglePi)
        rv.setOnClickPendingIntent(R.id.wg_check_icon, togglePi)

        // 右侧启动按钮：没绑包名就不显示
        val pkg = item.packageName
        if (pkg.isNullOrBlank()) {
            rv.setViewVisibility(R.id.wg_launch, View.GONE)
        } else {
            rv.setViewVisibility(R.id.wg_launch, View.VISIBLE)
            val launch = Intent(context, LaunchProxyActivity::class.java)
                .putExtra(LaunchProxyActivity.EXTRA_PACKAGE, pkg)
            val launchPi = PendingIntent.getActivity(
                context, 500_000 + item.taskId.toInt(), launch, FLAGS
            )
            rv.setOnClickPendingIntent(R.id.wg_launch, launchPi)
            rv.setOnClickPendingIntent(R.id.wg_launch_icon, launchPi)
        }

        // 整行：打开 App
        val open = Intent(context, MainActivity::class.java)
        rv.setOnClickPendingIntent(
            R.id.wg_item_root,
            PendingIntent.getActivity(context, 400_000 + item.taskId.toInt(), open, FLAGS)
        )
        return rv
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    // 任务增减会让位置整体移位，开着稳定 id 时桌面会拿旧行去复用
    override fun hasStableIds(): Boolean = false
}
