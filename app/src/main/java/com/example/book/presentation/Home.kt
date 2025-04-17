package com.example.book.presentation

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.graphics.Color
import android.graphics.PorterDuff
import android.media.AudioManager
import android.provider.MediaStore
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ImageButton
import android.widget.SearchView
import android.widget.SeekBar
import android.widget.Toast
import com.example.book.R
import java.io.File
import java.io.IOException
import java.util.Locale
import com.example.book.data.AudioFile
import com.example.book.presentation.MusicAdapter
import com.example.book.presentation.MusicAdapterListener

class Home : AppCompatActivity(), MusicAdapterListener {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var recyclerView: RecyclerView
    private lateinit var mediaPlayer: MediaPlayer
    private var currentPosition = -1
    private var isPlaying = false
    private var musicList = mutableListOf<AudioFile>()
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var runnable: Runnable
    private lateinit var adapter: MusicAdapter
    private var allTracks = mutableListOf<AudioFile>()
    private var isShuffleMode = false
    private var shuffleOrder = mutableListOf<Int>()
    private var currentShuffleIndex = 0
    private var isPrepared = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!checkAuth()) return

        setContentView(R.layout.activity_home)
        sharedPreferences = getSharedPreferences("user_session", MODE_PRIVATE)

        initViews()
        setupMediaPlayer()
        setupPlayerControls()
        setupSearchView()
        loadMusicFiles()

        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        loadUserData()
    }

    private fun checkAuth(): Boolean {
        sharedPreferences = getSharedPreferences("user_session", MODE_PRIVATE)
        val email = sharedPreferences.getString("EMAIL_KEY", "") ?: ""

        if (email.isEmpty()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return false
        }

        return true
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.setHasFixedSize(true)
    }

    private fun setupMediaPlayer() {
        mediaPlayer = MediaPlayer().apply {
            setAudioStreamType(AudioManager.STREAM_MUSIC)
            setOnCompletionListener { playNext() }
            setOnPreparedListener {
                isPrepared = true
                if (isPlaying) {
                    start()
                    updatePlayerUI(musicList[currentPosition])
                    startSeekBarUpdate()
                }
            }
            setOnErrorListener { _, what, extra ->
                Log.e("MediaPlayer", "Error what=$what extra=$extra")
                false
            }
        }
    }

    private fun loadMusicFiles() {
        val tempList = mutableListOf<AudioFile>()
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
        updateRecyclerView(tempList)
    }

    private fun setupSearchView() {
        val searchView = findViewById<SearchView>(R.id.searchView)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterTracks(newText.orEmpty())
                return true
            }
        })
    }

    private fun filterTracks(query: String) {
        if (allTracks.isEmpty()) return

        val filteredList = if (query.isEmpty()) {
            allTracks.toList()
        } else {
            val lowerCaseQuery = query.lowercase(Locale.getDefault())
            allTracks.filter {
                it.title.lowercase(Locale.getDefault()).contains(lowerCaseQuery) ||
                        it.artist.lowercase(Locale.getDefault()).contains(lowerCaseQuery)
            }
        }
        
        musicList.clear()
        musicList.addAll(filteredList)

        runOnUiThread {
            adapter.updateList(musicList)
        }
    }

    private fun updateRecyclerView(list: List<AudioFile>) {
        if (!isFinishing && !isDestroyed) {
            musicList.clear()
            musicList.addAll(list)
            runOnUiThread {
                if (!::adapter.isInitialized) {
                    adapter = MusicAdapter(musicList, this, this)
                    recyclerView.adapter = adapter
                } else {
                    adapter.updateList(musicList)
                }
            }
        }
    }

    private fun playMusic(position: Int) {
        if (position !in musicList.indices) return

        try {
            if (currentPosition == position && isPrepared) {
                mediaPlayer.start()
                isPlaying = true
                updatePlayerUI(musicList[position])
                startSeekBarUpdate()
                return
            }

            mediaPlayer.reset()
            currentPosition = position // Всегда сохраняем реальную позицию
            isPrepared = false

            // Обновляем текущий индекс в shuffleOrder, если режим активен
            if (isShuffleMode) {
                currentShuffleIndex = shuffleOrder.indexOf(position).coerceAtLeast(0)
            }

            val track = musicList[position]
            mediaPlayer.setDataSource(track.path)

            mediaPlayer.prepareAsync()  // Начинаем асинхронную подготовку

            // Устанавливаем слушатель для подготовки трека
            mediaPlayer.setOnPreparedListener { mp ->
                isPrepared = true
                isPlaying = true
                updatePlayerUI(track)  // Обновляем UI после успешной подготовки
                updateNowPlayingIndicator()
                startSeekBarUpdate()
                mediaPlayer.start()
            }

        } catch (e: Exception) {
            Log.e("MediaPlayer", "Error: ${e.message}")
            Toast.makeText(this, "Error playing track", Toast.LENGTH_SHORT).show()
        }

    }

    private fun generateShuffleOrder() {
        if (musicList.isEmpty()) {
            shuffleOrder = mutableListOf()
            return
        }
        // Создаем список всех индексов, кроме текущего
        val indices = musicList.indices.filter { it != currentPosition }.toMutableList()
        indices.shuffle()

        // Добавляем текущий трек в начало
        if (currentPosition in musicList.indices) {
            indices.add(0, currentPosition)
        }

        shuffleOrder = indices
        currentShuffleIndex = 0
    }



    private fun updatePlayerUI(track: AudioFile) {
        findViewById<TextView>(R.id.currentTrackTitle).text = track.title
        findViewById<TextView>(R.id.currentTrackArtist).text = track.artist
        findViewById<ImageButton>(R.id.btnPlayPause).setImageResource(
            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        )
        findViewById<SeekBar>(R.id.seekBar).max = mediaPlayer.duration
    }

    private fun startSeekBarUpdate() {
        val seekBar = findViewById<SeekBar>(R.id.seekBar)
        seekBar.progress = mediaPlayer.currentPosition
        runnable = Runnable {
            seekBar.progress = mediaPlayer.currentPosition
            handler.postDelayed(runnable, 1000)
        }
        handler.post(runnable)
    }

    private fun updateNowPlayingIndicator() {
        (recyclerView.adapter as? MusicAdapter)?.setNowPlayingPosition(currentPosition)
    }

    private fun playNext() {
        if (musicList.isEmpty()) return

        val nextPos = if (isShuffleMode) {
            // Берем следующий трек из shuffleOrder
            currentShuffleIndex = (currentShuffleIndex + 1) % shuffleOrder.size
            shuffleOrder[currentShuffleIndex]
        } else {
            (currentPosition + 1) % musicList.size
        }

        playMusic(nextPos)

    }

    private fun playPrevious() {
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

    private fun toggleShuffleMode() {
        isShuffleMode = !isShuffleMode
        if (isShuffleMode) {
            shuffleOrder = (0 until musicList.size).toMutableList()
            shuffleOrder.shuffle()
            if (currentPosition != -1) {
                val currentIndex = shuffleOrder.indexOf(currentPosition)
                if (currentIndex != -1) {
                    val temp = shuffleOrder[0]
                    shuffleOrder[0] = currentPosition
                    shuffleOrder[currentIndex] = temp
                }
                currentShuffleIndex = 0
            }
        }
        findViewById<ImageButton>(R.id.btnShuffle)?.apply {
            setColorFilter(if (isShuffleMode) Color.CYAN else Color.WHITE)
        }
    }

    private fun setupPlayerControls() {
        val btnShuffle = findViewById<ImageButton>(R.id.btnShuffle)
        val btnPlayPause = findViewById<ImageButton>(R.id.btnPlayPause)
        val btnPrev = findViewById<ImageButton>(R.id.btnPrev)
        val btnNext = findViewById<ImageButton>(R.id.btnNext)
        val seekBar = findViewById<SeekBar>(R.id.seekBar)
        val btnLyrics = findViewById<ImageButton>(R.id.btnLyrics)

        btnLyrics.setOnClickListener {
            onCurrentTrackLyricsRequested() // Открываем текст текущей песни
        }

        btnShuffle.setOnClickListener {
            toggleShuffle()
        }

        btnPlayPause.setOnClickListener {
            if (isPlaying) pauseMusic() else playCurrent()
        }

        btnNext.setOnClickListener { playNext() }
        btnPrev.setOnClickListener { playPrevious() }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) mediaPlayer.seekTo(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    override fun onCurrentTrackLyricsRequested() {
        if (currentPosition != -1) {
            showLyricsDialog(musicList[currentPosition])
        } else {
            Toast.makeText(this, "Нет играющего трека", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showLyricsDialog(track: AudioFile) {
        LyricsDialogFragment.newInstance(track.title, track.artist)
            .show(supportFragmentManager, "lyrics_dialog")
    }


    private fun toggleShuffle() {
        isShuffleMode = !isShuffleMode
        updateShuffleButton()

        if (isShuffleMode) {
            // Генерируем новый случайный порядок
            generateShuffleOrder()
            Toast.makeText(this, "Shuffle: ON", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Shuffle: OFF", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateShuffleButton() {
        val btnShuffle = findViewById<ImageButton>(R.id.btnShuffle)
        btnShuffle.setColorFilter(
            if (isShuffleMode) Color.parseColor("#FFD700") else Color.WHITE,
            PorterDuff.Mode.SRC_IN
        )
        btnShuffle.animate().rotationBy(360f).setDuration(300).start()
    }

    private fun pauseMusic() {
        if (isPlaying && isPrepared) {
            mediaPlayer.pause()
            isPlaying = false
            findViewById<ImageButton>(R.id.btnPlayPause).setImageResource(R.drawable.ic_play)
            handler.removeCallbacks(runnable)
        }
    }

    private fun playCurrent() {
        if (currentPosition != -1) {
            if (isPrepared) {
                // Если трек уже подготовлен, просто возобновляем воспроизведение
                mediaPlayer.start()
                isPlaying = true
                updatePlayerUI(musicList[currentPosition])
                startSeekBarUpdate()
            } else {
                // Если трек не подготовлен, запускаем его заново
                playMusic(currentPosition)
            }
        }
    }

    override fun onLyricsButtonClicked(track: AudioFile) {
        showLyricsDialog(track)
    }

    override fun onTrackSelected(track: AudioFile, position: Int) {
        playMusic(position) // Реализация из предыдущих исправлений
    }


    private fun loadUserData() {
        val email = sharedPreferences.getString("EMAIL_KEY", "")
        findViewById<TextView>(R.id.email).text = email
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runnable)
        mediaPlayer.release()
    }
} 