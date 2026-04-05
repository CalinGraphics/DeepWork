package com.deepwork.data.remote.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class DeepWorkMessage(
    val type: MessageType,
    val payload: JsonElement,
    val timestamp: Long = System.currentTimeMillis(),
    val deviceId: String
)

@Serializable
enum class MessageType {
    TIMER_START,
    TIMER_PAUSE,
    TIMER_STOP,
    TIMER_COMPLETED,
    TIMER_SYNC,
    GYRO_DATA,
    GESTURE_ACTION,
    SESSION_UPDATE,
    HAPTIC_TRIGGER,
    TASK_UPDATE
}
