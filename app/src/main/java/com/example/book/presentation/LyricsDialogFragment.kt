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

class LyricsDialogFragment : DialogFragment() {

    private lateinit var progressBar: ProgressBar
    private lateinit var lyricsText: EditText
    private lateinit var saveButton: Button
    private var savedLyrics: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.LyricsDialog)
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
        val title = args.getString("title") ?: ""
        val artist = args.getString("artist") ?: ""

        titleView.text = title
        artistView.text = artist

        closeButton.setOnClickListener {
            dismiss()
        }

        saveButton.setOnClickListener {
            saveLyrics()
        }

        lyricsText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                saveButton.isEnabled = s?.toString()?.isNotEmpty() ?: false
            }
        })

        return view
    }

    private fun saveLyrics() {
        progressBar.visibility = View.VISIBLE
        saveButton.isEnabled = false

        val lyrics = lyricsText.text.toString()
        savedLyrics = lyrics

        Handler().postDelayed({
            progressBar.visibility = View.GONE
            saveButton.isEnabled = true
            Toast.makeText(context, "Текст сохранен", Toast.LENGTH_SHORT).show()
        }, 1000)
    }

    private fun getSavedLyrics(): String {
        return savedLyrics
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

    override fun dismiss() {
        if (lyricsText.text.isNotEmpty() && lyricsText.text.toString() != getSavedLyrics()) {
            showUnsavedChangesDialog()
        } else {
            super.dismiss()
        }
    }

    private fun showUnsavedChangesDialog() {
        context?.let {
            AlertDialog.Builder(it)
                .setTitle("Несохраненные изменения")
                .setMessage("У вас есть несохраненные изменения. Сохранить?")
                .setPositiveButton("Сохранить") { _, _ ->
                    saveLyrics()
                    super.dismiss()
                }
                .setNegativeButton("Отмена", null)
                .setNeutralButton("Не сохранять") { _, _ ->
                    super.dismiss()
                }
                .show()
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