package com.deepwork.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import com.deepwork.MainActivity
import com.deepwork.core.common.FocusSessionGate
import com.deepwork.data.local.preferences.UserPreferencesRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class FocusBlockAccessibilityService : AccessibilityService() {

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    private var lastRedirectAt = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val ev = event ?: return
        if (ev.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        if (!FocusSessionGate.isFocusSessionActive()) return

        val pkg = ev.packageName?.toString() ?: return
        if (pkg == packageName) return

        val blocked = runBlocking(Dispatchers.IO) {
            userPreferencesRepository.getBlockedAppPackagesOnce()
        }
        if (blocked.isEmpty() || pkg !in blocked) return

        val now = SystemClock.elapsedRealtime()
        if (now - lastRedirectAt < 450L) return
        lastRedirectAt = now

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
    }

    override fun onInterrupt() = Unit
}
