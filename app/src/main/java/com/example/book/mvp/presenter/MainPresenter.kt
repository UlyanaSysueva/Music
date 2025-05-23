package com.example.book.mvp.presenter

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import com.example.book.mvp.contract.MainContract
import com.example.book.mvp.contract.MainContract.Presenter.Companion.PERMISSION_REQUEST_CODE
import com.example.book.mvp.model.AuthModel
import com.example.book.mvp.view.Registration

class MainPresenter(
    private val view: MainContract.View,
    private val context: Context
) : MainContract.Presenter {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
    private val model = AuthModel(context, sharedPreferences)

    override fun isUserAuthenticated(): Boolean {
        return model.checkAuthentication()
    }

    override fun onRegistrationClicked() {
        view.checkPermissions()
        val intent = Intent(context, Registration::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    override fun onLoginClicked(email: String, password: String) {
        when {
            email.isEmpty() || password.isEmpty() -> {
                view.showToast("Заполните все поля!")
            }
            model.authenticateUser(email, password) -> {
                model.saveUserSession(email)
                view.redirectToHome()
            }
            else -> {
                view.showToast("Неверный email или пароль")
            }
        }
    }

    override fun handlePermissionsResult(requestCode: Int, grantResults: IntArray) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                view.showToast("Разрешение получено")
            } else {
                view.showToast("Разрешение отклонено")
            }
        }
    }

}