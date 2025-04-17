package com.example.book

import android.app.Application

class BookApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        LyricsDatabase.initialize(this)
    }
}