package com.deepwork.domain.usecase

import com.deepwork.domain.gamification.AchievementCatalog
import com.deepwork.domain.repository.AchievementRepository
import com.deepwork.domain.repository.SessionRepository
import javax.inject.Inject
import kotlin.math.roundToInt

class CompleteSessionUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val achievementRepository: AchievementRepository,
    private val evaluateAchievementsUseCase: EvaluateAchievementsUseCase
) {
    suspend operator fun invoke(
        sessionId: String,
        plannedMinutes: Int,
        elapsedMillis: Long,
        completedFull: Boolean
    ) {
        val existing = sessionRepository.getSessionOnce(sessionId) ?: return
        if (existing.completedAt > 0L) return

        val plannedMillis = plannedMinutes.coerceAtLeast(1) * 60_000L
        val elapsedCoerced = elapsedMillis.coerceIn(0L, plannedMillis)
        val minutesActual = (elapsedCoerced / 60_000L).toInt()

        val minutesForXp = when {
            completedFull -> plannedMinutes.coerceAtLeast(1)
            else -> minutesActual.coerceAtLeast(if (elapsedCoerced > 0) 1 else 0)
        }

        val xpEarned =
            minutesForXp * AchievementCatalog.XP_PER_FOCUS_MINUTE +
                if (completedFull) AchievementCatalog.XP_BONUS_FULL_SESSION else 0

        val focusScore = when {
            plannedMillis <= 0 -> 0
            else ->
                ((elapsedCoerced.toFloat() / plannedMillis.toFloat()) * 100f)
                    .roundToInt()
                    .coerceIn(0, 100)
        }

        val updated = existing.copy(
            completedAt = System.currentTimeMillis(),
            xpEarned = if (elapsedCoerced > 0L) xpEarned else 0,
            focusScore = focusScore
        )
        sessionRepository.updateSession(updated)
        if (updated.xpEarned > 0) {
            achievementRepository.addTotalXp(updated.xpEarned)
        }
        evaluateAchievementsUseCase()
    }
}
