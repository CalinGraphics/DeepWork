package com.deepwork.data.local.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import com.deepwork.data.local.DeepWorkDatabase
import com.deepwork.data.local.dao.AchievementDao
import com.deepwork.data.local.dao.SessionDao
import com.deepwork.data.local.dao.TaskDao
import com.deepwork.data.local.preferences.dataStore
import com.deepwork.data.local.repository.AchievementRepositoryImpl
import com.deepwork.data.local.repository.SensorRepositoryImpl
import com.deepwork.data.local.repository.SessionRepositoryImpl
import com.deepwork.data.local.repository.TaskRepositoryImpl
import com.deepwork.domain.repository.AchievementRepository
import com.deepwork.domain.repository.SensorRepository
import com.deepwork.domain.repository.SessionRepository
import com.deepwork.domain.repository.TaskRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LocalDatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DeepWorkDatabase {
        return Room.databaseBuilder(
            context,
            DeepWorkDatabase::class.java,
            DeepWorkDatabase.DATABASE_NAME
        ).build()
    }

    @Provides
    fun provideSessionDao(db: DeepWorkDatabase) = db.sessionDao

    @Provides
    fun provideTaskDao(db: DeepWorkDatabase) = db.taskDao

    @Provides
    fun provideAchievementDao(db: DeepWorkDatabase) = db.achievementDao

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class LocalRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSessionRepository(impl: SessionRepositoryImpl): SessionRepository

    @Binds
    @Singleton
    abstract fun bindTaskRepository(impl: TaskRepositoryImpl): TaskRepository

    @Binds
    @Singleton
    abstract fun bindAchievementRepository(impl: AchievementRepositoryImpl): AchievementRepository

    @Binds
    @Singleton
    abstract fun bindSensorRepository(impl: SensorRepositoryImpl): SensorRepository
}
