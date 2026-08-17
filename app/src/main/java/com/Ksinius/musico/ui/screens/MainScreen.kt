package com.Ksinius.musico.ui.screens

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.Ksinius.musico.data.AudioScanner
import com.Ksinius.musico.data.PreferencesManager
import com.Ksinius.musico.model.Song
import com.Ksinius.musico.model.ThemeSettings
import com.Ksinius.musico.player.MusicPlaybackService
import com.Ksinius.musico.player.MusicPlayerManager
import com.Ksinius.musico.ui.components.BottomMusicPlayer
import com.Ksinius.musico.ui.components.BubbleBackground
import com.Ksinius.musico.ui.components.FolderItem
import com.Ksinius.musico.ui.components.SongItem
import com.Ksinius.musico.ui.theme.PureBlack
import com.Ksinius.musico.ui.theme.PurpleAccent
import com.Ksinius.musico.ui.theme.PurpleDark
import com.Ksinius.musico.ui.theme.PurplePrimary
import com.Ksinius.musico.ui.theme.PurpleSecondary
import com.Ksinius.musico.ui.theme.SurfaceGlass
import com.Ksinius.musico.ui.theme.SurfaceVariantDark
import com.Ksinius.musico.ui.theme.SurfaceVariantGlass
import com.Ksinius.musico.ui.theme.TextMuted
import com.Ksinius.musico.ui.theme.TextPrimary
import com.Ksinius.musico.ui.theme.TextSecondary
import com.Ksinius.musico.ui.theme.dynamicAccentColor
import com.Ksinius.musico.ui.theme.dynamicDarkColor
import com.Ksinius.musico.ui.theme.dynamicPrimaryColor
import com.Ksinius.musico.ui.theme.dynamicSecondaryColor
import com.Ksinius.musico.utils.FolderUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SortOption(val label: String) {
    TITLE_AZ("A ➔ Z"),
    TITLE_ZA("Z ➔ A"),
    NEWEST("Newest"),
    OLDEST("Oldest"),
    FAVORITES("Favorites"),
    FOLDERS("Folders")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    context: Context = LocalContext.current
) {
    val playerManager = MusicPlayerManager.getInstance(context)
    val prefsManager = remember { PreferencesManager(context) }
    val scope = rememberCoroutineScope()

    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedSort by remember { mutableStateOf(prefsManager.getSortOption()) }
    var selectedFolderPath by remember { mutableStateOf<String?>(null) }
    var favoriteIds by remember { mutableStateOf(prefsManager.getFavoriteSongIds()) }
    var showSlowdownerSheet by remember { mutableStateOf(false) }
    var showThemesSheet by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var themeSettings by remember { mutableStateOf(prefsManager.getThemeSettings()) }
    var isLoading by remember { mutableStateOf(false) }
    var hasPermission by remember { mutableStateOf(checkAudioPermission(context)) }
    var musicService: MusicPlaybackService? by remember { mutableStateOf(null) }
    var isServiceBound by remember { mutableStateOf(false) }

    val currentSong by playerManager.currentSong.collectAsState()
    val isPlaying by playerManager.isPlaying.collectAsState()
    val currentPosition by playerManager.currentPosition.collectAsState()
    val duration by playerManager.duration.collectAsState()

    val playbackSpeed by playerManager.playbackSpeed.collectAsState()
    val playbackPitch by playerManager.playbackPitch.collectAsState()
    val isVinylMode by playerManager.isVinylMode.collectAsState()
    val shuffleMode by playerManager.shuffleMode.collectAsState()
    val repeatMode by playerManager.repeatMode.collectAsState()

    val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.POST_NOTIFICATIONS
        )
    } else {
        arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    }

    val foregroundServicePermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        arrayOf(
            Manifest.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK
        )
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        emptyArray()
    }

    val serviceConnection = remember {
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                val binder = service as MusicPlaybackService.LocalBinder
                musicService = binder.getService()
                playerManager.setMusicService(musicService!!)
                isServiceBound = true
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                musicService = null
                isServiceBound = false
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        val audioGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsMap[Manifest.permission.READ_MEDIA_AUDIO] == true
        } else {
            permissionsMap[Manifest.permission.READ_EXTERNAL_STORAGE] == true
        }

        hasPermission = audioGranted

        if (audioGranted) {
            scope.launch {
                isLoading = true
                val scanned = withContext(Dispatchers.IO) {
                    AudioScanner.scanAudioFiles(context)
                }
                songs = scanned
                playerManager.setPlaylist(scanned)
                isLoading = false
            }
        }
    }

    // Trigger initial scan & check POST_NOTIFICATIONS permission
    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val notifGranted = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!notifGranted) {
                    permissionLauncher.launch(permissionsToRequest)
                }
            }

            // Request foreground service permission for Android 14+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val fgServiceGranted = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK
                ) == PackageManager.PERMISSION_GRANTED
                if (!fgServiceGranted && foregroundServicePermissions.isNotEmpty()) {
                    permissionLauncher.launch(foregroundServicePermissions)
                }
            }

            if (songs.isEmpty()) {
                isLoading = true
                val scanned = withContext(Dispatchers.IO) {
                    AudioScanner.scanAudioFiles(context)
                }
                songs = scanned
                playerManager.setPlaylist(scanned)
                isLoading = false
            }

            // Start and bind to music service for background playback
            val serviceIntent = Intent(context, MusicPlaybackService::class.java)
            context.startForegroundService(serviceIntent)
            context.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            // Don't release player or unbind service - let it continue in background
            // Only release when user explicitly stops music
        }
    }

    LaunchedEffect(selectedSort) {
        if (selectedSort != SortOption.FOLDERS) {
            selectedFolderPath = null
        }
    }

    val searchFilteredSongs = remember(songs, searchQuery) {
        if (searchQuery.isBlank()) {
            songs
        } else {
            val q = searchQuery.trim().lowercase()
            songs.filter {
                it.title.lowercase().contains(q) ||
                        it.artist.lowercase().contains(q) ||
                        it.album.lowercase().contains(q) ||
                        it.filePath.lowercase().contains(q)
            }
        }
    }

    val musicFolders = remember(searchFilteredSongs, searchQuery) {
        val folders = FolderUtils.groupSongsIntoFolders(searchFilteredSongs)
        if (searchQuery.isBlank()) {
            folders
        } else {
            val q = searchQuery.trim().lowercase()
            folders.filter {
                it.name.lowercase().contains(q) || it.path.lowercase().contains(q)
            }
        }
    }

    // Filtered and Sorted song list
    val filteredAndSortedSongs = remember(
        searchFilteredSongs,
        selectedSort,
        favoriteIds,
        selectedFolderPath
    ) {
        var result = searchFilteredSongs

        if (selectedSort == SortOption.FAVORITES) {
            result = result.filter { favoriteIds.contains(it.id.toString()) }
        }

        if (selectedSort == SortOption.FOLDERS && selectedFolderPath != null) {
            result = FolderUtils.songsInFolder(result, selectedFolderPath!!)
        }

        when (selectedSort) {
            SortOption.TITLE_AZ -> result.sortedBy { it.title.lowercase() }
            SortOption.TITLE_ZA -> result.sortedByDescending { it.title.lowercase() }
            SortOption.NEWEST -> result.sortedByDescending { it.dateAdded }
            SortOption.OLDEST -> result.sortedBy { it.dateAdded }
            SortOption.FAVORITES -> result.sortedBy { it.title.lowercase() }
            SortOption.FOLDERS -> result.sortedBy { it.title.lowercase() }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Bubble background as lowest layer
        BubbleBackground(themeSettings = themeSettings)
        
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            bottomBar = {
                BottomMusicPlayer(
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                    currentPosition = currentPosition,
                    duration = duration,
                    playbackSpeed = playbackSpeed,
                    playbackPitch = playbackPitch,
                    shuffleMode = shuffleMode,
                    repeatMode = repeatMode,
                    onPlayPauseToggle = { playerManager.togglePlayPause() },
                    onNext = { playerManager.playNext() },
                    onPrevious = { playerManager.playPrevious() },
                    onSeekTo = { playerManager.seekTo(it) },
                    onToggleShuffle = { playerManager.toggleShuffle() },
                    onToggleRepeat = { playerManager.toggleRepeat() },
                    onSkipForward = { playerManager.seekTo(currentPosition + 10000) },
                    onSkipBackward = { playerManager.seekTo(currentPosition - 10000) },
                    themeSettings = themeSettings
                )
            }
        ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = innerPadding.calculateTopPadding(),
                    bottom = 0.dp
                )
                .padding(horizontal = 16.dp)
        ) {

            // App Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(dynamicDarkColor(themeSettings)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = "App Icon",
                            tint = dynamicAccentColor(themeSettings),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Musico",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 24.sp
                            ),
                            color = TextPrimary
                        )
                        Text(
                            text = when {
                                hasPermission == false -> "Storage Scanner"
                                selectedSort == SortOption.FOLDERS && selectedFolderPath == null ->
                                    "${musicFolders.size} folders"
                                selectedSort == SortOption.FOLDERS && selectedFolderPath != null ->
                                    "${filteredAndSortedSongs.size} tracks"
                                else -> "${filteredAndSortedSongs.size} tracks"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = dynamicAccentColor(themeSettings)
                        )
                        if (hasPermission) {
                            when {
                                selectedSort == SortOption.FOLDERS && selectedFolderPath == null ->
                                    Text(
                                        text = "${songs.size} tracks",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = dynamicAccentColor(themeSettings)
                                    )
                                selectedSort == SortOption.FOLDERS && selectedFolderPath != null ->
                                    Text(
                                        text = "in ${FolderUtils.folderName(selectedFolderPath!!)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = dynamicAccentColor(themeSettings)
                                    )
                                else ->
                                    Text(
                                        text = "${favoriteIds.size} favorites",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = dynamicAccentColor(themeSettings)
                                    )
                            }
                        }
                    }
                }

                if (hasPermission) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Song Speed Button
                        IconButton(
                            onClick = { showSlowdownerSheet = true },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (playbackSpeed != 1.0f || playbackPitch != 1.0f) dynamicPrimaryColor(themeSettings) else SurfaceGlass)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = "Song Speed",
                                tint = if (playbackSpeed != 1.0f || playbackPitch != 1.0f) PureBlack else dynamicPrimaryColor(themeSettings)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Themes Button
                        IconButton(
                            onClick = { showThemesSheet = true },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(SurfaceGlass)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Brush,
                                contentDescription = "Themes",
                                tint = dynamicPrimaryColor(themeSettings)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                scope.launch {
                                    isLoading = true
                                    val scanned = withContext(Dispatchers.IO) {
                                        AudioScanner.scanAudioFiles(context)
                                    }
                                    songs = scanned
                                    playerManager.setPlaylist(scanned)
                                    isLoading = false
                                }
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(SurfaceGlass)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Rescan Storage",
                                tint = dynamicPrimaryColor(themeSettings)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = { showInfoDialog = true },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(SurfaceGlass)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "App Info",
                                tint = dynamicPrimaryColor(themeSettings)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (hasPermission == false) {
                // Permission Request Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(SurfaceGlass)
                            .padding(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderSpecial,
                            contentDescription = "Permission Needed",
                            tint = dynamicSecondaryColor(themeSettings),
                            modifier = Modifier.size(64.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Audio & Notification Permission",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Musico needs access to scan device storage for MP3 & WAV audio files and show media playback controls in your notification bar.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { permissionLauncher.launch(permissionsToRequest) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = dynamicPrimaryColor(themeSettings),
                                contentColor = PureBlack
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(0.8f)
                        ) {
                            Text(
                                text = "Grant Access & Scan",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            } else {
                // Search Input Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(text = "Search MP3 & WAV tracks...", color = TextMuted)
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Icon",
                            tint = dynamicPrimaryColor(themeSettings)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear Search",
                                    tint = TextSecondary
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceGlass,
                        unfocusedContainerColor = SurfaceGlass,
                        disabledContainerColor = SurfaceGlass,
                        focusedBorderColor = dynamicPrimaryColor(themeSettings),
                        unfocusedBorderColor = SurfaceVariantDark,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Sort & Favorites Filter Chips Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (selectedSort == SortOption.FOLDERS) {
                            Icons.Default.Folder
                        } else {
                            Icons.Default.SortByAlpha
                        },
                        contentDescription = "Sort Options",
                        tint = dynamicSecondaryColor(themeSettings),
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(SortOption.values()) { sortOption ->
                            val isSelected = selectedSort == sortOption
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) dynamicPrimaryColor(themeSettings) else SurfaceGlass
                                    )
                                    .border(
                                        width = if (isSelected) 0.dp else 1.dp,
                                        color = SurfaceVariantGlass,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        selectedSort = sortOption
                                        if (sortOption == SortOption.FOLDERS) {
                                            selectedFolderPath = null
                                        }
                                        prefsManager.saveSortOption(sortOption)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = sortOption.label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) PureBlack else TextSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (selectedSort == SortOption.FOLDERS && selectedFolderPath != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceGlass)
                            .border(1.dp, dynamicPrimaryColor(themeSettings).copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .clickable { selectedFolderPath = null }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back to folders",
                            tint = dynamicPrimaryColor(themeSettings),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "All Folders",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = FolderUtils.folderName(selectedFolderPath!!),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = dynamicAccentColor(themeSettings),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Track List / Folder List / Loading / Empty view
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = dynamicPrimaryColor(themeSettings))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Scanning storage for MP3 & WAV files...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = dynamicAccentColor(themeSettings)
                                )
                            }
                        }
                    }

                    selectedSort == SortOption.FOLDERS && selectedFolderPath == null && musicFolders.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = "No Folders",
                                    tint = TextMuted,
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = when {
                                        searchQuery.isNotEmpty() -> "No folders matching '$searchQuery'"
                                        else -> "No folders found"
                                    },
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    selectedSort == SortOption.FOLDERS && selectedFolderPath == null -> {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(top = 4.dp, bottom = 200.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = musicFolders,
                                key = { it.path }
                            ) { folder ->
                                FolderItem(
                                    folder = folder,
                                    onClick = { selectedFolderPath = folder.path },
                                    themeSettings = themeSettings
                                )
                            }
                        }
                    }

                    filteredAndSortedSongs.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.MusicOff,
                                    contentDescription = "No Music",
                                    tint = TextMuted,
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = when {
                                        selectedSort == SortOption.FAVORITES && searchQuery.isNotEmpty() -> "No songs matching '$searchQuery' in favorites"
                                        selectedSort == SortOption.FAVORITES -> "No favorite songs added yet!\nTap the heart icon on any song to add it here."
                                        selectedSort == SortOption.FOLDERS && selectedFolderPath != null -> "No tracks in this folder"
                                        searchQuery.isNotEmpty() -> "No tracks matching '$searchQuery'"
                                        else -> "No MP3 or WAV audio files found"
                                    },
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(top = 4.dp, bottom = 200.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = filteredAndSortedSongs,
                                key = { it.id }
                            ) { song ->
                                val isSelected = currentSong?.id == song.id
                                val isFav = favoriteIds.contains(song.id.toString())
                                val songSpeed = prefsManager.getSongSpeed(song.id)
                                SongItem(
                                    song = song,
                                    isSelected = isSelected,
                                    isPlaying = isSelected && isPlaying,
                                    isFavorite = isFav,
                                    playbackSpeed = songSpeed,
                                    onFavoriteToggle = {
                                        prefsManager.toggleFavorite(song.id)
                                        favoriteIds = prefsManager.getFavoriteSongIds()
                                    },
                                    onClick = {
                                        playerManager.playSong(song, filteredAndSortedSongs)
                                    },
                                    themeSettings = themeSettings
                                )
                            }
                        }
                    }
                }
            }
        }

        // Info Dialog
        if (showInfoDialog) {
            AlertDialog(
                onDismissRequest = { showInfoDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = "Musico Icon",
                            tint = dynamicPrimaryColor(themeSettings),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "MUSICO",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = dynamicPrimaryColor(themeSettings)
                        )
                    }
                },
                text = {
                    Column {
                        Text(
                            text = "Open source music app",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Made by - ksinius",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Features:",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "• RGB Theme Customization\n• High-Quality Audio Slowdown\n• Skip 10s Forward/Backward\n• Speed & Pitch Control\n• Folder Organization\n• Search & Sort\n• Favorites System",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showInfoDialog = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = dynamicPrimaryColor(themeSettings),
                            contentColor = PureBlack
                        )
                    ) {
                        Text("OK")
                    }
                }
            )
        }
    }

    // Slide-in Music Slowdowner & Pitch Modal Drawer Sheet
    if (showSlowdownerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSlowdownerSheet = false },
            containerColor = PureBlack,
            scrimColor = PureBlack.copy(alpha = 0.75f),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .width(44.dp)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(dynamicPrimaryColor(themeSettings).copy(alpha = 0.5f))
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Slowdowner Icon",
                            tint = dynamicPrimaryColor(themeSettings),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "MUSIC SLOWDOWNER & PITCHER",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            ),
                            color = TextPrimary
                        )
                    }

                    if (playbackSpeed != 1.0f || playbackPitch != 1.0f) {
                        Button(
                            onClick = {
                                playerManager.setPlaybackSpeed(1.0f)
                                playerManager.setPlaybackPitch(1.0f)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SurfaceGlass,
                                contentColor = dynamicAccentColor(themeSettings)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset",
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Reset 1.0x", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Vinyl Tape Mode Toggle Card (100% Crystal Clear Audio Quality)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceGlass)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    color = SurfaceGlass
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "easy Mode",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                                color = TextPrimary
                            )
                            Text(
                                text = "Disable if you want to have advanced control of speed & pitch",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = dynamicAccentColor(themeSettings)
                            )
                        }

                        Switch(
                            checked = isVinylMode,
                            onCheckedChange = { playerManager.setVinylMode(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PureBlack,
                                checkedTrackColor = dynamicPrimaryColor(themeSettings),
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = SurfaceVariantDark
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Slowdowner Sliders Container Card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(SurfaceGlass)
                        .border(1.dp, dynamicDarkColor(themeSettings).copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                        .padding(18.dp),
                    color = SurfaceGlass
                ) {
                    Column {
                        // BPM / Speed Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "BPM / Speed (Slowdowner)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(
                                text = String.format("%.2fx", playbackSpeed),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = dynamicAccentColor(themeSettings)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Slider(
                            value = playbackSpeed,
                            onValueChange = { playerManager.setPlaybackSpeed(it) },
                            valueRange = 0.5f..1.5f,
                            colors = SliderDefaults.colors(
                                thumbColor = dynamicSecondaryColor(themeSettings),
                                activeTrackColor = dynamicPrimaryColor(themeSettings),
                                inactiveTrackColor = SurfaceVariantDark
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "0.50x (Slowed)", fontSize = 10.sp, color = TextMuted)
                            Text(text = "1.00x (Normal)", fontSize = 10.sp, color = TextMuted)
                            Text(text = "1.50x (Fast)", fontSize = 10.sp, color = TextMuted)
                        }

                        if (!isVinylMode) {
                            Spacer(modifier = Modifier.height(20.dp))

                            // Pitch Adjuster Slider (Only when Vinyl Mode is OFF)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Pitch Adjuster", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text(
                                    text = String.format("%.2fx", playbackPitch),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = dynamicAccentColor(themeSettings)
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Slider(
                                value = playbackPitch,
                                onValueChange = { playerManager.setPlaybackPitch(it) },
                                valueRange = 0.5f..1.5f,
                                colors = SliderDefaults.colors(
                                    thumbColor = dynamicSecondaryColor(themeSettings),
                                    activeTrackColor = dynamicPrimaryColor(themeSettings),
                                    inactiveTrackColor = SurfaceVariantDark
                                )
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "0.50x (Deep Pitch)", fontSize = 10.sp, color = TextMuted)
                                Text(text = "1.00x (Normal)", fontSize = 10.sp, color = TextMuted)
                                Text(text = "1.50x (High Pitch)", fontSize = 10.sp, color = TextMuted)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }

    // Themes Modal Bottom Sheet
    if (showThemesSheet) {
        ModalBottomSheet(
            onDismissRequest = { showThemesSheet = false },
            containerColor = PureBlack,
            scrimColor = PureBlack.copy(alpha = 0.75f),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .width(44.dp)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(dynamicPrimaryColor(themeSettings).copy(alpha = 0.5f))
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Brush,
                            contentDescription = "Themes Icon",
                            tint = dynamicPrimaryColor(themeSettings),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "THEME CUSTOMIZER",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            ),
                            color = TextPrimary
                        )
                    }

                    Button(
                        onClick = {
                            themeSettings = ThemeSettings()
                            prefsManager.saveThemeSettings(themeSettings)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SurfaceGlass,
                            contentColor = dynamicAccentColor(themeSettings)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Reset", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // RGB Sliders Container Card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(SurfaceGlass)
                        .border(1.dp, dynamicDarkColor(themeSettings).copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                        .padding(18.dp),
                    color = SurfaceGlass
                ) {
                    Column {
                        // Red Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Red", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                            Text(
                                text = String.format("%.2f", themeSettings.red),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.Red
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Slider(
                            value = themeSettings.red,
                            onValueChange = {
                                themeSettings = themeSettings.copy(red = it)
                                prefsManager.saveThemeSettings(themeSettings)
                            },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color.Red,
                                activeTrackColor = Color.Red,
                                inactiveTrackColor = SurfaceVariantDark
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Green Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Green", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Green)
                            Text(
                                text = String.format("%.2f", themeSettings.green),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.Green
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Slider(
                            value = themeSettings.green,
                            onValueChange = {
                                themeSettings = themeSettings.copy(green = it)
                                prefsManager.saveThemeSettings(themeSettings)
                            },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color.Green,
                                activeTrackColor = Color.Green,
                                inactiveTrackColor = SurfaceVariantDark
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Blue Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Blue", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Blue)
                            Text(
                                text = String.format("%.2f", themeSettings.blue),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.Blue
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Slider(
                            value = themeSettings.blue,
                            onValueChange = {
                                themeSettings = themeSettings.copy(blue = it)
                                prefsManager.saveThemeSettings(themeSettings)
                            },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color.Blue,
                                activeTrackColor = Color.Blue,
                                inactiveTrackColor = SurfaceVariantDark
                            )
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Color Preview
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(themeSettings.toColor())
                            )
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(themeSettings.toDarkerColor())
                            )
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(themeSettings.toLighterColor())
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Text(text = "Primary", fontSize = 10.sp, color = TextMuted)
                            Text(text = "Dark", fontSize = 10.sp, color = TextMuted)
                            Text(text = "Light", fontSize = 10.sp, color = TextMuted)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
    }
}

private fun checkAudioPermission(context: Context): Boolean {
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
