package com.example.book.mvp.contract

import com.example.book.mvp.model.AudioFile

interface HomeView {
    fun showToast(message: String)
    fun navigateToLogin()
    fun showTracks(list: List<AudioFile>)
    fun updatePlayerUI(track: AudioFile)
    fun updateTrackInfo(title: String, artist: String)
    fun updatePlayPauseButton(isPlaying: Boolean)
    fun updateSeekBar(max: Int)
    fun updateSeekBarProgress(progress: Int)
    fun updateNowPlayingPosition(position: Int)
    fun updateShuffleButton(isShuffle: Boolean)
    fun clearUserSession()
}
