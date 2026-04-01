package com.deepwork.data.remote.client

import com.deepwork.data.remote.model.DeepWorkMessage
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeepWorkWebSocketClient @Inject constructor() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val client = HttpClient(Android) {
        install(WebSockets)
    }

    private var session: io.ktor.client.plugins.websocket.ClientWebSocketSession? = null

    val isConnected: Boolean get() = session != null

    private val _incomingMessages = MutableSharedFlow<DeepWorkMessage>()
    val incomingMessages: Flow<DeepWorkMessage> = _incomingMessages.asSharedFlow()

    suspend fun connect(ipAddress: String): Result<Unit> {
        return try {
            session = client.webSocketSession(
                host = ipAddress,
                port = 8080,
                path = "/deepwork"
            )
            
            // Start listening for messages
            scope.launch {
                listenForMessages()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun listenForMessages() {
        try {
            session?.let { activeSession ->
                for (frame in activeSession.incoming) {
                    if (frame is Frame.Text) {
                        val message = Json.decodeFromString<DeepWorkMessage>(frame.readText())
                        _incomingMessages.emit(message)
                    }
                }
            }
        } catch (e: Exception) {
            // Handle disconnect
            session = null
        }
    }

    suspend fun sendMessage(message: DeepWorkMessage): Result<Unit> {
        val active = session ?: return Result.success(Unit)
        return try {
            active.send(Frame.Text(Json.encodeToString(message)))
            Result.success(Unit)
        } catch (_: Exception) {
            session = null
            Result.success(Unit)
        }
    }

    suspend fun disconnect() {
        session?.close()
        session = null
    }

    fun shutdown() {
        scope.cancel()
        client.close()
    }
}
