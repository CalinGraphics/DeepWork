package com.deepwork.domain.repository

import com.deepwork.domain.model.Achievement
import kotlinx.coroutines.flow.Flow

interface AchievementRepository {
    fun getAchievements(): Flow<List<Achievement>>
    suspend fun ensureAchievementsSeeded()
    suspend fun unlockAchievement(achievementId: String)
    suspend fun addTotalXp(delta: Int)
    fun getTotalXp(): Flow<Int>
}
