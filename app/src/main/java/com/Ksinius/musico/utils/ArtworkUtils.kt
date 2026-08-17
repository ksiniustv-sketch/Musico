package com.Ksinius.musico.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.util.LruCache

object ArtworkUtils {

    // Memory cache for decoded album cover art
    private val memoryCache: LruCache<String, Bitmap> = object : LruCache<String, Bitmap>(30 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount
        }
    }

    fun getArtwork(context: Context, filePath: String): Bitmap? {
        if (filePath.isBlank()) return null

        // Return cached bitmap if available
        memoryCache.get(filePath)?.let { return it }

        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(filePath)
            val picture = retriever.embeddedPicture
            retriever.release()

            if (picture != null) {
                val bitmap = BitmapFactory.decodeByteArray(picture, 0, picture.size)
                if (bitmap != null) {
                    memoryCache.put(filePath, bitmap)
                }
                bitmap
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
