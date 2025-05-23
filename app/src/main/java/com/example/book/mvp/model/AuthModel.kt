package com.example.book.mvp.model

import android.content.Context
import android.content.SharedPreferences
import com.example.book.data.DbUsers

class AuthModel(
    private val context: Context,
    private val sharedPreferences: SharedPreferences
) {

    fun checkAuthentication(): Boolean {
        val email = sharedPreferences.getString("EMAIL_KEY", null) ?: return false
        return DbUsers(context, null).checkUserExists(email)
    }

    fun authenticateUser(email: String, password: String): Boolean {
        return DbUsers(context, null).getUser(email, password)
    }

    fun saveUserSession(email: String) {
        sharedPreferences.edit().putString("EMAIL_KEY", email).apply()
    }

    fun checkFirstRun() {
        val isFirstRun = sharedPreferences.getBoolean(PREF_FIRST_RUN, true)
        if (isFirstRun) {
            sharedPreferences.edit().putBoolean(PREF_FIRST_RUN, false).apply()
        }
    }

    companion object {
        const val PREF_FIRST_RUN = "first_run"
    }
}