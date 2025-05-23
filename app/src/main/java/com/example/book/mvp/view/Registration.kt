package com.example.book.mvp.view

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.book.R
import com.example.book.data.DbUsers
import com.example.book.mvp.contract.RegistrationContract
import com.example.book.mvp.model.User
import com.example.book.mvp.presenter.RegistrationPresenter

class Registration : AppCompatActivity(), RegistrationContract.View {

    private lateinit var presenter: RegistrationContract.Presenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_registration)
        setupWindowInsets()

        presenter = RegistrationPresenter(this, applicationContext)
        setupUI()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupUI() {
        val userEmail: EditText = findViewById(R.id.email)
        val userPass: EditText = findViewById(R.id.pass)

        findViewById<Button>(R.id.enter).setOnClickListener { presenter.handleAuthLinkClick() }
        findViewById<Button>(R.id.regist).setOnClickListener {
            presenter.handleRegistration(
                userEmail.text.toString().trim(),
                userPass.text.toString().trim()
            )
        }
    }

    override fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun navigateToAuth() {
        startActivity(Intent(this, MainActivity::class.java))
    }

    override fun clearInputFields() {
        findViewById<EditText>(R.id.email).text.clear()
        findViewById<EditText>(R.id.pass).text.clear()
    }

} 