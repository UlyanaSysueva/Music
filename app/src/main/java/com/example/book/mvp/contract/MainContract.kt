package com.example.book.mvp.contract

interface MainContract {
    interface View {
        fun redirectToHome()
        fun showToast(message: String)
        fun checkPermissions()
    }

    interface Presenter {
        companion object {
            const val PERMISSION_REQUEST_CODE = 101
            const val PREF_FIRST_RUN = "first_run"
        }

        fun isUserAuthenticated(): Boolean
        fun onRegistrationClicked()
        fun onLoginClicked(email: String, password: String)
        fun handlePermissionsResult(requestCode: Int, grantResults: IntArray)
    }
}