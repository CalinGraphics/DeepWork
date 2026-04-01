package com.deepwork.domain.usecase

import com.deepwork.domain.gamification.AchievementCatalog
import com.deepwork.domain.gamification.AchievementIds
import com.deepwork.domain.gamification.StreakCalculator
import com.deepwork.domain.repository.AchievementRepository
import com.deepwork.domain.repository.SessionRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class EvaluateAchievementsUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val achievementRepository: AchievementRepository
) {
    suspend operator fun invoke() {
        achievementRepository.ensureAchievementsSeeded()

        val sessions = sessionRepository.getSessions().first()
        val completedSessions = sessions.filter { it.completedAt > 0L }
        val completedCount = completedSessions.size
        val streak = StreakCalculator.currentStreak(completedSessions)
        val totalXp = achievementRepository.getTotalXp().first()
        val achievements = achievementRepository.getAchievements().first()
        val unlockedIds =
            achievements.filter { it.isUnlocked }.map { it.id }.toMutableSet()

        suspend fun unlockWithReward(id: String, condition: Boolean) {
            if (!condition || id in unlockedIds) return
            achievementRepository.unlockAchievement(id)
            unlockedIds.add(id)
            val bonus = AchievementCatalog.xpRewardFor(id)
            if (bonus > 0) {
                achievementRepository.addTotalXp(bonus)
            }
        }

        unlockWithReward(AchievementIds.FIRST_FOCUS, completedCount >= 1)
        unlockWithReward(AchievementIds.TEN_SESSIONS, completedCount >= 10)
        unlockWithReward(AchievementIds.STREAK_3, streak >= 3)
        unlockWithReward(AchievementIds.STREAK_7, streak >= 7)
        unlockWithReward(AchievementIds.XP_500, totalXp >= 500)
        val totalXpAfter = achievementRepository.getTotalXp().first()
        unlockWithReward(AchievementIds.XP_2000, totalXpAfter >= 2000)
    }
}
