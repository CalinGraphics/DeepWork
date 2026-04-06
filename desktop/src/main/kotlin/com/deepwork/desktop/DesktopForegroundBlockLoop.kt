package com.deepwork.desktop

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Cât timp o sesiune e activă pe desktop, dacă fereastra foreground e un .exe blocat,
 * cere reafișarea ferestrei Kara (Windows + JNA).
 */
object DesktopForegroundBlockLoop {
    private var lastKillAtMs = 0L
    private var lastKilledExe: String? = null

    suspend fun run(onBringKaraToFront: () -> Unit) {
        if (!WindowsForegroundExe.isWindowsOs()) return
        while (currentCoroutineContext().isActive) {
            delay(800)
            if (!DesktopBlockedAppsStore.isBlockingConfigured()) continue
            val fg = WindowsForegroundExe.foregroundExeLowercase() ?: continue
            val ours = WindowsForegroundExe.ourProcessExeLowercase()
            if (ours != null && fg == ours) continue
            if (DesktopBlockedAppsStore.isBlocked(fg)) {
                killExeIfAllowed(fg)
                onBringKaraToFront()
            }
        }
    }

    private fun killExeIfAllowed(exeLowercase: String) {
        if (!DesktopBlockedAppsStore.killBlockedProcesses.value) return
        val now = System.currentTimeMillis()
        if (exeLowercase == lastKilledExe && now - lastKillAtMs < 1_500L) return
        lastKilledExe = exeLowercase
        lastKillAtMs = now
        runCatching {
            ProcessBuilder(
                "cmd",
                "/c",
                "taskkill /F /IM $exeLowercase /T"
            ).redirectErrorStream(true).start().waitFor()
        }
    }
}
