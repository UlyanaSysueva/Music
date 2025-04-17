package com.example.book.di

import android.app.Application
import com.example.book.data.LyricsDatabase

class BookApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        LyricsDatabase.initialize(this)
    }
} 