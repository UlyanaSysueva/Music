package com.example.book.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.book.data.api.ChartLyricsApi
import com.example.book.data.api.LyricsApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.simplexml.SimpleXmlConverterFactory

class LyricsRepository(
    private val context: Context,
    private val lyricsApi: LyricsApi
) {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("lyrics_prefs", Context.MODE_PRIVATE)
    }

    private val chartLyricsApi: ChartLyricsApi by lazy {
        Retrofit.Builder()
            .baseUrl(ChartLyricsApi.BASE_URL)
            .addConverterFactory(SimpleXmlConverterFactory.create())
            .build()
            .create(ChartLyricsApi::class.java)
    }

    suspend fun getLyrics(artist: String, title: String): String {
        return withContext(Dispatchers.IO) {
            // Сначала проверяем сохраненный текст
            val savedLyrics = getSavedLyrics(artist, title)
            if (savedLyrics != null) {
                return@withContext savedLyrics
            }

            // Если сохраненного текста нет, пробуем получить из первого источника
            try {
                val response = lyricsApi.getLyrics(artist, title)
                if (response.error == null) {
                    return@withContext response.lyrics
                }
            } catch (e: Exception) {
                // Продолжаем к следующему источнику
            }

            // Если первый источник не дал результатов, пробуем ChartLyrics
            try {
                val searchResponse = chartLyricsApi.searchLyrics(artist, title)
                if (searchResponse.SearchLyricResult.isNotEmpty()) {
                    val result = searchResponse.SearchLyricResult.first()
                    val lyricResponse = chartLyricsApi.getLyric(result.LyricId, result.LyricChecksum)
                    return@withContext lyricResponse.GetLyricResult.Lyric
                }
            } catch (e: Exception) {
                // Если оба источника не дали результатов, возвращаем пустую строку
            }

            ""
        }
    }

    fun saveLyrics(artist: String, title: String, lyrics: String) {
        val key = generateKey(artist, title)
        prefs.edit().putString(key, lyrics).apply()
    }

    fun getSavedLyrics(artist: String, title: String): String? {
        val key = generateKey(artist, title)
        return prefs.getString(key, null)
    }

    private fun generateKey(artist: String, title: String): String {
        return "${artist}_${title}".toLowerCase()
    }
} 