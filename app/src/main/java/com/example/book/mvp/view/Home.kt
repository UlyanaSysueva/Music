package com.example.book.mvp.view

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.PorterDuff
import android.media.MediaPlayer
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.book.R
import com.example.book.mvp.contract.HomeView
import com.example.book.mvp.contract.MusicAdapterListener
import com.example.book.mvp.model.AudioFile
import com.example.book.mvp.model.MusicAdapter
import com.example.book.presentation.LyricsDialogFragment
import com.example.book.presentation.mvp.HomePresenter

class Home : AppCompatActivity(), HomeView, MusicAdapterListener {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var presenter: HomePresenter
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MusicAdapter
    private lateinit var seekBar: SeekBar

    private var isPlaying = false
    private var isShuffleMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedPreferences = getSharedPreferences("user_session", MODE_PRIVATE)
        if (sharedPreferences.getString("EMAIL_KEY", "").isNullOrEmpty()) {
            navigateToLogin()
            return
        }

        setContentView(R.layout.activity_home)
        presenter = HomePresenter(this, contentResolver)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = MusicAdapter(presenter.getMusicList(), this, this)
        recyclerView.adapter = adapter

        seekBar = findViewById(R.id.seekBar)
        setupPlayerControls()

        findViewById<ImageButton>(R.id.logOut).setOnClickListener {
            presenter.logOut()
        }

        findViewById<SearchView>(R.id.searchView).setOnQueryTextListener(object :
            SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                presenter.filterTracks(newText ?: "")
                return true
            }
        })

        presenter.loadTracks()
        loadUserData()
    }

    private fun loadUserData() {
        val email = sharedPreferences.getString("EMAIL_KEY", "")
        findViewById<TextView>(R.id.email).text = email
    }

    override fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun navigateToLogin() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finishAffinity()
    }

    override fun showTracks(list: List<AudioFile>) {
        adapter.updateList(list)
    }

    override fun updateTrackInfo(title: String, artist: String) {
        findViewById<TextView>(R.id.currentTrackTitle).text = title
        findViewById<TextView>(R.id.currentTrackArtist).text = artist
    }

    override fun updatePlayPauseButton(isPlaying: Boolean) {
        findViewById<ImageButton>(R.id.btnPlayPause).setImageResource(
            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        )
    }

    override fun updateSeekBar(max: Int) {
        seekBar.max = max
    }

    override fun updateSeekBarProgress(progress: Int) {
        seekBar.progress = progress
    }

    override fun updateNowPlayingPosition(position: Int) {
        adapter.setNowPlayingPosition(position)
    }

    override fun clearUserSession() {
        sharedPreferences.edit().remove("EMAIL_KEY").commit()
    }

    override fun onTrackSelected(track: AudioFile, position: Int) {
        presenter.playMusic(position)
    }

    override fun onLyricsButtonClicked(track: AudioFile) {
        showLyricsDialog(track)
    }

    override fun onCurrentTrackLyricsRequested() {
        val currentTrack = presenter.getCurrentTrack()
        if (currentTrack != null) {
            showLyricsDialog(currentTrack)
        } else {
            showToast("Нет играющего трека")
        }
    }

    private fun showLyricsDialog(track: AudioFile) {
        LyricsDialogFragment.newInstance(track.title, track.artist)
            .show(supportFragmentManager, "lyrics_dialog")
    }

    private fun setupPlayerControls() {
        val btnPlayPause = findViewById<ImageButton>(R.id.btnPlayPause)
        val btnNext = findViewById<ImageButton>(R.id.btnNext)
        val btnPrev = findViewById<ImageButton>(R.id.btnPrev)
        val btnShuffle = findViewById<ImageButton>(R.id.btnShuffle)
        val btnLyrics = findViewById<ImageButton>(R.id.btnLyrics)
        val seekBar = findViewById<SeekBar>(R.id.seekBar)

        btnPlayPause.setOnClickListener {
            if (presenter.isPlaying()) {
                presenter.pauseMusic()
            } else {
                presenter.playCurrent()
            }
        }

        btnNext.setOnClickListener {
            presenter.playNext()
        }

        btnPrev.setOnClickListener {
            presenter.playPrevious()
        }

        btnShuffle.setOnClickListener {
            presenter.toggleShuffle()
        }

        btnLyrics.setOnClickListener {
            onCurrentTrackLyricsRequested()
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    presenter.seekTo(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    override fun updateShuffleButton(isShuffle: Boolean) {
        val btnShuffle = findViewById<ImageButton>(R.id.btnShuffle)
        btnShuffle.setColorFilter(
            if (isShuffle) Color.parseColor("#FFD700") else Color.WHITE,
            PorterDuff.Mode.SRC_IN
        )
        btnShuffle.animate().rotationBy(180f).setDuration(300).start()
    }

    override fun updatePlayerUI(track: AudioFile) {
        findViewById<TextView>(R.id.currentTrackTitle).text = track.title
        findViewById<TextView>(R.id.currentTrackArtist).text = track.artist
        val isPlaying = presenter.isPlaying()
        findViewById<ImageButton>(R.id.btnPlayPause).setImageResource(
            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        )
        presenter.getCurrentDuration()?.let {
            findViewById<SeekBar>(R.id.seekBar).max = it
        }
    }

    override fun onResume() {
        super.onResume()
        // Восстанавливаем состояние воспроизведения при возвращении в активность
        presenter.playCurrent()
    }

    override fun onPause() {
        super.onPause()
        // Приостанавливаем воспроизведение при уходе из активности
        presenter.pauseMusic()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Освобождаем ресурсы MediaPlayer
        presenter.releaseMediaPlayer()
    }
}
