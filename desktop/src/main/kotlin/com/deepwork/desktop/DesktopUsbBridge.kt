package com.deepwork.desktop

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

object DesktopUsbBridge {
    private const val PORT = 8080

    suspend fun ensureAdbReverse(): String = withContext(Dispatchers.IO) {
        val adbCmd = detectAdbCommand() ?: return@withContext "USB bridge: adb not found"
        val serials = listDeviceSerials(adbCmd)
        if (serials.isEmpty()) return@withContext "USB bridge: no authorized USB device"

        var ok = 0
        serials.forEach { serial ->
            if (enableReverse(adbCmd, serial)) ok++
        }
        when {
            ok == serials.size -> "USB bridge ready (${ok} device${if (ok == 1) "" else "s"})"
            ok > 0 -> "USB bridge partial ($ok/${serials.size})"
            else -> "USB bridge failed (check USB debugging)"
        }
    }

    private fun detectAdbCommand(): List<String>? {
        val local = System.getenv("LOCALAPPDATA")
        if (!local.isNullOrBlank()) {
            val path = "$local\\Android\\Sdk\\platform-tools\\adb.exe"
            if (java.io.File(path).exists()) return listOf(path)
        }
        val androidHome = System.getenv("ANDROID_HOME")
        if (!androidHome.isNullOrBlank()) {
            val path = "$androidHome\\platform-tools\\adb.exe"
            if (java.io.File(path).exists()) return listOf(path)
        }
        return listOf("adb")
    }

    private fun listDeviceSerials(adbCmd: List<String>): List<String> {
        val out = exec(adbCmd + "devices")
        if (out.exitCode != 0) return emptyList()
        return out.stdout
            .lineSequence()
            .drop(1)
            .map { it.trim() }
            .filter { it.isNotBlank() && "\tdevice" in it }
            .map { it.substringBefore('\t') }
            .toList()
    }

    private fun enableReverse(adbCmd: List<String>, serial: String): Boolean {
        val cmd = adbCmd + listOf("-s", serial, "reverse", "tcp:$PORT", "tcp:$PORT")
        return exec(cmd).exitCode == 0
    }

    private fun exec(cmd: List<String>): ExecResult {
        return try {
            val p = ProcessBuilder(cmd).redirectErrorStream(true).start()
            val done = p.waitFor(4, TimeUnit.SECONDS)
            if (!done) {
                p.destroyForcibly()
                return ExecResult(-1, "")
            }
            val text = p.inputStream.bufferedReader().use { it.readText() }
            ExecResult(p.exitValue(), text)
        } catch (_: Exception) {
            ExecResult(-1, "")
        }
    }

    private data class ExecResult(val exitCode: Int, val stdout: String)
}
