package com.deepwork.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.deepwork.domain.model.Session
import com.deepwork.domain.model.Task
import com.deepwork.domain.model.Achievement

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val durationMinutes: Int,
    val completedAt: Long,
    val xpEarned: Int,
    val focusScore: Int,
    val isSynced: Boolean
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val category: String,
    val estimatedMinutes: Int,
    val isCompleted: Boolean,
    val createdAt: Long
)

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val id: String,
    val title: String,
    val xpReward: Int,
    val lottieAnimationUrl: String,
    val isUnlocked: Boolean,
    val unlockedAt: Long?
)

fun SessionEntity.toDomain() = Session(id, durationMinutes, completedAt, xpEarned, focusScore, isSynced)
fun Session.toEntity() = SessionEntity(id, durationMinutes, completedAt, xpEarned, focusScore, isSynced)

fun TaskEntity.toDomain() = Task(id, title, category, estimatedMinutes, isCompleted, createdAt)
fun Task.toEntity() = TaskEntity(id, title, category, estimatedMinutes, isCompleted, createdAt)

fun AchievementEntity.toDomain() = Achievement(id, title, xpReward, lottieAnimationUrl, isUnlocked, unlockedAt)
fun Achievement.toEntity() = AchievementEntity(id, title, xpReward, lottieAnimationUrl, isUnlocked, unlockedAt)
