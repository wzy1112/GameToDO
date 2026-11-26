package com.airu.gametodo.widget

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** App 里改了数据之后，把桌面小组件同步重画一次 */
object WidgetRefresh {
    fun trigger(context: Context) {
        val app = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { WidgetUpdater.updateAll(app) }
        }
    }
}
