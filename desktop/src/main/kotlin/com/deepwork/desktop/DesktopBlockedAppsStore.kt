package com.deepwork.desktop

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.util.Locale

@Serializable
private data class BlockedAppsFile(
    val enabled: Boolean = true,
    val killBlockedProcesses: Boolean = true,
    val blockedExesLowercase: List<String> = emptyList()
)

/**
 * Persistă aplicațiile blocate pe Windows (nume fișier .exe, lowercase).
 */
object DesktopBlockedAppsStore {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val dir = File(System.getProperty("user.home") ?: ".", ".deepwork").apply { mkdirs() }
    private val file = File(dir, "desktop-blocked-apps.json")
    private val lock = Any()

    private val _enabled = MutableStateFlow(true)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private val _blocked = MutableStateFlow<Set<String>>(emptySet())
    val blocked: StateFlow<Set<String>> = _blocked.asStateFlow()

    private val _killBlockedProcesses = MutableStateFlow(true)
    val killBlockedProcesses: StateFlow<Boolean> = _killBlockedProcesses.asStateFlow()

    init {
        synchronized(lock) {
            runCatching {
                if (!file.exists()) return@runCatching
                val data = json.decodeFromString(BlockedAppsFile.serializer(), file.readText())
                _enabled.value = data.enabled
                _killBlockedProcesses.value = data.killBlockedProcesses
                _blocked.value = data.blockedExesLowercase.map { it.lowercase(Locale.ROOT) }.toSet()
            }
        }
    }

    fun isBlockingConfigured(): Boolean = _enabled.value && _blocked.value.isNotEmpty()

    fun isBlocked(exeLowercase: String): Boolean =
        exeLowercase.lowercase(Locale.ROOT) in _blocked.value

    fun setEnabled(value: Boolean) {
        _enabled.value = value
        persist()
    }

    fun setKillBlockedProcesses(value: Boolean) {
        _killBlockedProcesses.value = value
        persist()
    }

    fun setBlocked(exeLowercase: String, blockedFlag: Boolean) {
        val key = exeLowercase.lowercase(Locale.ROOT)
        _blocked.update { cur ->
            if (blockedFlag) cur + key else cur - key
        }
        persist()
    }

    fun clearAll() {
        _blocked.value = emptySet()
        persist()
    }

    private fun persist() {
        synchronized(lock) {
            runCatching {
                file.writeText(
                    json.encodeToString(
                        BlockedAppsFile.serializer(),
                        BlockedAppsFile(
                            enabled = _enabled.value,
                            killBlockedProcesses = _killBlockedProcesses.value,
                            blockedExesLowercase = _blocked.value.sorted()
                        )
                    )
                )
            }
        }
    }
}
