package com.deepwork.domain.repository

import com.deepwork.domain.model.Session
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    fun getSessions(): Flow<List<Session>>
    fun getActiveSession(): Flow<Session?>
    suspend fun getSessionOnce(sessionId: String): Session?
    suspend fun saveSession(session: Session): Result<String>
    suspend fun updateSession(session: Session)
    suspend fun deleteSession(sessionId: String)
}
