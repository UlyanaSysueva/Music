package com.example.book.mvp.view

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.book.R
import com.example.book.mvp.contract.MainContract
import com.example.book.mvp.contract.MainContract.Presenter.Companion.PERMISSION_REQUEST_CODE
import com.example.book.mvp.presenter.MainPresenter

class MainActivity : AppCompatActivity(), MainContract.View {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var presenter: MainContract.Presenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = getSharedPreferences("user_session", MODE_PRIVATE)

        presenter = MainPresenter(this, this)

        if (presenter.isUserAuthenticated()) {
            redirectToHome()
            return
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupUI()
    }

    private fun setupUI() {
        val linkReg: Button = findViewById(R.id.regist)
        val userEmail: EditText = findViewById(R.id.email)
        val userPass: EditText = findViewById(R.id.pass)
        val buttonEnter: Button = findViewById(R.id.enter)

        linkReg.setOnClickListener { presenter.onRegistrationClicked() }
        buttonEnter.setOnClickListener {
            presenter.onLoginClicked(
                userEmail.text.toString().trim(),
                userPass.text.toString().trim()
            )
        }
    }

    override fun redirectToHome() {
        startActivity(Intent(this, Home::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        })
        finish()
    }

    override fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun checkPermissions() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        presenter.handlePermissionsResult(requestCode, grantResults)
    }
} 