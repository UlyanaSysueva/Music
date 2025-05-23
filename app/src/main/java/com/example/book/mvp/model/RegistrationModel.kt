package com.example.book.mvp.model

import android.content.Context
import com.example.book.data.DbUsers

class RegistrationModel(private val context: Context) {

    fun registerUser(email: String, password: String): Boolean {
        return try {
            val db = DbUsers(context, null)
            db.addUser(User(email, password))
            true
        } catch (e: Exception) {
            false
        }
    }

}