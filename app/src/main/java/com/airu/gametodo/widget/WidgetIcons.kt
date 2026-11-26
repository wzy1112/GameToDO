package com.airu.gametodo.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

/**
 * 游戏图标先落成本地 PNG，再由桌面通过 content:// 取。
 * 直接往 RemoteViews 塞 bitmap 时，MIUI 复用的行不会重设图片，新任务会沿用上一行的图标；
 * 每行给独立 URI 就没这个问题，顺便也不用担心 bitmap 撑爆 binder。
 */
object WidgetIcons {

    const val AUTHORITY = "com.airu.gametodo.icons"

    private const val SIZE_DP = 32
    private const val DIR = "widget_icons"
    private val ILLEGAL = Regex("[^A-Za-z0-9._-]")

    fun uri(context: Context, packageName: String?): Uri? {
        if (packageName.isNullOrBlank()) return null
        val file = file(context, packageName) ?: return null
        return Uri.parse("content://$AUTHORITY/icon/${Uri.encode(packageName)}?v=${file.lastModified()}")
    }

    /** 确保图标已落盘；包名无效或取不到图标时返回 null */
    fun file(context: Context, packageName: String): File? {
        if (packageName.isBlank()) return null
        val dir = File(context.filesDir, DIR)
        val out = File(dir, packageName.replace(ILLEGAL, "_") + "@$SIZE_DP.png")
        if (out.isFile && out.length() > 0L) return out
        val bitmap = runCatching { render(context, packageName) }.getOrNull() ?: return null
        return runCatching {
            dir.mkdirs()
            FileOutputStream(out).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            out
        }.getOrNull()
    }

    private fun render(context: Context, packageName: String): Bitmap {
        val drawable = context.packageManager.getApplicationIcon(packageName)
        val px = (SIZE_DP * context.resources.displayMetrics.density).toInt().coerceIn(64, 160)
        // 自适应图标是 108dp 出血图，可见区只有中间 72dp，直接铺满会被放大裁掉一圈
        val canvasSize = if (drawable is AdaptiveIconDrawable) px * 3 / 2 else px
        val full = Bitmap.createBitmap(canvasSize, canvasSize, Bitmap.Config.ARGB_8888)
        drawable.setBounds(0, 0, canvasSize, canvasSize)
        drawable.draw(Canvas(full))
        if (canvasSize == px) return full
        val offset = (canvasSize - px) / 2
        return Bitmap.createBitmap(full, offset, offset, px, px)
    }
}
