package com.deepwork.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.deepwork.data.local.dao.AchievementDao
import com.deepwork.data.local.dao.SessionDao
import com.deepwork.data.local.dao.TaskDao
import com.deepwork.data.local.entity.AchievementEntity
import com.deepwork.data.local.entity.SessionEntity
import com.deepwork.data.local.entity.TaskEntity

@Database(
    entities = [SessionEntity::class, TaskEntity::class, AchievementEntity::class],
    version = 1,
    exportSchema = false
)
abstract class DeepWorkDatabase : RoomDatabase() {
    abstract val sessionDao: SessionDao
    abstract val taskDao: TaskDao
    abstract val achievementDao: AchievementDao
    
    companion object {
        const val DATABASE_NAME = "deepwork_db"
    }
}
