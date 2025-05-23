package com.example.book.mvp.contract

import com.example.book.mvp.model.AudioFile

interface MusicAdapterListener {
    fun onTrackSelected(track: AudioFile, position: Int) // Для воспроизведения
    fun onLyricsButtonClicked(track: AudioFile) // Для текста песни в списке
    fun onCurrentTrackLyricsRequested() // Для текста текущей песни
} 