package com.deepwork.desktop

import java.awt.Color
import java.awt.Image
import java.awt.SystemTray
import java.awt.Toolkit
import java.awt.TrayIcon
import java.awt.image.BufferedImage
import javax.swing.JOptionPane
import javax.swing.SwingUtilities

object DesktopSessionAlerts {

    @Volatile
    private var trayIcon: TrayIcon? = null

    fun ensureTray() {
        if (!SystemTray.isSupported()) return
        synchronized(this) {
            if (trayIcon != null) return
            runCatching {
                val tray = SystemTray.getSystemTray()
                val icon = TrayIcon(createTrayImage(), "Kara")
                icon.isImageAutoSize = true
                tray.add(icon)
                trayIcon = icon
            }
        }
    }

    fun notifySessionCompleted(minutes: Int) {
        runCatching { Toolkit.getDefaultToolkit().beep() }
        val icon = trayIcon
        if (icon != null) {
            runCatching {
                icon.displayMessage(
                    "Sesiune finalizată",
                    "Ai completat $minutes minute de focus.",
                    TrayIcon.MessageType.INFO
                )
            }
        } else {
            SwingUtilities.invokeLater {
                JOptionPane.showMessageDialog(
                    null,
                    "Ai completat $minutes minute de focus.",
                    "Sesiune finalizată",
                    JOptionPane.INFORMATION_MESSAGE
                )
            }
        }
    }

    private fun createTrayImage(): Image {
        val img = BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics()
        g.color = Color(0x5C55E8)
        g.fillOval(0, 0, 16, 16)
        g.dispose()
        return img
    }
}
