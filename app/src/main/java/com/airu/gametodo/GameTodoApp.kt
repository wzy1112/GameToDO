package com.airu.gametodo

import android.app.Application
import com.airu.gametodo.data.AppDatabase
import com.airu.gametodo.domain.ReminderScheduler

class GameTodoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppDatabase.get(this)
        ReminderScheduler.ensureChannel(this)
    }
}
