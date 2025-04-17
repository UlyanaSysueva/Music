package com.example.book.data.api

import com.example.book.data.model.LyricsResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface LyricsApi {
    @GET("v1/{artist}/{title}")
    suspend fun getLyrics(
        @Path("artist") artist: String,
        @Path("title") title: String
    ): LyricsResponse

    companion object {
        const val BASE_URL = "https://api.lyrics.ovh/"
    }
} 