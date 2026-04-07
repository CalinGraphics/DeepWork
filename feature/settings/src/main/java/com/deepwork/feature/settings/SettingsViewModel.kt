package com.deepwork.feature.settings

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import java.util.Locale
import javax.inject.Inject

data class LaunchableApp(
    val packageName: String,
    val label: String
)

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
        @Suppress("DEPRECATION")
        val installed = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledApplications(
                PackageManager.ApplicationInfoFlags.of(0L)
            )
        } else {
            pm.getInstalledApplications(0)
        }
        val ours = appContext.packageName
        val userInstalled = installed.asSequence()
            .mapNotNull { ai ->
                val pkg = ai.packageName
                if (pkg == ours) return@mapNotNull null
                val label = runCatching { ai.loadLabel(pm).toString() }
                    .getOrDefault(pkg)
                    .ifBlank { pkg }
                val isSystem = (ai.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
                val isUpdatedSystem =
                    (ai.flags and android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                if (isSystem || isUpdatedSystem) return@mapNotNull null
                LaunchableApp(pkg, label)
            }
            .toMutableList()

        // Fallback robust: include si aplicatiile lansabile din launcher (utile cand OEM ascunde pachete la installed apps).
        @Suppress("DEPRECATION")
        val launchables = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
            )
        } else {
            pm.queryIntentActivities(
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
                PackageManager.MATCH_DEFAULT_ONLY
            )
        }.asSequence()
            .mapNotNull { ri ->
                val pkg = ri.activityInfo?.packageName ?: return@mapNotNull null
                if (pkg == ours) return@mapNotNull null
                val label = runCatching { ri.loadLabel(pm).toString() }.getOrDefault(pkg).ifBlank { pkg }
                LaunchableApp(pkg, label)
            }
            .toList()

        val merged = (userInstalled + launchables)
            .distinctBy { it.packageName }
            .sortedWith(compareBy<LaunchableApp> { it.label.lowercase(Locale.ROOT) }.thenBy { it.packageName })

        // Dacă tot e gol, întoarce măcar tot ce vede PackageManager (fără filtrare) ca fallback de ultim nivel.
        if (merged.isNotEmpty()) return merged

        return installed.asSequence()
            .mapNotNull { ai ->
                val pkg = ai.packageName
                if (pkg == ours) return@mapNotNull null
                val label = runCatching { ai.loadLabel(pm).toString() }.getOrDefault(pkg).ifBlank { pkg }
                LaunchableApp(pkg, label)
            }
            .distinctBy { it.packageName }
            .sortedWith(compareBy<LaunchableApp> { it.label.lowercase(Locale.ROOT) }.thenBy { it.packageName })
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
                        payload = JsonPrimitive(durationMinutes.coerceIn(1, 360)),
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
