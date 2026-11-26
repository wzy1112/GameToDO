package com.airu.gametodo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.airu.gametodo.domain.ReminderScheduler
import com.airu.gametodo.ui.AppRoot
import com.airu.gametodo.ui.theme.GameTodoTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onResume() {
        super.onResume()
        // 回到前台顺手把桌面小组件刷新一次，跨天 / 刷新点后能对上
        com.airu.gametodo.widget.WidgetRefresh.trigger(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        ReminderScheduler.ensureChannel(this)
        ReminderScheduler.rescheduleAll(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (intent?.getStringExtra("seed") == "1") {
            lifecycleScope.launch { com.airu.gametodo.data.DebugSeed.seed(this@MainActivity) }
        }

        setContent {
            GameTodoTheme {
                AppRoot()
            }
        }
    }
}
