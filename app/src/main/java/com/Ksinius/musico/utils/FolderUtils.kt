package com.Ksinius.musico.utils

import com.Ksinius.musico.model.MusicFolder
import com.Ksinius.musico.model.Song
import java.io.File

object FolderUtils {

    fun parentFolderPath(song: Song): String {
        return File(song.filePath).parent?.replace('\\', '/') ?: "Unknown"
    }

    fun folderName(folderPath: String): String {
        if (folderPath == "Unknown") return folderPath
        return File(folderPath).name.ifBlank { folderPath }
    }

    fun groupSongsIntoFolders(songs: List<Song>): List<MusicFolder> {
        return songs
            .groupBy { parentFolderPath(it) }
            .map { (path, folderSongs) ->
                MusicFolder(
                    path = path,
                    name = folderName(path),
                    songCount = folderSongs.size
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    fun songsInFolder(songs: List<Song>, folderPath: String): List<Song> {
        return songs.filter { parentFolderPath(it) == folderPath }
    }
}
