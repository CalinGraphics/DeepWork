package com.deepwork.desktop

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.util.Locale

@Serializable
data class DesktopInstalledAppRow(
    val name: String,
    /** Numele fișierului .exe, lowercase (ex. chrome.exe). */
    val exe: String
)

/** Citește aplicații instalate direct din registry (robust, fără PowerShell parsing). */
object DesktopInstalledAppsScanner {
    private val roots = listOf(
        "HKLM\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall",
        "HKLM\\SOFTWARE\\WOW6432Node\\Microsoft\\Windows\\CurrentVersion\\Uninstall",
        "HKCU\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall"
    )

    suspend fun scan(): Result<List<DesktopInstalledAppRow>> = withContext(Dispatchers.IO) {
        if (!WindowsForegroundExe.isWindowsOs()) {
            return@withContext Result.success(emptyList())
        }
        runCatching {
            val rows = linkedMapOf<String, DesktopInstalledAppRow>()
            for (root in roots) {
                val proc = ProcessBuilder("reg", "query", root, "/s")
                    .redirectErrorStream(true)
                    .start()
                val text = proc.inputStream.bufferedReader().readText()
                proc.waitFor()
                parseRegistryDump(text).forEach { row ->
                    rows.putIfAbsent("${row.name}|${row.exe}", row)
                }
            }
            rows.values.sortedBy { it.name.lowercase(Locale.ROOT) }
        }
    }

    private fun parseRegistryDump(dump: String): List<DesktopInstalledAppRow> {
        val out = mutableListOf<DesktopInstalledAppRow>()
        var displayName: String? = null
        var displayIcon: String? = null

        fun flushCurrent() {
            val name = displayName?.trim().orEmpty()
            val icon = displayIcon?.trim().orEmpty()
            if (name.isEmpty() || icon.isEmpty()) return
            val exe = normalizeExeFromDisplayIcon(icon) ?: return
            if (isNoiseExe(exe)) return
            out += DesktopInstalledAppRow(name = name, exe = exe)
        }

        dump.lineSequence().forEach { raw ->
            val line = raw.trimEnd()
            if (line.isBlank()) {
                flushCurrent()
                displayName = null
                displayIcon = null
                return@forEach
            }
            if (!line.startsWith("HKEY_", ignoreCase = true)) {
                val t = line.trimStart()
                when {
                    t.startsWith("DisplayName", ignoreCase = true) -> {
                        displayName = t.substringAfter("REG_SZ", "").trim().ifBlank { null }
                    }
                    t.startsWith("DisplayIcon", ignoreCase = true) -> {
                        displayIcon = t.substringAfter("REG_SZ", "").trim().ifBlank { null }
                    }
                }
            }
        }
        flushCurrent()
        return out
    }

    private fun normalizeExeFromDisplayIcon(raw: String): String? {
        var s = raw.trim().replace("\"", "")
        val comma = s.lastIndexOf(',')
        if (comma > 1) {
            val suffix = s.substring(comma + 1).trim()
            if (suffix.toIntOrNull() != null) s = s.substring(0, comma)
        }
        val slash = maxOf(s.lastIndexOf('\\'), s.lastIndexOf('/'))
        val name = if (slash >= 0) s.substring(slash + 1) else s
        val exe = name.lowercase(Locale.ROOT)
        return exe.takeIf { it.endsWith(".exe") }
    }

    private fun isNoiseExe(exe: String): Boolean {
        val x = exe.lowercase(Locale.ROOT)
        if (x.startsWith("setup") || x.startsWith("unins") || x.startsWith("uninstall")) return true
        if (x.startsWith("update") || x.startsWith("updater") || x.startsWith("installer")) return true
        return x.contains("uninstall") || x.contains("updater")
    }
}
