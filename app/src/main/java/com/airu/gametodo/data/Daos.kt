package com.airu.gametodo.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM games ORDER BY sortOrder ASC, id ASC")
    fun observeAll(): Flow<List<GameEntity>>

    @Query("SELECT * FROM games ORDER BY sortOrder ASC, id ASC")
    suspend fun getAll(): List<GameEntity>

    @Query("SELECT * FROM games WHERE id = :id")
    suspend fun getById(id: Long): GameEntity?

    @Insert
    suspend fun insert(game: GameEntity): Long

    @Update
    suspend fun update(game: GameEntity)

    @Delete
    suspend fun delete(game: GameEntity)

    @Query("DELETE FROM games WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE archived = 0 ORDER BY sortOrder ASC, id ASC")
    fun observeAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE archived = 0 ORDER BY sortOrder ASC, id ASC")
    suspend fun getAll(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: Long): TaskEntity?

    @Insert
    suspend fun insert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Delete
    suspend fun delete(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM tasks WHERE activityId = :activityId")
    suspend fun deleteByActivity(activityId: Long)
}

@Dao
interface ActivityDao {
    @Query("SELECT * FROM activities ORDER BY startEpochDay ASC, id ASC")
    fun observeAll(): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM activities ORDER BY startEpochDay ASC, id ASC")
    suspend fun getAll(): List<ActivityEntity>

    @Insert
    suspend fun insert(activity: ActivityEntity): Long

    @Update
    suspend fun update(activity: ActivityEntity)

    @Query("DELETE FROM activities WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface LogDao {
    @Query("SELECT * FROM logs")
    fun observeAll(): Flow<List<LogEntity>>

    @Query("SELECT * FROM logs")
    suspend fun getAll(): List<LogEntity>

    @Query("SELECT * FROM logs WHERE taskId = :taskId AND periodKey = :key LIMIT 1")
    suspend fun find(taskId: Long, key: String): LogEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(log: LogEntity): Long

    @Query("DELETE FROM logs WHERE taskId = :taskId AND periodKey = :key")
    suspend fun delete(taskId: Long, key: String)
}
