package com.example.book

interface MusicAdapterListener {
    fun onTrackSelected(track: AudioFile, position: Int) // Для воспроизведения
    fun onLyricsButtonClicked(track: AudioFile) // Для текста песни в списке
    fun onCurrentTrackLyricsRequested() // Для текста текущей песни
}