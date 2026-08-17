package com.Ksinius.musico.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Ksinius.musico.model.MusicFolder
import com.Ksinius.musico.model.ThemeSettings
import com.Ksinius.musico.ui.theme.PurpleAccent
import com.Ksinius.musico.ui.theme.PurpleDark
import com.Ksinius.musico.ui.theme.PurplePrimary
import com.Ksinius.musico.ui.theme.PurpleSecondary
import com.Ksinius.musico.ui.theme.SurfaceGlass
import com.Ksinius.musico.ui.theme.TextMuted
import com.Ksinius.musico.ui.theme.TextPrimary
import com.Ksinius.musico.ui.theme.TextSecondary
import com.Ksinius.musico.ui.theme.dynamicAccentColor
import com.Ksinius.musico.ui.theme.dynamicDarkColor
import com.Ksinius.musico.ui.theme.dynamicPrimaryColor
import com.Ksinius.musico.ui.theme.dynamicSecondaryColor

@Composable
fun FolderItem(
    folder: MusicFolder,
    onClick: () -> Unit,
    themeSettings: ThemeSettings = ThemeSettings(),
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceGlass)
            .border(1.dp, dynamicPrimaryColor(themeSettings).copy(alpha = 0.45f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(dynamicDarkColor(themeSettings), dynamicPrimaryColor(themeSettings))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = "Folder",
                tint = dynamicAccentColor(themeSettings),
                modifier = Modifier.size(26.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = folder.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                ),
                color = dynamicSecondaryColor(themeSettings),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = folder.path,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(dynamicPrimaryColor(themeSettings).copy(alpha = 0.22f))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = "${folder.songCount}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
    }
}
