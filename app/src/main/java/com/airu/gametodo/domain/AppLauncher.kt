package com.airu.gametodo.domain

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import java.io.ByteArrayOutputStream

data class InstalledApp(
    val label: String,
    val packageName: String,
    val icon: ByteArray?
)

/**
 * 扫描手机上的可启动应用 & 拉起。
 * 依赖 Manifest 里的 <queries> 声明，否则 Android 11+ 只能看到自己。
 */
object AppLauncher {

    fun listLaunchable(context: Context): List<InstalledApp> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = pm.queryIntentActivities(intent, 0)
        val self = context.packageName
        return resolved
            .mapNotNull { info ->
                val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
                if (pkg == self) return@mapNotNull null
                val label = runCatching { info.loadLabel(pm).toString() }.getOrNull() ?: pkg
                val iconBytes = runCatching { drawableToPng(info.loadIcon(pm)) }.getOrNull()
                InstalledApp(label, pkg, iconBytes)
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    fun drawableToPng(drawable: Drawable, size: Int = 144): ByteArray {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(canvas)
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 92, out)
        bitmap.recycle()
        return out.toByteArray()
    }

    /** 返回 false 表示应用已被卸载或无法启动 */
    fun launch(context: Context, packageName: String?): Boolean {
        if (packageName.isNullOrBlank()) return false
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { context.startActivity(intent) }.isSuccess
    }
}
