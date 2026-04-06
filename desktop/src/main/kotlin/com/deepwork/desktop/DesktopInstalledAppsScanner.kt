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
 * Citește aplicații instalate din Start Menu shortcuts (.lnk → TargetPath). Doar Windows.
 *
 * Motivație: lista din registry include frecvent uninstallers/setup/update. Shortcut-urile corespund mai bine
 * listei „Aplicații instalate” din UI-ul Windows și dau un .exe concret pe care îl putem bloca.
 */
object DesktopInstalledAppsScanner {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun scan(): Result<List<DesktopInstalledAppRow>> = withContext(Dispatchers.IO) {
        if (!WindowsForegroundExe.isWindowsOs()) {
            return@withContext Result.success(emptyList())
        }
        runCatching {
            val script = """
                ${'$'}startDirs = @(
                  \"${'$'}env:APPDATA\\Microsoft\\Windows\\Start Menu\\Programs\",
                  \"${'$'}env:ProgramData\\Microsoft\\Windows\\Start Menu\\Programs\\\"
                )
                ${'$'}shell = New-Object -ComObject WScript.Shell
                ${'$'}rows = @()
                foreach (${'$'}dir in ${'$'}startDirs) {
                  if (-not (Test-Path ${'$'}dir)) { continue }
                  ${'$'}lnks = Get-ChildItem -Path ${'$'}dir -Recurse -Filter *.lnk -ErrorAction SilentlyContinue
                  foreach (${'$'}l in ${'$'}lnks) {
                    try {
                      ${'$'}sc = ${'$'}shell.CreateShortcut(${'$'}l.FullName)
                      ${'$'}target = ${'$'}sc.TargetPath
                      if (-not ${'$'}target) { continue }
                      if (-not ${'$'}target.ToLower().EndsWith('.exe')) { continue }
                      ${'$'}exe = [System.IO.Path]::GetFileName(${'$'}target).ToLowerInvariant()
                      # filtrează instalatori/updaters/uninstallers
                      if (${'$'}exe -match '^(setup|unins|uninstall|update|updater|installer)') { continue }
                      if (${'$'}exe -match '.*(setup|unins|uninstall|update|updater|installer)\\.exe${'$'}') { continue }
                      ${'$'}name = [System.IO.Path]::GetFileNameWithoutExtension(${'$'}l.Name)
                      if (-not ${'$'}name) { continue }
                      ${'$'}rows += [PSCustomObject]@{ name = ${'$'}name; exe = ${'$'}exe }
                    } catch { }
                  }
                }
                @(${'$'}rows) | Sort-Object name -Unique | ConvertTo-Json -Compress -Depth 4
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
