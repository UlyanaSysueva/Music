package com.example.book

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DbUsers(val context: Context, val factory: SQLiteDatabase.CursorFactory?) :
    SQLiteOpenHelper(context, "app.db", factory, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE users(id INTEGER PRIMARY KEY AUTOINCREMENT, email TEXT UNIQUE, pass TEXT)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS users")
        onCreate(db)
    }

    fun addUser(user: User): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put("email", user.email)
            put("pass", user.pass)
        }
        return try {
            db.insert("users", null, values) != -1L
        } catch (e: Exception) {
            false
        } finally {
            db.close()
        }
    }

    fun getUser(email: String, pass: String): Boolean {
        val db = this.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM users WHERE email = ? AND pass = ?",
            arrayOf(email, pass)
        )
        val exists = cursor.count > 0
        cursor.close()
        return exists
    }

    fun checkUserExists(email: String): Boolean {
        val db = this.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM users WHERE email = ?",
            arrayOf(email)
        )
        val exists = cursor.count > 0
        cursor.close()
        return exists
    }
}