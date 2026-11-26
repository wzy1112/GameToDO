package com.airu.gametodo.widget

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast

/**
 * 小组件里点游戏时的中转页：透明、无 UI，启动完游戏立刻 finish。
 * 直接从广播里 startActivity 在 Android 10+ 可能被后台启动限制拦，走 Activity 最稳。
 */
class LaunchProxyActivity : Activity() {

    companion object {
        const val EXTRA_PACKAGE = "extra_package"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pkg = intent?.getStringExtra(EXTRA_PACKAGE)
        val launched = if (pkg.isNullOrBlank()) false else runCatching {
            packageManager.getLaunchIntentForPackage(pkg)?.let {
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(it)
                true
            } ?: false
        }.getOrDefault(false)

        if (!launched) {
            // 游戏被卸载/改名时给个明确反馈，别让人以为按钮坏了
            Toast.makeText(this, "没找到这个游戏，可能已经卸载了", Toast.LENGTH_SHORT).show()
        }
        finish()
        overridePendingTransition(0, 0)
    }
}
