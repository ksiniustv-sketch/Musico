package com.Ksinius.musico.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.Ksinius.musico.model.Song
import java.io.File

object AudioScanner {

    fun scanAudioFiles(context: Context): List<Song> {
        val songsList = mutableListOf<Song>()
        val contentResolver = context.contentResolver

        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_ADDED
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            val cursor = contentResolver.query(uri, projection, selection, null, sortOrder)
            cursor?.use { c ->
                val idColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val sizeColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val dateAddedColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

                while (c.moveToNext()) {
                    val id = c.getLong(idColumn)
                    val filePath = c.getString(dataColumn) ?: ""
                    val title = c.getString(titleColumn) ?: File(filePath).nameWithoutExtension
                    val artist = c.getString(artistColumn) ?: "<Unknown Artist>"
                    val album = c.getString(albumColumn) ?: "<Unknown Album>"
                    val duration = c.getLong(durationColumn)
                    val size = c.getLong(sizeColumn)
                    val dateAddedSec = c.getLong(dateAddedColumn)
                    val dateAddedMs = if (dateAddedSec > 0) dateAddedSec * 1000L else File(filePath).lastModified()

                    // Strictly exclude system folders, backup files, and hidden directories
                    if (isIgnoredPath(filePath)) {
                        continue
                    }

                    // Check file extension (.mp3 or .wav)
                    val lowerPath = filePath.lowercase()
                    val extension = when {
                        lowerPath.endsWith(".mp3") -> "mp3"
                        lowerPath.endsWith(".wav") -> "wav"
                        else -> null
                    }

                    if (extension != null) {
                        val contentUri = ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                        )

                        val displayArtist = if (artist == "<unknown>" || artist.isBlank()) "Unknown Artist" else artist
                        val displayTitle = if (title.isBlank()) File(filePath).nameWithoutExtension else title

                        songsList.add(
                            Song(
                                id = id,
                                title = displayTitle,
                                artist = displayArtist,
                                album = album,
                                duration = duration,
                                contentUri = contentUri,
                                filePath = filePath,
                                extension = extension.uppercase(),
                                fileSize = size,
                                dateAdded = dateAddedMs
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return songsList
    }

    /**
     * Exclude system paths, Android folders, auto-backups, and hidden folders
     */
    private fun isIgnoredPath(path: String): Boolean {
        if (path.isEmpty()) return true
        val normalizedPath = path.replace('\\', '/').lowercase()

        // Exclude system Android directories
        if (normalizedPath.contains("/android/") || normalizedPath.startsWith("android/")) {
            return true
        }

        // Exclude auto-backup folders and files containing "autobackup" or "backup"
        if (normalizedPath.contains("autobackup") || normalizedPath.contains("backup")) {
            return true
        }

        // Exclude hidden folders/files starting with a dot
        val components = normalizedPath.split('/')
        for (part in components) {
            if (part.startsWith(".") && part.length > 1) {
                return true
            }
        }

        return false
    }
}
