package com.example.book.data.api

import retrofit2.http.GET
import retrofit2.http.Query

interface ChartLyricsApi {
    @GET("SearchLyric")
    suspend fun searchLyrics(
        @Query("artist") artist: String,
        @Query("song") title: String
    ): ChartLyricsResponse

    @GET("GetLyric")
    suspend fun getLyric(
        @Query("lyricId") lyricId: Int,
        @Query("lyricCheckSum") checkSum: String
    ): ChartLyricsTextResponse

    companion object {
        const val BASE_URL = "http://api.chartlyrics.com/apiv1.asmx/"
    }
}

data class ChartLyricsResponse(
    val SearchLyricResult: List<ChartLyricsResult>
)

data class ChartLyricsResult(
    val TrackId: Int,
    val LyricId: Int,
    val LyricChecksum: String,
    val LyricUrl: String,
    val ArtistUrl: String,
    val Artist: String,
    val Song: String,
    val SongRank: Int,
    val TrackChecksum: String
)

data class ChartLyricsTextResponse(
    val GetLyricResult: ChartLyricsTextResult
)

data class ChartLyricsTextResult(
    val TrackId: Int,
    val LyricChecksum: String,
    val LyricId: Int,
    val LyricSong: String,
    val LyricArtist: String,
    val LyricUrl: String,
    val LyricCovertArtUrl: String,
    val LyricRank: Int,
    val LyricCorrectUrl: String,
    val Lyric: String
) 