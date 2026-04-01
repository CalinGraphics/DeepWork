package com.deepwork.sync

import com.deepwork.data.local.preferences.UserPreferencesRepository
import com.deepwork.data.remote.client.DeepWorkWebSocketClient
import com.deepwork.data.remote.model.MessageType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Aplică TIMER_SYNC de la companion desktop la preferințele locale (durată sesiune).
 */
@Singleton
class RemoteSessionDurationSync @Inject constructor(
    private val webSocketClient: DeepWorkWebSocketClient,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        scope.launch {
            webSocketClient.incomingMessages.collect { msg ->
                if (msg.type != MessageType.TIMER_SYNC) return@collect
                val minutes = (msg.payload as? JsonPrimitive)?.content?.toIntOrNull()?.coerceIn(5, 120)
                    ?: return@collect
                userPreferencesRepository.updateSessionDuration(minutes)
            }
        }
    }
}
