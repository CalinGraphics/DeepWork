package com.deepwork.data.local.repository

import com.deepwork.data.local.dao.AchievementDao
import com.deepwork.data.local.entity.toDomain
import com.deepwork.data.local.entity.toEntity
import com.deepwork.data.local.preferences.UserPreferencesRepository
import com.deepwork.domain.gamification.AchievementCatalog
import com.deepwork.domain.model.Achievement
import com.deepwork.domain.repository.AchievementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AchievementRepositoryImpl @Inject constructor(
    private val achievementDao: AchievementDao,
    private val userPreferencesRepository: UserPreferencesRepository
) : AchievementRepository {

    override fun getAchievements(): Flow<List<Achievement>> =
        achievementDao.getAllAchievements().map { entities -> entities.map { it.toDomain() } }

    override suspend fun ensureAchievementsSeeded() {
        if (achievementDao.achievementCount() == 0) {
            val seed = AchievementCatalog.defaultAchievements().map { it.toEntity() }
            achievementDao.insertAchievements(seed)
        }
    }

    override suspend fun unlockAchievement(achievementId: String) {
        achievementDao.unlockAchievement(achievementId, System.currentTimeMillis())
    }

    override suspend fun addTotalXp(delta: Int) {
        userPreferencesRepository.addTotalXp(delta)
    }

    override fun getTotalXp(): Flow<Int> = userPreferencesRepository.totalXpFlow
}
