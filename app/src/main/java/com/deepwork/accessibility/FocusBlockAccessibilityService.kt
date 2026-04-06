package com.deepwork.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import com.deepwork.MainActivity
import com.deepwork.data.local.preferences.UserPreferencesRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class FocusBlockAccessibilityService : AccessibilityService() {

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    private var lastBlockAt = 0L
    private var lastBlockedPackage: String? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val ev = event ?: return
        if (
            ev.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            ev.eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) return

        val pkg = ev.packageName?.toString()
            ?: ev.source?.packageName?.toString()
            ?: return
        if (pkg == packageName) return

        val (blockingActive, blocked) = runBlocking(Dispatchers.IO) {
            userPreferencesRepository.isFocusBlockingActiveOnce() to
                userPreferencesRepository.getBlockedAppPackagesOnce()
        }
        if (!blockingActive) return
        if (blocked.isEmpty() || pkg !in blocked) return

        val now = SystemClock.elapsedRealtime()
        if (pkg == lastBlockedPackage && now - lastBlockAt < 300L) return
        lastBlockedPackage = pkg
        lastBlockAt = now

        // Împinge imediat utilizatorul în afara aplicației blocate.
        runCatching { performGlobalAction(GLOBAL_ACTION_HOME) }
        runCatching { performGlobalAction(GLOBAL_ACTION_BACK) }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
    }

    override fun onInterrupt() = Unit
}
