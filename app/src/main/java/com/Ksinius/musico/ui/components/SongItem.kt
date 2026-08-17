package com.Ksinius.musico.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Ksinius.musico.model.Song
import com.Ksinius.musico.model.ThemeSettings
import com.Ksinius.musico.ui.theme.CardGlass
import com.Ksinius.musico.ui.theme.PurpleAccent
import com.Ksinius.musico.ui.theme.PurpleDark
import com.Ksinius.musico.ui.theme.PurplePrimary
import com.Ksinius.musico.ui.theme.SongTypography
import com.Ksinius.musico.ui.theme.SurfaceGlass
import com.Ksinius.musico.ui.theme.TextMuted
import com.Ksinius.musico.ui.theme.TextPrimary
import com.Ksinius.musico.ui.theme.TextSecondary
import com.Ksinius.musico.ui.theme.dynamicAccentColor
import com.Ksinius.musico.ui.theme.dynamicDarkColor
import com.Ksinius.musico.ui.theme.dynamicPrimaryColor
import com.Ksinius.musico.utils.ArtworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

@Composable
fun SongItem(
    song: Song,
    isSelected: Boolean,
    isPlaying: Boolean,
    isFavorite: Boolean,
    playbackSpeed: Float = 1.0f,
    onFavoriteToggle: () -> Unit,
    onClick: () -> Unit,
    themeSettings: ThemeSettings = ThemeSettings(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val artworkBitmap by produceState<Bitmap?>(initialValue = null, song.filePath) {
        value = withContext(Dispatchers.IO) {
            ArtworkUtils.getArtwork(context, song.filePath)
        }
    }

    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) CardGlass else SurfaceGlass,
        label = "bg_color"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) dynamicPrimaryColor(themeSettings) else Color.Transparent,
        label = "border_color"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(
                width = if (isSelected) 1.5.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail icon (Embedded Album Cover Art or default icon)
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isSelected) dynamicDarkColor(themeSettings) else Color(0xFF1E172B)
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
            } else if (isSelected && isPlaying) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = "Playing",
                    tint = dynamicAccentColor(themeSettings),
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "Music",
                    tint = if (isSelected) dynamicAccentColor(themeSettings) else dynamicPrimaryColor(themeSettings),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Title and Artist
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = song.title,
                style = SongTypography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                ),
                color = if (isSelected) dynamicAccentColor(themeSettings) else TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                // Audio Format badge (MP3 / WAV)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(dynamicPrimaryColor(themeSettings).copy(alpha = 0.2f))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = song.extension,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = dynamicAccentColor(themeSettings)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = song.artist,
                    style = SongTypography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Duration text
        Text(
            text = formatDuration(song.duration),
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted
        )

        Spacer(modifier = Modifier.width(4.dp))

        // Speed badge (only show if not 1.0x)
        if (playbackSpeed != 1.0f) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(dynamicAccentColor(themeSettings).copy(alpha = 0.2f))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = String.format("%.2fx", playbackSpeed),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = dynamicAccentColor(themeSettings)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))
        }

        // Heart Icon (Empty outline -> Turns white when favorited)
        IconButton(
            onClick = onFavoriteToggle,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = if (isFavorite) "Remove Favorite" else "Add Favorite",
                tint = if (isFavorite) Color.White else TextMuted,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0) return "0:00"
    val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60
    return String.format("%d:%02d", minutes, seconds)
}
