package com.example.book.mvp.contract

interface RegistrationContract {
    interface View {
        fun showToast(message: String)
        fun navigateToAuth()
        fun clearInputFields()
    }

    interface Presenter {
        fun handleRegistration(email: String, password: String)
        fun handleAuthLinkClick()
    }
}