package com.Ksinius.musico.player

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder

class MusicPlaybackService : Service() {

    private val binder = LocalBinder()
    private var musicPlayerManager: MusicPlayerManager? = null
    private var isForeground = false

    inner class LocalBinder : Binder() {
        fun getService(): MusicPlaybackService = this@MusicPlaybackService
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        // Get singleton instance
        musicPlayerManager = MusicPlayerManager.getInstance(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Return START_STICKY to ensure service restarts if killed
        return START_STICKY
    }

    fun setPlayerManager(playerManager: MusicPlayerManager) {
        musicPlayerManager = playerManager
    }

    fun startForegroundWithNotification(notification: Notification) {
        if (!isForeground) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForeground(MusicNotificationManager.NOTIFICATION_ID, notification)
                isForeground = true
            }
        }
    }

    fun stopForegroundService() {
        if (isForeground) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            }
            isForeground = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForegroundService()
        musicPlayerManager?.release()
    }
}
