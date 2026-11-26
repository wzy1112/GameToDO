package com.airu.gametodo.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class Converters {
    @TypeConverter
    fun toType(value: String): TaskType = TaskType.valueOf(value)

    @TypeConverter
    fun fromType(value: TaskType): String = value.name
}

@Database(
    entities = [GameEntity::class, TaskEntity::class, ActivityEntity::class, LogEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
    abstract fun taskDao(): TaskDao
    abstract fun activityDao(): ActivityDao
    abstract fun logDao(): LogDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "gametodo.db"
            ).build().also { instance = it }
        }
    }
}
