package com.example.book

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object LyricsDatabase {
    private const val LYRICS_DIR = "lyrics_cache"
    private lateinit var cacheDir: File

    fun initialize(context: Context) {
        cacheDir = File(context.filesDir, LYRICS_DIR).apply {
            if (!exists()) mkdirs()
        }
    }

    private fun getLyricsFile(title: String, artist: String): File {
        val filename = "${title.hashCode()}_${artist.hashCode()}.txt"
        return File(cacheDir, filename)
    }

    suspend fun getLyricsForTrack(title: String, artist: String): String {
        return withContext(Dispatchers.IO) {
            val file = getLyricsFile(title, artist)
            if (file.exists()) file.readText() else ""
        }
    }

    suspend fun saveLyrics(title: String, artist: String, text: String) {
        withContext(Dispatchers.IO) {
            getLyricsFile(title, artist).writeText(text)
        }
    }

    suspend fun deleteLyrics(title: String, artist: String) {
        withContext(Dispatchers.IO) {
            getLyricsFile(title, artist).delete()
        }
    }
}