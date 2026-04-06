package com.deepwork.desktop

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class DesktopInstalledAppRow(
    val name: String,
    /** Numele fișierului .exe, lowercase (ex. chrome.exe). */
    val exe: String
)

/**
 * Citește programe din registry-ul Windows (DisplayIcon → .exe). Doar Windows.
 */
object DesktopInstalledAppsScanner {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun scan(): Result<List<DesktopInstalledAppRow>> = withContext(Dispatchers.IO) {
        if (!WindowsForegroundExe.isWindowsOs()) {
            return@withContext Result.success(emptyList())
        }
        runCatching {
            val script = """
                ${'$'}keys = @(
                  'HKLM:\SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\*',
                  'HKLM:\SOFTWARE\WOW6432Node\Microsoft\Windows\CurrentVersion\Uninstall\*',
                  'HKCU:\SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\*'
                )
                ${'$'}items = foreach (${'$'}k in ${'$'}keys) {
                  Get-ItemProperty ${'$'}k -ErrorAction SilentlyContinue | Where-Object { ${'$'}_.DisplayName -and ${'$'}_.DisplayIcon }
                }
                ${'$'}out = foreach (${'$'}p in ${'$'}items) {
                  ${'$'}icon = (${'$'}p.DisplayIcon -replace '^"+|"+$', '') -replace ',\d+${'$'}',''
                  if (${'$'}icon.ToLower().EndsWith('.exe')) {
                    ${'$'}icon = ${'$'}icon -replace '"',''
                    ${'$'}exe = [System.IO.Path]::GetFileName(${'$'}icon).ToLowerInvariant()
                    [PSCustomObject]@{ name = ${'$'}p.DisplayName; exe = ${'$'}exe }
                  }
                }
                @(${'$'}out) | Sort-Object name -Unique | ConvertTo-Json -Compress -Depth 4
            """.trimIndent()

            val f = File.createTempFile("kara-scan", ".ps1")
            try {
                f.writeText(script, Charsets.UTF_8)
                val proc = ProcessBuilder(
                    "powershell",
                    "-NoProfile",
                    "-NonInteractive",
                    "-ExecutionPolicy", "Bypass",
                    "-File",
                    f.absolutePath
                )
                    .redirectErrorStream(true)
                    .start()
                val out = proc.inputStream.bufferedReader().readText()
                proc.waitFor()
                val trimmed = out.trim()
                if (trimmed.isEmpty() || trimmed == "null") return@runCatching emptyList()
                when {
                    trimmed.startsWith("[") ->
                        json.decodeFromString(ListSerializer(DesktopInstalledAppRow.serializer()), trimmed)
                    trimmed.startsWith("{") -> {
                        val one = json.decodeFromString(DesktopInstalledAppRow.serializer(), trimmed)
                        listOf(one)
                    }
                    else -> emptyList()
                }
            } finally {
                runCatching { f.delete() }
            }
        }
    }
}
