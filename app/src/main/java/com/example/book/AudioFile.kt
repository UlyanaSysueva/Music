package com.example.book

import java.io.Serializable

data class AudioFile(
    val title: String,
    val artist: String,
    val path: String,
    val duration: Long
) : Serializable