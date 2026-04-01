package com.deepwork.data.local.repository

import com.deepwork.data.local.dao.SessionDao
import com.deepwork.data.local.entity.toDomain
import com.deepwork.data.local.entity.toEntity
import com.deepwork.domain.model.Session
import com.deepwork.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SessionRepositoryImpl @Inject constructor(
    private val sessionDao: SessionDao
) : SessionRepository {

    override fun getSessions(): Flow<List<Session>> = 
        sessionDao.getAllSessions().map { entities -> entities.map { it.toDomain() } }

    override fun getActiveSession(): Flow<Session?> =
        sessionDao.getActiveDraftSession().map { it?.toDomain() }

    override suspend fun getSessionOnce(sessionId: String): Session? =
        sessionDao.getSession(sessionId)?.toDomain()

    override suspend fun saveSession(session: Session): Result<String> {
        return try {
            sessionDao.insertSession(session.toEntity())
            Result.success(session.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateSession(session: Session) {
        sessionDao.updateSession(session.toEntity())
    }

    override suspend fun deleteSession(sessionId: String) {
        sessionDao.deleteSession(sessionId)
    }
}
