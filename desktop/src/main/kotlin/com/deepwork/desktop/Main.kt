package com.deepwork.desktop

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.MutableState
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

    val windowState: WindowState = rememberWindowState()
    val strictFocusState: MutableState<Boolean> = remember { mutableStateOf(false) }
    val strictFocusEnabled = strictFocusState.value
    val phase by DesktopLocalSession.phase.collectAsState()
    val strictFocusActive = strictFocusEnabled && phase == DesktopSessionPhase.Running

    LaunchedEffect(strictFocusActive) {
        // Fullscreen poate fi instabil pe unele setup-uri Windows/driver.
        // Maximize + alwaysOnTop e suficient pentru "strict focus" fără crash.
        runCatching {
            windowState.placement = if (strictFocusActive) WindowPlacement.Maximized else WindowPlacement.Floating
        }
    }

    Window(
        onCloseRequest = {
            if (!strictFocusActive) {
                exitApplication()
            }
        },
        title = "Kara Companion",
        state = windowState,
        alwaysOnTop = strictFocusActive
    ) {
        DesktopCompanionAppTheme {
            DesktopCompanionApp(
                strictFocusEnabled = strictFocusEnabled,
                onStrictFocusChange = { strictFocusState.value = it }
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
