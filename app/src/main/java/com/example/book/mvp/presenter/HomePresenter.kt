package com.example.book.presentation.mvp

import android.content.ContentResolver
import android.graphics.Color
import android.graphics.PorterDuff
import android.media.MediaPlayer
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import android.widget.ImageButton
import android.widget.Toast
import com.example.book.R
import com.example.book.mvp.contract.HomeView
import com.example.book.mvp.model.AudioFile
import java.io.File
import java.util.*

class HomePresenter(
    private val view: HomeView,
    private val contentResolver: ContentResolver
) {
    private val handler = Handler()
    private var runnable: Runnable = Runnable { }
    private var currentPosition = -1
    private var isPrepared = false
    private var isPlaying = false
    private var isShuffleMode = false
    private var shuffleOrder = mutableListOf<Int>()
    private var currentShuffleIndex = 0

    private var allTracks = mutableListOf<AudioFile>()
    private var musicList = mutableListOf<AudioFile>()

    private var mediaPlayer: MediaPlayer? = null
    init {
        initMediaPlayer()
    }

    private fun initMediaPlayer() {
        mediaPlayer?.release() // Освобождаем предыдущий экземпляр
        mediaPlayer = MediaPlayer().apply {
            setOnCompletionListener { playNext() }
            setOnErrorListener { _, what, extra ->
                Log.e("MediaPlayer", "Error what=$what extra=$extra")
                false
            }
        }
        isPrepared = false
        isPlaying = false
    }

    fun getMusicList() = musicList
    fun isPlaying() = isPlaying

    fun loadTracks() {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.IS_MUSIC
        )

        val selection = """
            ${MediaStore.Audio.Media.MIME_TYPE} LIKE 'audio/%' 
            AND ${MediaStore.Audio.Media.IS_MUSIC} = 1
            AND ${MediaStore.Audio.Media.DATA} NOT LIKE '%/Recordings/%'
            AND ${MediaStore.Audio.Media.DATA} NOT LIKE '%/Voice/%'
            AND ${MediaStore.Audio.Media.DATA} NOT LIKE '%/Sounds/%'
        """.trimIndent()

        val tempList = mutableListOf<AudioFile>()

        contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            "${MediaStore.Audio.Media.TITLE} ASC"
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE))
                val artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST))
                val path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA))
                val duration = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION))
                val isMusic = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.IS_MUSIC)) == 1

                if (File(path).exists() && isMusic) {
                    tempList.add(AudioFile(title, artist ?: "Unknown", path, duration))
                }
            }
        }

        allTracks.clear()
        allTracks.addAll(tempList)
        musicList.clear()
        musicList.addAll(tempList)
        view.showTracks(musicList)
    }

    fun playMusic(position: Int) {
        if (position !in musicList.indices) return

        try {
            if (currentPosition == position && isPrepared && mediaPlayer?.isPlaying == true) {
                mediaPlayer?.start()
                isPlaying = true
                updateUI(musicList[position])
                startSeekBarUpdate()
                return
            }

            mediaPlayer?.reset()
            currentPosition = position
            isPrepared = false

            if (isShuffleMode) {
                currentShuffleIndex = shuffleOrder.indexOf(position).coerceAtLeast(0)
            }

            val track = musicList[position]
            mediaPlayer?.setDataSource(track.path)
            mediaPlayer?.prepareAsync()

            mediaPlayer?.setOnPreparedListener {
                isPrepared = true
                isPlaying = true
                mediaPlayer?.start()
                updateUI(track)
                startSeekBarUpdate()
            }

        } catch (e: Exception) {
            Log.e("MediaPlayer", "Error: ${e.message}")
            view.showToast("Error playing track")
        }
    }

    private fun updateUI(track: AudioFile) {
        view.updateTrackInfo(track.title, track.artist)
        view.updatePlayPauseButton(isPlaying)
        mediaPlayer?.let {
            view.updateSeekBar(it.duration)
        }
        view.updateNowPlayingPosition(currentPosition)
    }

     fun startSeekBarUpdate() {
        runnable = object : Runnable {
            override fun run() {
                if (isPrepared) {
                    mediaPlayer?.let{
                        view.updateSeekBarProgress(it.currentPosition)
                    }
                }
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(runnable)
    }

    fun seekTo(progress: Int) {
        if (isPrepared && mediaPlayer?.isPlaying == true) {
            mediaPlayer?.seekTo(progress)
        }
    }

    fun filterTracks(query: String) {
        val result = if (query.isEmpty()) allTracks
        else allTracks.filter {
            it.title.lowercase(Locale.getDefault()).contains(query.lowercase(Locale.getDefault())) ||
                    it.artist.lowercase(Locale.getDefault()).contains(query.lowercase(Locale.getDefault()))
        }
        musicList.clear()
        musicList.addAll(result)
        view.showTracks(result)
    }

    fun playNext() {
        if (musicList.isEmpty() || !isPrepared) return

        val nextPos = if (isShuffleMode) {
            currentShuffleIndex = (currentShuffleIndex + 1) % shuffleOrder.size
            shuffleOrder[currentShuffleIndex]
        } else {
            (currentPosition + 1) % musicList.size
        }
        playMusic(nextPos)
    }

     fun playPrevious() {
        if (musicList.isEmpty()) return

        val prevPos = if (isShuffleMode) {
            // Берем предыдущий трек из shuffleOrder
            currentShuffleIndex = if (currentShuffleIndex - 1 < 0) shuffleOrder.size - 1 else currentShuffleIndex - 1
            shuffleOrder[currentShuffleIndex]
        } else {
            if (currentPosition - 1 < 0) musicList.size - 1 else currentPosition - 1
        }

        playMusic(prevPos)

     }

     fun toggleShuffle() {
        isShuffleMode = !isShuffleMode
        view.updateShuffleButton(isShuffleMode)

        if (isShuffleMode) {
            // Генерируем новый случайный порядок
            generateShuffleOrder()
            view.showToast("Shuffle: ON")
        } else {
            view.showToast("Shuffle: OFF")
        }
     }

    fun generateShuffleOrder() {
        val indices = musicList.indices.filter { it != currentPosition }.toMutableList()
        indices.shuffle()
        if (currentPosition in musicList.indices) {
            indices.add(0, currentPosition)
        }
        shuffleOrder = indices
        currentShuffleIndex = 0
    }

    fun logOut() {
        mediaPlayer?.let {
            if(it.isPlaying) {
                it.stop()
            }
            it.release()
        }
        view.clearUserSession()
        view.navigateToLogin()
    }

    fun getCurrentPosition(): Int {
        return currentPosition
    }

    fun getCurrentTrack(): AudioFile? {
        return if (currentPosition in musicList.indices) {
            musicList[currentPosition]
        } else {
            null
        }
    }

    fun pauseMusic() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                isPlaying = false
                view.updatePlayPauseButton(false)
                handler.removeCallbacks(runnable)
            }
        }
    }

    fun playCurrent() {
        if (currentPosition == -1) return

        if (mediaPlayer == null) {
            initMediaPlayer() // Пересоздаем MediaPlayer при необходимости
        }

        when {
            isPrepared -> {
                mediaPlayer?.start()
                isPlaying = true
                view.updatePlayPauseButton(true)
                musicList[currentPosition].let {
                    view.updatePlayerUI(it)
                    view.updateTrackInfo(it.title, it.artist)
                }
                startSeekBarUpdate()
            }
            mediaPlayer != null -> {
                // Перезагружаем трек если MediaPlayer существует
                playMusic(currentPosition)
            }
            else -> {
                // Полная переинициализация при проблемах
                initMediaPlayer()
                playMusic(currentPosition)
            }
        }
    }

    fun releaseMediaPlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
    }

    fun getCurrentDuration(): Int? = mediaPlayer?.duration
}
