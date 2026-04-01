package com.deepwork.domain.repository

import com.deepwork.domain.model.GestureType
import kotlinx.coroutines.flow.Flow

interface SensorRepository {
    fun startListening()
    fun stopListening()
    fun getGestureFlow(): Flow<GestureType>
}
