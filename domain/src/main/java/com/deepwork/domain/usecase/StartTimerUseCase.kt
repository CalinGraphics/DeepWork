package com.deepwork.domain.usecase

import com.deepwork.domain.model.Session
import com.deepwork.domain.repository.SessionRepository
import javax.inject.Inject

class StartTimerUseCase @Inject constructor(
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(durationMinutes: Int): Result<Session> {
        if (durationMinutes !in 1..120) {
            return Result.failure(IllegalArgumentException("Duration must be between 1 and 120 minutes"))
        }
        
        val session = Session(
            durationMinutes = durationMinutes,
            completedAt = 0L,
            xpEarned = 0,
            focusScore = 0
        )
        val result = sessionRepository.saveSession(session)
        
        return if (result.isSuccess) {
            Result.success(session)
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Failed to save session"))
        }
    }
}
