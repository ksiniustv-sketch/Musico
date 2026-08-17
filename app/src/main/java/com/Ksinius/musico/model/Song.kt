package com.Ksinius.musico.model

import android.net.Uri

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val contentUri: Uri,
    val filePath: String,
    val extension: String,
    val fileSize: Long = 0L,
    val dateAdded: Long = 0L
)
