package com.example.book.presentation

import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.book.R
import com.example.book.data.AudioFile

class MusicAdapter(
    private var musicList: List<AudioFile>,
    private val context: Context,
    private val listener: MusicAdapterListener) :
    RecyclerView.Adapter<MusicAdapter.MusicViewHolder>() {

    private var nowPlayingPosition = -1

    fun setNowPlayingPosition(position: Int) {
        val oldPos = nowPlayingPosition
        nowPlayingPosition = position
        if (oldPos != -1) notifyItemChanged(oldPos)
        if (position != -1) notifyItemChanged(position)
    }

    inner class MusicViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.musicTitle)
        val artist: TextView = itemView.findViewById(R.id.musicArtist)
        val duration: TextView = itemView.findViewById(R.id.musicDuration)
        val btnTrackLyrics: ImageButton = itemView.findViewById(R.id.btnTrackLyrics)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    try {
                        listener.onTrackSelected(musicList[position], position)
                    } catch (e: Exception) {
                        Log.e("MusicAdapter", "Error in click: ${e.message}")
                    }
                }
            }

            btnTrackLyrics.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    try {
                        listener.onLyricsButtonClicked(musicList[position])
                    } catch (e: Exception) {
                        Log.e("MusicAdapter", "Error in lyrics click: ${e.message}")
                    }
                }
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MusicViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_music, parent, false)
        return MusicViewHolder(view)
    }

    override fun onBindViewHolder(holder: MusicViewHolder, position: Int) {
        val music = musicList[position]
        holder.title.text = music.title
        holder.artist.text = music.artist
        holder.duration.text = formatDuration(music.duration)

        holder.itemView.setBackgroundColor(
            if (position == nowPlayingPosition) Color.parseColor("#2A2A2A")
            else Color.TRANSPARENT
        )
    }

    private fun formatDuration(durationMs: Long): String {
        val minutes = (durationMs / 1000) / 60
        val seconds = (durationMs / 1000) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    fun updateList(newList: List<AudioFile>) {
        musicList = newList
        notifyDataSetChanged()
    }

    override fun getItemCount() = musicList.size
} 