package com.Ksinius.musico.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Ksinius.musico.model.Song
import com.Ksinius.musico.model.ThemeSettings
import com.Ksinius.musico.ui.theme.PlayheadColor
import com.Ksinius.musico.ui.theme.PureBlack
import com.Ksinius.musico.ui.theme.dynamicAccentColor
import com.Ksinius.musico.ui.theme.dynamicDarkColor
import com.Ksinius.musico.ui.theme.dynamicPrimaryColor
import com.Ksinius.musico.ui.theme.dynamicSecondaryColor
import com.Ksinius.musico.ui.theme.SongTypography
import com.Ksinius.musico.ui.theme.SurfaceGlass
import com.Ksinius.musico.ui.theme.SurfaceVariantDark
import com.Ksinius.musico.ui.theme.TextMuted
import com.Ksinius.musico.ui.theme.TextPrimary
import com.Ksinius.musico.ui.theme.dynamicAccentColor
import com.Ksinius.musico.ui.theme.dynamicDarkColor
import com.Ksinius.musico.ui.theme.dynamicPrimaryColor
import com.Ksinius.musico.ui.theme.dynamicSecondaryColor
import com.Ksinius.musico.ui.theme.TextSecondary
import com.Ksinius.musico.utils.ArtworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomMusicPlayer(
    currentSong: Song?,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    playbackSpeed: Float = 1.0f,
    playbackPitch: Float = 1.0f,
    shuffleMode: com.Ksinius.musico.player.ShuffleMode = com.Ksinius.musico.player.ShuffleMode.OFF,
    repeatMode: com.Ksinius.musico.player.RepeatMode = com.Ksinius.musico.player.RepeatMode.OFF,
    onPlayPauseToggle: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit = {},
    onToggleRepeat: () -> Unit = {},
    onSkipForward: () -> Unit = {},
    onSkipBackward: () -> Unit = {},
    themeSettings: ThemeSettings = ThemeSettings(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    val artworkBitmap by produceState<Bitmap?>(initialValue = null, currentSong?.filePath) {
        value = withContext(Dispatchers.IO) {
            currentSong?.filePath?.let { ArtworkUtils.getArtwork(context, it) }
        }
    }

    AnimatedVisibility(
        visible = currentSong != null,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier
    ) {
        if (currentSong == null) return@AnimatedVisibility

        // Mini player container with navigationBarsPadding to stay strictly above system home/back buttons
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 20.dp,
                        shape = RoundedCornerShape(24.dp),
                        spotColor = dynamicPrimaryColor(themeSettings)
                    )
                    .clip(RoundedCornerShape(24.dp))
                    .clickable { isExpanded = true },
                color = SurfaceGlass,
                tonalElevation = 0.dp
            ) {
                Column {
                    // Top Progress indicator line
                    val progressFloat = if (duration > 0) (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f
                    LinearProgressIndicator(
                        progress = { progressFloat },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        color = dynamicPrimaryColor(themeSettings),
                        trackColor = dynamicDarkColor(themeSettings).copy(alpha = 0.35f)
                    )

                    // Expanded Height Music Bar Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Song Cover Thumbnail
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(dynamicDarkColor(themeSettings), dynamicPrimaryColor(themeSettings))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (artworkBitmap != null) {
                                Image(
                                    bitmap = artworkBitmap!!.asImageBitmap(),
                                    contentDescription = "Album Cover",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = "Playing Song",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        // Track details (Title & Artist)
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = currentSong.title,
                                style = SongTypography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                ),
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = currentSong.artist,
                                    style = SongTypography.bodySmall.copy(fontSize = 13.sp),
                                    color = dynamicAccentColor(themeSettings),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (playbackSpeed != 1.0f || playbackPitch != 1.0f) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(dynamicPrimaryColor(themeSettings))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = String.format("%.2fx", playbackSpeed),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PureBlack
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Controls (Previous, Prominent Middle Play/Pause, Next)
                        IconButton(
                            onClick = onPrevious,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Previous Track",
                                tint = TextPrimary,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Prominent Middle Play / Pause Button
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .shadow(8.dp, CircleShape, spotColor = dynamicPrimaryColor(themeSettings))
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(dynamicSecondaryColor(themeSettings), dynamicPrimaryColor(themeSettings))
                                    )
                                )
                                .clickable(onClick = onPlayPauseToggle),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause (2 lines)" else "Play (Triangle)",
                                tint = PureBlack,
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        IconButton(
                            onClick = onNext,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next Track",
                                tint = TextPrimary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Expanded Full Screen Music Sheet
    if (isExpanded && currentSong != null) {
        ModalBottomSheet(
            onDismissRequest = { isExpanded = false },
            sheetState = sheetState,
            containerColor = PureBlack,
            scrimColor = PureBlack.copy(alpha = 0.75f),
            modifier = Modifier.fillMaxSize(),
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
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header dismiss bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion { isExpanded = false }
                    }) {
                        Icon(
                            imageVector = Icons.Default.ExpandMore,
                            contentDescription = "Collapse Menu",
                            tint = dynamicAccentColor(themeSettings),
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Text(
                        text = "NOW PLAYING",
                        style = MaterialTheme.typography.labelLarge.copy(
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = dynamicSecondaryColor(themeSettings)
                    )

                    Spacer(modifier = Modifier.width(32.dp))
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Large Glowing Album Card (Real artwork image or default gradient)
                Box(
                    modifier = Modifier
                        .size(270.dp)
                        .shadow(
                            elevation = 36.dp,
                            shape = RoundedCornerShape(32.dp),
                            spotColor = dynamicPrimaryColor(themeSettings)
                        )
                        .clip(RoundedCornerShape(32.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    SurfaceVariantDark,
                                    dynamicDarkColor(themeSettings),
                                    dynamicPrimaryColor(themeSettings)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (artworkBitmap != null) {
                        Image(
                            bitmap = artworkBitmap!!.asImageBitmap(),
                            contentDescription = "Album Cover",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = "Album Art",
                            tint = TextPrimary,
                            modifier = Modifier.size(110.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Song Title & Artist
                Text(
                    text = currentSong.title,
                    style = SongTypography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp
                    ),
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = currentSong.artist,
                    style = SongTypography.titleMedium,
                    color = dynamicAccentColor(themeSettings),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Progress Slider (Scrubber)
                val sliderPosition = if (duration > 0) currentPosition.toFloat() else 0f
                val maxSliderValue = if (duration > 0) duration.toFloat() else 1f

                Slider(
                    value = sliderPosition,
                    onValueChange = { onSeekTo(it.toLong()) },
                    valueRange = 0f..maxSliderValue,
                    colors = SliderDefaults.colors(
                        thumbColor = PlayheadColor,
                        activeTrackColor = dynamicPrimaryColor(themeSettings),
                        inactiveTrackColor = SurfaceVariantDark
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Time labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatDuration(currentPosition),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                    Text(
                        text = formatDuration(duration),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Shuffle & Repeat Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onToggleShuffle,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (shuffleMode == com.Ksinius.musico.player.ShuffleMode.ON) dynamicAccentColor(themeSettings) else TextMuted,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(
                        onClick = onToggleRepeat,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = when (repeatMode) {
                                com.Ksinius.musico.player.RepeatMode.ONE -> Icons.Default.RepeatOne
                                else -> Icons.Default.Repeat
                            },
                            contentDescription = "Repeat",
                            tint = if (repeatMode != com.Ksinius.musico.player.RepeatMode.OFF) dynamicAccentColor(themeSettings) else TextMuted,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Full Controls (Skip Backward, Previous, Big Middle Play/Pause, Next, Skip Forward)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onSkipBackward,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastRewind,
                            contentDescription = "Skip Backward 10s",
                            tint = TextSecondary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    IconButton(
                        onClick = onPrevious,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous Track",
                            tint = TextPrimary,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    // Middle Play / Pause Glowing Button (Play triangle / Pause 2 lines)
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .shadow(20.dp, CircleShape, spotColor = dynamicPrimaryColor(themeSettings))
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(dynamicSecondaryColor(themeSettings), dynamicPrimaryColor(themeSettings))
                                )
                            )
                            .clickable(onClick = onPlayPauseToggle),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause (2 lines)" else "Play (Triangle)",
                            tint = PureBlack,
                            modifier = Modifier.size(46.dp)
                        )
                    }

                    IconButton(
                        onClick = onNext,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next Track",
                            tint = TextPrimary,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    IconButton(
                        onClick = onSkipForward,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastForward,
                            contentDescription = "Skip Forward 10s",
                            tint = TextSecondary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
