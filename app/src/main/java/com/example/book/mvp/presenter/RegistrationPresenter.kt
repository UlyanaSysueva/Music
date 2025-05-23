package com.example.book.mvp.presenter

import android.content.Context
import com.example.book.mvp.contract.RegistrationContract
import com.example.book.mvp.model.RegistrationModel

class RegistrationPresenter(
    private val view: RegistrationContract.View,
    private val context: Context
) : RegistrationContract.Presenter {

    private val model = RegistrationModel(context)

    override fun handleRegistration(email: String, password: String) {
        when {
            email.isBlank() || password.isBlank() -> {
                view.showToast("Заполните все поля!")
            }
            model.registerUser(email, password) -> {
                view.showToast("Добро пожаловать в семью!")
                view.clearInputFields()
                view.navigateToAuth()
            }
            else -> {
                view.showToast("Ошибка регистрации")
            }
        }
    }

    override fun handleAuthLinkClick() {
        view.navigateToAuth()
    }

}