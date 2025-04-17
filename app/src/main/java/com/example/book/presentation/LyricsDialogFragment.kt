package com.example.book.presentation

import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.book.R
import com.example.book.data.api.LyricsApi
import com.example.book.data.repository.LyricsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class LyricsDialogFragment : DialogFragment() {

    private lateinit var progressBar: ProgressBar
    private lateinit var lyricsText: EditText
    private lateinit var saveButton: Button
    private lateinit var lyricsRepository: LyricsRepository
    private var currentArtist: String = ""
    private var currentTitle: String = ""
    private var hasUnsavedChanges: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.LyricsDialog)
        
        val retrofit = Retrofit.Builder()
            .baseUrl(LyricsApi.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        val lyricsApi = retrofit.create(LyricsApi::class.java)
        lyricsRepository = LyricsRepository(requireContext(), lyricsApi)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_lyrics, container, false)

        progressBar = view.findViewById(R.id.progressBar)
        val titleView = view.findViewById<TextView>(R.id.lyricsTrackTitle)
        val artistView = view.findViewById<TextView>(R.id.lyricsTrackArtist)
        val closeButton = view.findViewById<ImageButton>(R.id.btnCloseLyrics)
        lyricsText = view.findViewById(R.id.lyricsText)
        saveButton = view.findViewById(R.id.btnSaveLyrics)

        val args = arguments ?: return view
        currentTitle = args.getString("title") ?: ""
        currentArtist = args.getString("artist") ?: ""

        titleView.text = currentTitle
        artistView.text = currentArtist

        closeButton.setOnClickListener {
            checkUnsavedChanges()
        }

        saveButton.setOnClickListener {
            saveLyrics()
        }

        lyricsText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val hasText = s?.toString()?.isNotEmpty() ?: false
                saveButton.isEnabled = hasText
                hasUnsavedChanges = hasText
            }
        })

        loadLyrics()

        return view
    }

    private fun loadLyrics() {
        progressBar.visibility = View.VISIBLE
        lyricsText.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val lyrics = lyricsRepository.getLyrics(currentArtist, currentTitle)
                withContext(Dispatchers.Main) {
                    lyricsText.setText(lyrics)
                    saveButton.isEnabled = lyrics.isNotEmpty()
                    hasUnsavedChanges = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Ошибка загрузки текста: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    lyricsText.isEnabled = true
                }
            }
        }
    }

    private fun saveLyrics() {
        progressBar.visibility = View.VISIBLE
        saveButton.isEnabled = false

        val lyrics = lyricsText.text.toString()
        lyricsRepository.saveLyrics(currentArtist, currentTitle, lyrics)
        hasUnsavedChanges = false

        Handler().postDelayed({
            progressBar.visibility = View.GONE
            saveButton.isEnabled = true
            Toast.makeText(context, "Текст сохранен", Toast.LENGTH_SHORT).show()
        }, 1000)
    }

    private fun checkUnsavedChanges() {
        if (hasUnsavedChanges) {
            showUnsavedChangesDialog()
        } else {
            dismiss()
        }
    }

    private fun showUnsavedChangesDialog() {
        context?.let {
            AlertDialog.Builder(it)
                .setTitle("Несохраненные изменения")
                .setMessage("У вас есть несохраненные изменения. Сохранить?")
                .setPositiveButton("Сохранить") { _, _ ->
                    saveLyrics()
                    dismiss()
                }
                .setNegativeButton("Отмена", null)
                .setNeutralButton("Не сохранять") { _, _ ->
                    dismiss()
                }
                .show()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("lyrics_text", lyricsText.text.toString())
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        savedInstanceState?.getString("lyrics_text")?.let {
            lyricsText.setText(it)
        }
    }

    companion object {
        fun newInstance(title: String, artist: String): LyricsDialogFragment {
            return LyricsDialogFragment().apply {
                arguments = Bundle().apply {
                    putString("title", title)
                    putString("artist", artist)
                }
            }
        }
    }
} 