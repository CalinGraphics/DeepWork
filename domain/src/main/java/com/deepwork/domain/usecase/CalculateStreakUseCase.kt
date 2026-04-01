package com.deepwork.domain.usecase

import com.deepwork.domain.gamification.StreakCalculator
import com.deepwork.domain.repository.AchievementRepository
import com.deepwork.domain.repository.SessionRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import kotlin.math.roundToInt

class CalculateStreakUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val achievementRepository: AchievementRepository
) {
    suspend operator fun invoke(): StreakResult {
        val sessions = sessionRepository.getSessions().first()
        val completed = sessions.filter { it.completedAt > 0L }
        val streak = StreakCalculator.currentStreak(completed)
        val totalXp = achievementRepository.getTotalXp().first()
        val focusScore =
            if (completed.isEmpty()) {
                0
            } else {
                completed.take(10).map { it.focusScore }.average().roundToInt()
            }
        return StreakResult(
            currentStreak = streak,
            totalXp = totalXp,
            focusScore = focusScore
        )
    }
}

data class StreakResult(
    val currentStreak: Int,
    val totalXp: Int,
    val focusScore: Int
)
