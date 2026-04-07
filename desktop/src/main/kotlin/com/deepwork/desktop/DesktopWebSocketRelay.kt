package com.deepwork.desktop

import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.Frame
import io.ktor.websocket.send
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive

private val relayJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
}

/**
 * Păstrează sesiunea WebSocket activă ca să putem trimite mesaje către telefon (ex. durată timer).
 */
object DesktopWebSocketRelay {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val lock = Any()
    private var active: DefaultWebSocketServerSession? = null

    fun attach(session: DefaultWebSocketServerSession) {
        synchronized(lock) { active = session }
    }

    fun detach(session: DefaultWebSocketServerSession) {
        synchronized(lock) {
            if (active === session) active = null
        }
    }

    fun hasClient(): Boolean = synchronized(lock) { active != null }

    fun broadcastTimerSyncMinutes(minutes: Int) {
        val session = synchronized(lock) { active } ?: return
        val msg = DeepWorkMessage(
            type = MessageType.TIMER_SYNC,
            payload = JsonPrimitive(minutes.coerceIn(1, 360)),
            deviceId = "desktop_companion"
        )
        val text = relayJson.encodeToString(DeepWorkMessage.serializer(), msg)
        scope.launch {
            runCatching { session.send(Frame.Text(text)) }
        }
    }
}
