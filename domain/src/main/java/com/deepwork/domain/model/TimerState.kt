package com.deepwork.domain.model

sealed interface TimerState {
    data object Idle : TimerState
    data class Running(val remainingMillis: Long, val progress: Float) : TimerState
    data class Paused(val remainingMillis: Long, val progress: Float) : TimerState
    data class Completed(val durationMinutes: Int, val achievement: Achievement?) : TimerState
    data class Error(val message: String) : TimerState
}
