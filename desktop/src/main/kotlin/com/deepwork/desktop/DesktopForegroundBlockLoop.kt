package com.deepwork.desktop

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Cât timp o sesiune e activă pe desktop, dacă fereastra foreground e un .exe blocat,
 * cere reafișarea ferestrei Kara (Windows + JNA).
 */
object DesktopForegroundBlockLoop {
    private var lastOverlayAtMs = 0L
    private var lastOverlayExe: String? = null

    suspend fun run(onBringKaraToFront: () -> Unit) {
        if (!WindowsForegroundExe.isWindowsOs()) return
        while (currentCoroutineContext().isActive) {
            delay(500)
            if (!DesktopBlockedAppsStore.isBlockingConfigured()) {
                DesktopState.clearBlockingOverlay()
                continue
            }
            // Blocăm doar în focus activ (Pomodoro running), nu în pauză.
            if (DesktopLocalSession.phase.value != DesktopSessionPhase.Running) {
                DesktopState.clearBlockingOverlay()
                continue
            }
            val fg = WindowsForegroundExe.foregroundExeLowercase() ?: continue
            val ours = WindowsForegroundExe.ourProcessExeLowercase()
            if (ours != null && fg == ours) continue
            if (DesktopBlockedAppsStore.isBlocked(fg)) {
                maybeShowOverlay(fg)
                onBringKaraToFront()
            } else {
                DesktopState.clearBlockingOverlay()
            }
        }
    }

    private fun maybeShowOverlay(exeLowercase: String) {
        val now = System.currentTimeMillis()
        if (exeLowercase == lastOverlayExe && now - lastOverlayAtMs < 700L) return
        lastOverlayExe = exeLowercase
        lastOverlayAtMs = now
        DesktopState.showBlockingOverlay(exeLowercase)
    }
}
