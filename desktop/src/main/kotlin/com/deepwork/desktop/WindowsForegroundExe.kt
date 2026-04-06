package com.deepwork.desktop

import com.sun.jna.Native
import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinNT
import com.sun.jna.ptr.IntByReference
import java.nio.file.Paths
import java.util.Locale

object WindowsForegroundExe {

    private val isWindows: Boolean =
        System.getProperty("os.name")?.contains("windows", ignoreCase = true) == true

    private val ourExeLower: String? by lazy {
        runCatching {
            ProcessHandle.current().info().command().orElse(null)
                ?.let { Paths.get(it).fileName.toString().lowercase(Locale.ROOT) }
        }.getOrNull()
    }

    fun isWindowsOs(): Boolean = isWindows

    /** .exe curent al procesului Kara / JVM. */
    fun ourProcessExeLowercase(): String? = ourExeLower

    /**
     * Numele fișierului .exe al ferestrei active (foreground), lowercase.
     * Non-Windows: întotdeauna null.
     */
    fun foregroundExeLowercase(): String? {
        if (!isWindows) return null
        return runCatching {
            val user32 = User32.INSTANCE
            val kernel32 = Kernel32.INSTANCE
            val hwnd = user32.GetForegroundWindow() ?: return null
            val pidRef = IntByReference()
            user32.GetWindowThreadProcessId(hwnd, pidRef)
            val handle = kernel32.OpenProcess(WinNT.PROCESS_QUERY_LIMITED_INFORMATION, false, pidRef.value)
                ?: return null
            try {
                val buf = CharArray(4096)
                val size = IntByReference(buf.size)
                if (!kernel32.QueryFullProcessImageName(handle, 0, buf, size)) return null
                val path = Native.toString(buf).trim { it <= '\u0000' }
                if (path.isEmpty()) return null
                Paths.get(path).fileName.toString().lowercase(Locale.ROOT)
            } finally {
                kernel32.CloseHandle(handle)
            }
        }.getOrNull()
    }
}
