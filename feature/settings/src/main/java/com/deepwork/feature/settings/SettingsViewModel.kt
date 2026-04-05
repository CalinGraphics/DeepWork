package com.deepwork.feature.settings

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepwork.core.common.KaraAccessibility
import com.deepwork.data.local.preferences.UserPreferences
import com.deepwork.data.local.preferences.UserPreferencesRepository
import com.deepwork.data.remote.client.DeepWorkWebSocketClient
import com.deepwork.data.remote.model.DeepWorkMessage
import com.deepwork.data.remote.model.MessageType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonPrimitive
import javax.inject.Inject

data class LaunchableApp(val packageName: String, val label: String)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val preferencesRepository: UserPreferencesRepository,
    private val webSocketClient: DeepWorkWebSocketClient
) : ViewModel() {

    val preferences: StateFlow<UserPreferences> = preferencesRepository.userPreferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences(false, false, 25, 0)
        )

    val blockedPackages: StateFlow<Set<String>> = preferencesRepository.blockedAppPackagesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptySet()
        )

    private val _launchableApps = MutableStateFlow<List<LaunchableApp>>(emptyList())
    val launchableApps: StateFlow<List<LaunchableApp>> = _launchableApps.asStateFlow()

    private val _accessibilityServiceEnabled = MutableStateFlow(false)
    val accessibilityServiceEnabled: StateFlow<Boolean> = _accessibilityServiceEnabled.asStateFlow()

    fun refreshAccessibilityServiceState() {
        _accessibilityServiceEnabled.value = isFocusBlockServiceEnabled()
    }

    fun refreshLaunchableApps() {
        viewModelScope.launch {
            val list = withContext(Dispatchers.Default) { queryLaunchableApps() }
            _launchableApps.value = list
        }
    }

    private fun queryLaunchableApps(): List<LaunchableApp> {
        val pm = appContext.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        @Suppress("DEPRECATION")
        val resolves = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
            )
        } else {
            pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        }
        val ours = appContext.packageName
        return resolves.asSequence()
            .mapNotNull { ri ->
                val pkg = ri.activityInfo.packageName
                if (pkg == ours) return@mapNotNull null
                LaunchableApp(pkg, ri.loadLabel(pm).toString())
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    fun setPackageBlocked(packageName: String, blocked: Boolean) {
        viewModelScope.launch {
            val cur = preferencesRepository.getBlockedAppPackagesOnce().toMutableSet()
            if (blocked) cur.add(packageName) else cur.remove(packageName)
            preferencesRepository.setBlockedAppPackages(cur)
        }
    }

    fun clearAllBlockedApps() {
        viewModelScope.launch {
            preferencesRepository.setBlockedAppPackages(emptySet())
        }
    }

    private fun isFocusBlockServiceEnabled(): Boolean {
        val cn = ComponentName(appContext.packageName, KaraAccessibility.FOCUS_BLOCK_SERVICE_CLASS)
        val expected = cn.flattenToString()
        val raw = Settings.Secure.getString(
            appContext.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return raw.split(':', ',')
            .map { it.trim() }
            .any { it.equals(expected, ignoreCase = true) }
    }

    fun updateSessionDuration(durationMinutes: Int) {
        viewModelScope.launch {
            preferencesRepository.updateSessionDuration(durationMinutes)
            if (webSocketClient.isConnected) {
                webSocketClient.sendMessage(
                    DeepWorkMessage(
                        type = MessageType.TIMER_SYNC,
                        payload = JsonPrimitive(durationMinutes.coerceIn(5, 120)),
                        deviceId = "android_client"
                    )
                )
            }
        }
    }

    fun replayOnboarding() {
        viewModelScope.launch {
            preferencesRepository.updateOnboardingCompleted(false)
        }
    }
}
