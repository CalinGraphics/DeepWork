package com.deepwork.desktop

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.serialization.json.Json
import java.net.BindException
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.seconds

private val wsJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

fun Application.module() {
    install(WebSockets) {
        pingPeriod = 15.seconds
        timeout = 15.seconds
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    routing {
        webSocket("/deepwork") {
            DesktopState.setConnected(true)
            DesktopState.updateStatus("Client Connected! DeepWork session active.")
            try {
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        try {
                            val message = wsJson.decodeFromString<DeepWorkMessage>(text)
                            DesktopState.onMessage(message)
                            DesktopLocalSession.applyRemoteMessage(message)
                        } catch (_: Exception) {
                            DesktopState.updateStatus("Received: $text")
                        }
                    }
                }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    DesktopState.updateStatus("Error: ${e.message}")
                }
            } finally {
                DesktopState.setConnected(false)
                DesktopState.updateStatus("Client Disconnected.")
            }
        }
    }
}

fun startServer() {
    try {
        embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module).start(wait = false)
        DesktopState.updateStatus("Server ready on :8080")
    } catch (e: BindException) {
        // Dacă portul e ocupat (altă instanță rulează), nu oprim aplicația UI.
        DesktopState.setConnected(false)
        DesktopState.updateStatus("Server port 8080 busy — running without server")
    } catch (_: Exception) {
        DesktopState.setConnected(false)
        DesktopState.updateStatus("Server failed — running without server")
    }
}
