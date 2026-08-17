package com.Ksinius.musico.data

import android.content.Context
import android.content.SharedPreferences
import com.Ksinius.musico.model.ThemeSettings
import com.Ksinius.musico.ui.screens.SortOption

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("musico_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SORT_OPTION = "key_sort_option"
        private const val KEY_FAVORITES = "key_favorites"
        private const val KEY_SONG_SPEED_PREFIX = "song_speed_"
        private const val KEY_THEME_RED = "theme_red"
        private const val KEY_THEME_GREEN = "theme_green"
        private const val KEY_THEME_BLUE = "theme_blue"
    }

    fun getSortOption(): SortOption {
        val saved = prefs.getString(KEY_SORT_OPTION, SortOption.TITLE_AZ.name)
        return try {
            SortOption.valueOf(saved ?: SortOption.TITLE_AZ.name)
        } catch (e: Exception) {
            SortOption.TITLE_AZ
        }
    }

    fun saveSortOption(sortOption: SortOption) {
        prefs.edit().putString(KEY_SORT_OPTION, sortOption.name).apply()
    }

    fun getFavoriteSongIds(): Set<String> {
        return prefs.getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()
    }

    fun toggleFavorite(songId: Long): Boolean {
        val currentFavorites = getFavoriteSongIds().toMutableSet()
        val idStr = songId.toString()
        val isFav: Boolean
        if (currentFavorites.contains(idStr)) {
            currentFavorites.remove(idStr)
            isFav = false
        } else {
            currentFavorites.add(idStr)
            isFav = true
        }
        prefs.edit().putStringSet(KEY_FAVORITES, currentFavorites).apply()
        return isFav
    }

    fun saveSongSpeed(songId: Long, speed: Float) {
        prefs.edit().putFloat("$KEY_SONG_SPEED_PREFIX$songId", speed).apply()
    }

    fun getSongSpeed(songId: Long): Float {
        return prefs.getFloat("$KEY_SONG_SPEED_PREFIX$songId", 1.0f)
    }

    fun getAllSongSpeeds(): Map<Long, Float> {
        val speeds = mutableMapOf<Long, Float>()
        val allPrefs = prefs.all
        allPrefs.forEach { (key, value) ->
            if (key.startsWith(KEY_SONG_SPEED_PREFIX) && value is Float) {
                val songId = key.removePrefix(KEY_SONG_SPEED_PREFIX).toLongOrNull()
                if (songId != null) {
                    speeds[songId] = value
                }
            }
        }
        return speeds
    }

    fun saveThemeSettings(themeSettings: ThemeSettings) {
        prefs.edit()
            .putFloat(KEY_THEME_RED, themeSettings.red)
            .putFloat(KEY_THEME_GREEN, themeSettings.green)
            .putFloat(KEY_THEME_BLUE, themeSettings.blue)
            .apply()
    }

    fun getThemeSettings(): ThemeSettings {
        return ThemeSettings(
            red = prefs.getFloat(KEY_THEME_RED, 0.66f),
            green = prefs.getFloat(KEY_THEME_GREEN, 0.33f),
            blue = prefs.getFloat(KEY_THEME_BLUE, 0.97f)
        )
    }
}
