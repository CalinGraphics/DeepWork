package com.deepwork.desktop

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.MutableState
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.net.NetworkInterface

fun main() = application {
    startServer()
    val ip = getLocalIpAddress()
    DesktopState.setPairingUrl("ws://$ip:8080/deepwork")
    LaunchedEffect(Unit) {
        DesktopSessionAlerts.ensureTray()
        while (true) {
            DesktopState.setUsbBridgeStatus(DesktopUsbBridge.ensureAdbReverse())
            delay(15_000)
        }
    }

    // Strict Focus implicit ON: la pornire intră direct în mod immersiv; îl poți opri din footer.
    val strictFocusState: MutableState<Boolean> = remember { mutableStateOf(true) }
    val strictFocusEnabled = strictFocusState.value
    val strictFocusActive = strictFocusEnabled

    val windowState: WindowState = rememberWindowState(
        placement = if (strictFocusEnabled) WindowPlacement.Fullscreen else WindowPlacement.Floating
    )

    LaunchedEffect(strictFocusActive) {
        // Fără delay: aplică imediat schimbarea de placement (Windows poate cădea înapoi pe Maximized).
        runCatching {
            windowState.placement = if (strictFocusActive) {
                WindowPlacement.Fullscreen
            } else {
                WindowPlacement.Floating
            }
        }.onFailure {
            runCatching {
                windowState.placement = if (strictFocusActive) {
                    WindowPlacement.Maximized
                } else {
                    WindowPlacement.Floating
                }
            }
        }
    }

    Window(
        onCloseRequest = { exitApplication() },
        title = "Kara Companion",
        state = windowState,
        alwaysOnTop = strictFocusActive,
        resizable = !strictFocusActive
    ) {
        LaunchedEffect(strictFocusActive) {
            if (!strictFocusActive) return@LaunchedEffect
            yield()
            runCatching {
                window.toFront()
                window.requestFocus()
            }
        }
        DesktopCompanionAppTheme {
            DesktopCompanionApp(
                strictFocusEnabled = strictFocusEnabled,
                onStrictFocusChange = { strictFocusState.value = it },
                onRequestFocusForBlocking = {
                    java.awt.EventQueue.invokeLater {
                        runCatching {
                            window.toFront()
                            window.requestFocus()
                        }
                    }
                }
            )
        }
    }
}

fun getLocalIpAddress(): String {
    try {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        while (interfaces.hasMoreElements()) {
            val networkInterface = interfaces.nextElement()
            if (networkInterface.isLoopback || !networkInterface.isUp) continue
            val addresses = networkInterface.inetAddresses
            while (addresses.hasMoreElements()) {
                val address = addresses.nextElement()
                if (address is java.net.Inet4Address) {
                    return address.hostAddress ?: "127.0.0.1"
                }
            }
        }
    } catch (_: Exception) {
        return "127.0.0.1"
    }
    return "127.0.0.1"
}
