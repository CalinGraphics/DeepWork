package com.deepwork.desktop

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.MutableState
import kotlinx.coroutines.delay
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
    // Mod strict cât timp există sesiune activă (rulează sau în pauză), nu doar în Running — altfel la Pause fereastra „cădea” din fullscreen.
    val strictFocusActive = strictFocusEnabled && phase != DesktopSessionPhase.Idle

    LaunchedEffect(strictFocusActive) {
        // Pe Windows nu există echivalent Lock Task ca pe Android (nu putem bloca Alt+Tab la nivel de OS din JVM).
        // Mod strict = ecran complet + mereu deasupra + fereastră fixă — reduce distragerile, dar nu „încuie” sistemul.
        delay(32)
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
        onCloseRequest = {
            if (!strictFocusActive) {
                exitApplication()
            }
        },
        title = "Kara Companion",
        state = windowState,
        alwaysOnTop = strictFocusActive,
        resizable = !strictFocusActive
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
