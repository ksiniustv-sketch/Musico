package com.Ksinius.musico.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import androidx.core.app.NotificationCompat
import com.Ksinius.musico.MainActivity
import com.Ksinius.musico.model.Song
import com.Ksinius.musico.utils.ArtworkUtils

class MusicNotificationManager(
    private val context: Context,
    private val onPlayPause: () -> Unit,
    private val onNext: () -> Unit,
    private val onPrevious: () -> Unit
) {
    companion object {
        const val CHANNEL_ID = "musico_playback_channel_v2"
        const val NOTIFICATION_ID = 1001

        const val ACTION_PLAY_PAUSE = "com.Ksinius.musico.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "com.Ksinius.musico.ACTION_NEXT"
        const val ACTION_PREVIOUS = "com.Ksinius.musico.ACTION_PREVIOUS"
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private var mediaSession: MediaSession? = null
    private var defaultArtwork: Bitmap? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(cntx: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_PLAY_PAUSE -> onPlayPause()
                ACTION_NEXT -> onNext()
                ACTION_PREVIOUS -> onPrevious()
            }
        }
    }

    init {
        createNotificationChannel()
        initMediaSession()

        val filter = IntentFilter().apply {
            addAction(ACTION_PLAY_PAUSE)
            addAction(ACTION_NEXT)
            addAction(ACTION_PREVIOUS)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
    }

    private fun initMediaSession() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mediaSession = MediaSession(context, "MusicoMediaSession").apply {
                setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS)
                setCallback(object : MediaSession.Callback() {
                    override fun onPlay() { onPlayPause() }
                    override fun onPause() { onPlayPause() }
                    override fun onSkipToNext() { onNext() }
                    override fun onSkipToPrevious() { onPrevious() }
                })
                isActive = true
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Musico Live Player",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Expanded media controls and lockscreen widget"
                setShowBadge(false)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showNotification(song: Song, isPlaying: Boolean): Notification {
        val albumArt: Bitmap = ArtworkUtils.getArtwork(context, song.filePath)
            ?: getDefaultArtworkBitmap()

        // Update MediaSession metadata & state for Lockscreen controls and Android System Media Player
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mediaSession?.let { session: MediaSession ->
                val pbState = if (isPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED
                val playbackState = PlaybackState.Builder()
                    .setActions(
                        PlaybackState.ACTION_PLAY or
                                PlaybackState.ACTION_PAUSE or
                                PlaybackState.ACTION_SKIP_TO_NEXT or
                                PlaybackState.ACTION_SKIP_TO_PREVIOUS
                    )
                    .setState(pbState, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1.0f)
                    .build()
                session.setPlaybackState(playbackState)

                val metadata = MediaMetadata.Builder()
                    .putString(MediaMetadata.METADATA_KEY_TITLE, song.title)
                    .putString(MediaMetadata.METADATA_KEY_ARTIST, song.artist)
                    .putString(MediaMetadata.METADATA_KEY_ALBUM, song.album)
                    .putLong(MediaMetadata.METADATA_KEY_DURATION, song.duration)
                    .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, albumArt)
                    .putBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON, albumArt)
                    .build()
                session.setMetadata(metadata)
            }
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prevIntent = Intent(ACTION_PREVIOUS).apply { setPackage(context.packageName) }
        val prevPendingIntent = PendingIntent.getBroadcast(
            context, 1, prevIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIntent = Intent(ACTION_PLAY_PAUSE).apply { setPackage(context.packageName) }
        val playPausePendingIntent = PendingIntent.getBroadcast(
            context, 2, playPauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextIntent = Intent(ACTION_NEXT).apply { setPackage(context.packageName) }
        val nextPendingIntent = PendingIntent.getBroadcast(
            context, 3, nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIcon = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        val playPauseTitle = if (isPlaying) "Pause" else "Play"

        val mediaStyle = androidx.media.app.NotificationCompat.MediaStyle()
            .setShowActionsInCompactView(0, 1, 2)

        mediaSession?.sessionToken?.let { token ->
            mediaStyle.setMediaSession(android.support.v4.media.session.MediaSessionCompat.Token.fromToken(token))
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(song.title)
            .setContentText(song.artist)
            .setSubText("Musico • ${song.extension}")
            .setLargeIcon(albumArt)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true) // Always on top while player active
            .setPriority(NotificationCompat.PRIORITY_MAX) // Max priority for auto-extended layout
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setColor(0xFFA855F7.toInt())
            .setColorized(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Visible on lockscreen
            .addAction(android.R.drawable.ic_media_previous, "Previous", prevPendingIntent)
            .addAction(playPauseIcon, playPauseTitle, playPausePendingIntent)
            .addAction(android.R.drawable.ic_media_next, "Next", nextPendingIntent)
            .setStyle(mediaStyle)

        return builder.build()
    }

    private fun getDefaultArtworkBitmap(): Bitmap {
        if (defaultArtwork == null) {
            val bitmap = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint().apply {
                color = Color.parseColor("#A855F7")
                isAntiAlias = true
            }
            canvas.drawCircle(150f, 150f, 150f, paint)
            defaultArtwork = bitmap
        }
        return defaultArtwork!!
    }

    fun notify(notification: Notification) {
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    fun cancelNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
    }

    fun unregister() {
        try {
            context.unregisterReceiver(receiver)
            mediaSession?.release()
            mediaSession = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
        cancelNotification()
    }
}
