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
 * Citește aplicații instalate Windows și le mapează la executabile.
 * Sursa principală: Start Menu shortcuts; fallback/completează din registry.
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
                  "${'$'}env:APPDATA\\Microsoft\\Windows\\Start Menu\\Programs",
                  "${'$'}env:ProgramData\\Microsoft\\Windows\\Start Menu\\Programs"
                )
                ${'$'}regKeys = @(
                  'HKLM:\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\*',
                  'HKLM:\\SOFTWARE\\WOW6432Node\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\*',
                  'HKCU:\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\*'
                )

                function Is-BadExe([string]${'$'}exe) {
                  if (-not ${'$'}exe) { return ${'$'}true }
                  ${'$'}x = ${'$'}exe.ToLowerInvariant()
                  if (${'$'}x -match '^(setup|unins|uninstall|update|updater|installer)') { return ${'$'}true }
                  if (${'$'}x -match '.*(setup|unins|uninstall|update|updater|installer)\\.exe${'$'}') { return ${'$'}true }
                  return ${'$'}false
                }

                ${'$'}rows = New-Object System.Collections.Generic.List[object]
                ${'$'}seen = New-Object 'System.Collections.Generic.HashSet[string]'

                # 1) Start menu shortcuts (installed apps users actually launch)
                try {
                  ${'$'}shell = New-Object -ComObject WScript.Shell
                  foreach (${'$'}dir in ${'$'}startDirs) {
                    if (-not (Test-Path ${'$'}dir)) { continue }
                    ${'$'}lnks = Get-ChildItem -Path ${'$'}dir -Recurse -Filter *.lnk -ErrorAction SilentlyContinue
                    foreach (${'$'}l in ${'$'}lnks) {
                      try {
                        ${'$'}sc = ${'$'}shell.CreateShortcut(${'$'}l.FullName)
                        ${'$'}target = ${'$'}sc.TargetPath
                        if (-not ${'$'}target) { continue }
                        if (-not ${'$'}target.ToLowerInvariant().EndsWith('.exe')) { continue }
                        ${'$'}exe = [System.IO.Path]::GetFileName(${'$'}target).ToLowerInvariant()
                        if (Is-BadExe ${'$'}exe) { continue }
                        ${'$'}name = [System.IO.Path]::GetFileNameWithoutExtension(${'$'}l.Name)
                        if (-not ${'$'}name) { continue }
                        ${'$'}k = "${'$'}name|${'$'}exe"
                        if (${'$'}seen.Add(${'$'}k)) {
                          ${'$'}rows.Add([PSCustomObject]@{ name = ${'$'}name; exe = ${'$'}exe })
                        }
                      } catch { }
                    }
                  }
                } catch { }

                # 2) Registry fallback/completion (for apps without shortcuts)
                foreach (${'$'}k in ${'$'}regKeys) {
                  ${'$'}items = Get-ItemProperty ${'$'}k -ErrorAction SilentlyContinue | Where-Object { ${'$'}_.DisplayName -and ${'$'}_.DisplayIcon }
                  foreach (${'$'}p in ${'$'}items) {
                    try {
                      ${'$'}icon = (${'$'}p.DisplayIcon -replace '^"+|"+${'$'}', '') -replace ',\d+${'$'}',''
                      if (-not ${'$'}icon) { continue }
                      ${'$'}icon = ${'$'}icon -replace '"',''
                      if (-not ${'$'}icon.ToLowerInvariant().EndsWith('.exe')) { continue }
                      ${'$'}exe = [System.IO.Path]::GetFileName(${'$'}icon).ToLowerInvariant()
                      if (Is-BadExe ${'$'}exe) { continue }
                      ${'$'}name = [string]${'$'}p.DisplayName
                      if (-not ${'$'}name) { continue }
                      ${'$'}k2 = "${'$'}name|${'$'}exe"
                      if (${'$'}seen.Add(${'$'}k2)) {
                        ${'$'}rows.Add([PSCustomObject]@{ name = ${'$'}name; exe = ${'$'}exe })
                      }
                    } catch { }
                  }
                }

                ${'$'}out = @(${'$'}rows) | Sort-Object name -Unique
                if (${'$'}out.Count -eq 0) { "[]" } else { ${'$'}out | ConvertTo-Json -Compress -Depth 4 }
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
                val payload = extractJsonPayload(trimmed) ?: return@runCatching emptyList()
                when {
                    payload.startsWith("[") ->
                        json.decodeFromString(ListSerializer(DesktopInstalledAppRow.serializer()), payload)
                    payload.startsWith("{") -> {
                        val one = json.decodeFromString(DesktopInstalledAppRow.serializer(), payload)
                        listOf(one)
                    }
                    else -> emptyList()
                }
            } finally {
                runCatching { f.delete() }
            }
        }
    }

    /**
     * Uneori PowerShell emite warnings/text înainte de JSON.
     * Extrage primul payload JSON detectat din output.
     */
    private fun extractJsonPayload(raw: String): String? {
        val startArr = raw.indexOf('[').takeIf { it >= 0 }
        val startObj = raw.indexOf('{').takeIf { it >= 0 }
        val start = listOfNotNull(startArr, startObj).minOrNull() ?: return null
        return raw.substring(start).trim()
    }
}
