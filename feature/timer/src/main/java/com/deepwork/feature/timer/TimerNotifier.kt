package com.deepwork.feature.timer

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.deepwork.core.ui.util.HapticManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimerNotifier @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val manager = NotificationManagerCompat.from(context)
    private val haptics = HapticManager(context)

    fun showSessionStarted(minutes: Int) {
        notify(
            id = 1101,
            title = "Sesiune pornita",
            body = "Deep focus activ pentru $minutes minute."
        )
    }

    fun showSessionPaused() {
        notify(
            id = 1102,
            title = "Sesiune pusa pe pauza",
            body = "Revino in aplicatie pentru a continua focusul."
        )
    }

    fun showSessionCompleted(minutes: Int) {
        playCompletionSound()
        runCatching { haptics.playDoubleBuzz() }
        notify(
            id = 1103,
            title = "Sesiune finalizata",
            body = "Felicitari! Ai completat $minutes minute de focus.",
            soundUri = completionSoundUri()
        )
    }

    private fun completionSoundUri(): Uri =
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

    private fun playCompletionSound() {
        runCatching {
            val uri = completionSoundUri()
            val ringtone = RingtoneManager.getRingtone(context, uri) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ringtone.isLooping = false
            }
            ringtone.play()
            Handler(Looper.getMainLooper()).postDelayed({
                runCatching { ringtone.stop() }
            }, 2800L)
        }
    }

    private fun notify(
        id: Int,
        title: String,
        body: String,
        soundUri: Uri? = null
    ) {
        createChannelIfNeeded()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(android.app.Notification.CATEGORY_EVENT)
            .setAutoCancel(true)

        if (soundUri != null) {
            builder.setSound(soundUri)
        } else {
            builder.setDefaults(NotificationCompat.DEFAULT_SOUND)
        }

        manager.notify(id, builder.build())
    }

    private fun createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Kara Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notificari pentru sesiuni si focus."
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            if (uri != null) {
                setSound(
                    uri,
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            }
            enableVibration(true)
        }
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "deepwork_focus_alerts"
    }
}
