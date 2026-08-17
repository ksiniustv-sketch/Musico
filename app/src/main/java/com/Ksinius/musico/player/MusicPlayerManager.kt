package com.Ksinius.musico.player

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import com.Ksinius.musico.data.PreferencesManager
import com.Ksinius.musico.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ShuffleMode {
    OFF,
    ON
}

enum class RepeatMode {
    OFF,
    ALL,
    ONE
}

class MusicPlayerManager private constructor(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressJob: Job? = null
    private val prefsManager = PreferencesManager(context)
    private var musicService: MusicPlaybackService? = null

    companion object {
        @Volatile
        private var instance: MusicPlayerManager? = null

        fun getInstance(context: Context): MusicPlayerManager {
            return instance ?: synchronized(this) {
                instance ?: MusicPlayerManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val notificationManager = MusicNotificationManager(
        context = context,
        onPlayPause = { togglePlayPause() },
        onNext = { playNext() },
        onPrevious = { playPrevious() }
    )

    private val _playlist = mutableListOf<Song>()
    val playlist: List<Song> get() = _playlist

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _playbackPitch = MutableStateFlow(1.0f)
    val playbackPitch: StateFlow<Float> = _playbackPitch.asStateFlow()

    private val _isVinylMode = MutableStateFlow(true) // Default to crystal clear Vinyl Tape mode
    val isVinylMode: StateFlow<Boolean> = _isVinylMode.asStateFlow()

    private val _shuffleMode = MutableStateFlow(ShuffleMode.OFF)
    val shuffleMode: StateFlow<ShuffleMode> = _shuffleMode.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private var shuffledPlaylist = mutableListOf<Song>()
    private var originalPlaylist = mutableListOf<Song>()

    fun setPlaylist(songs: List<Song>) {
        _playlist.clear()
        _playlist.addAll(songs)
        originalPlaylist.clear()
        originalPlaylist.addAll(songs)
        if (_shuffleMode.value == ShuffleMode.ON) {
            createShuffledPlaylist()
        }
    }

    fun setMusicService(service: MusicPlaybackService) {
        musicService = service
    }

    fun toggleShuffle() {
        _shuffleMode.value = if (_shuffleMode.value == ShuffleMode.OFF) {
            ShuffleMode.ON
        } else {
            ShuffleMode.OFF
        }

        if (_shuffleMode.value == ShuffleMode.ON) {
            createShuffledPlaylist()
            _playlist.clear()
            _playlist.addAll(shuffledPlaylist)
        } else {
            _playlist.clear()
            _playlist.addAll(originalPlaylist)
        }
    }

    fun toggleRepeat() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
    }

    private fun createShuffledPlaylist() {
        shuffledPlaylist.clear()
        shuffledPlaylist.addAll(originalPlaylist)
        shuffledPlaylist.shuffle()
    }

    fun playSong(song: Song, newPlaylist: List<Song> = emptyList()) {
        if (newPlaylist.isNotEmpty()) {
            _playlist.clear()
            _playlist.addAll(newPlaylist)
        }

        if (_currentSong.value?.id == song.id && mediaPlayer != null) {
            if (!mediaPlayer!!.isPlaying) {
                mediaPlayer!!.start()
                _isPlaying.value = true
                applyParams()
                startProgressTracker()
                updateNotification()
            }
            return
        }

        stopAndReset()
        _currentSong.value = song

        // Load saved speed for this song
        val savedSpeed = prefsManager.getSongSpeed(song.id)
        _playbackSpeed.value = savedSpeed
        if (_isVinylMode.value) {
            _playbackPitch.value = savedSpeed
        }

        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(context, song.contentUri)
                prepareAsync()
                setOnPreparedListener { mp ->
                    applyParamsToPlayer(mp)
                    mp.start()
                    _isPlaying.value = true
                    _duration.value = mp.duration.toLong()
                    startProgressTracker()
                    updateNotification()

                    // Start foreground service for background playback
                    val notification = notificationManager.showNotification(song, true)
                    musicService?.startForegroundWithNotification(notification)
                }
                setOnCompletionListener {
                    _isPlaying.value = false
                    stopProgressTracker()
                    updateNotification()
                    playNext()
                }
                setOnErrorListener { _, _, _ ->
                    _isPlaying.value = false
                    stopProgressTracker()
                    updateNotification()
                    true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _isPlaying.value = false
        }
    }

    fun togglePlayPause() {
        val player = mediaPlayer ?: return
        if (player.isPlaying) {
            player.pause()
            _isPlaying.value = false
            stopProgressTracker()
            // Stop foreground service when paused
            musicService?.stopForegroundService()
        } else {
            player.start()
            _isPlaying.value = true
            applyParams()
            startProgressTracker()
            // Start foreground service when playing
            _currentSong.value?.let { song ->
                val notification = notificationManager.showNotification(song, true)
                musicService?.startForegroundWithNotification(notification)
            }
        }
        updateNotification()
    }

    fun setVinylMode(enabled: Boolean) {
        _isVinylMode.value = enabled
        if (enabled) {
            _playbackPitch.value = _playbackSpeed.value
        }
        applyParams()
    }

    fun setPlaybackSpeed(speed: Float) {
        val validSpeed = speed.coerceIn(0.25f, 2.5f)
        _playbackSpeed.value = validSpeed
        if (_isVinylMode.value) {
            _playbackPitch.value = validSpeed
        }
        applyParams()

        // Save speed for current song
        _currentSong.value?.let { song ->
            prefsManager.saveSongSpeed(song.id, validSpeed)
        }
    }

    fun setPlaybackPitch(pitch: Float) {
        val validPitch = pitch.coerceIn(0.5f, 2.0f)
        _playbackPitch.value = validPitch
        applyParams()
    }

    private fun applyParams() {
        mediaPlayer?.let { applyParamsToPlayer(it) }
    }

    private fun applyParamsToPlayer(player: MediaPlayer) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val params = player.playbackParams ?: PlaybackParams()
                val speed = _playbackSpeed.value
                val pitch = if (_isVinylMode.value) speed else _playbackPitch.value

                params.speed = speed
                params.pitch = pitch
                params.audioFallbackMode = PlaybackParams.AUDIO_FALLBACK_MODE_DEFAULT
                player.playbackParams = params
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun playNext() {
        val songs = _playlist
        if (songs.isEmpty()) return

        when (_repeatMode.value) {
            RepeatMode.ONE -> {
                // Repeat current song
                _currentSong.value?.let { playSong(it) }
            }
            else -> {
                val currentIndex = songs.indexOfFirst { it.id == _currentSong.value?.id }
                val nextIndex = if (currentIndex != -1 && currentIndex < songs.size - 1) {
                    currentIndex + 1
                } else {
                    if (_repeatMode.value == RepeatMode.ALL) {
                        0 // Loop back to start
                    } else {
                        return // Stop at end
                    }
                }
                playSong(songs[nextIndex])
            }
        }
    }

    fun playPrevious() {
        val songs = _playlist
        if (songs.isEmpty()) return

        when (_repeatMode.value) {
            RepeatMode.ONE -> {
                // Repeat current song
                _currentSong.value?.let { playSong(it) }
            }
            else -> {
                val currentIndex = songs.indexOfFirst { it.id == _currentSong.value?.id }
                val prevIndex = if (currentIndex > 0) {
                    currentIndex - 1
                } else {
                    if (_repeatMode.value == RepeatMode.ALL) {
                        songs.size - 1 // Loop to end
                    } else {
                        return // Stop at beginning
                    }
                }
                playSong(songs[prevIndex])
            }
        }
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.let { player ->
            try {
                player.seekTo(positionMs.toInt())
                _currentPosition.value = positionMs
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun updateNotification() {
        _currentSong.value?.let { song ->
            val notification = notificationManager.showNotification(song, _isPlaying.value)
            notificationManager.notify(notification)
        }
    }

    fun startForegroundService() {
        _currentSong.value?.let { song ->
            val notification = notificationManager.showNotification(song, _isPlaying.value)
            // This will be called from the service
        }
    }

    fun stopForegroundService() {
        notificationManager.cancelNotification()
    }

    private fun startProgressTracker() {
        stopProgressTracker()
        progressJob = scope.launch {
            while (true) {
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        _currentPosition.value = player.currentPosition.toLong()
                        _duration.value = player.duration.toLong()
                    }
                }
                delay(300)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun stopAndReset() {
        stopProgressTracker()
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }
        mediaPlayer = null
        _isPlaying.value = false
        _currentPosition.value = 0L
    }

    fun release() {
        stopAndReset()
        notificationManager.unregister()
        musicService?.stopForegroundService()
    }
}
