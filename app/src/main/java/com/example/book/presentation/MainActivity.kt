package com.example.book.presentation

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.book.R
import com.example.book.data.DbUsers
import com.example.book.presentation.Home
import com.example.book.presentation.Registration

class MainActivity : AppCompatActivity() {
    private lateinit var sharedPreferences: SharedPreferences
    private val PERMISSION_REQUEST_CODE = 101
    private val PREF_FIRST_RUN = "first_run"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = getSharedPreferences("user_session", MODE_PRIVATE)

        if (isUserAuthenticated()) {
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

        if (sharedPreferences.contains("EMAIL_KEY")) {
            val intent = Intent(this, Home::class.java)
            intent.putExtra("EMAIL_KEY", sharedPreferences.getString("EMAIL_KEY", ""))
            startActivity(intent)
            finish()
            return
        }

        val isFirstRun = sharedPreferences.getBoolean(PREF_FIRST_RUN, true)
        if (isFirstRun) {
            checkPermissions()
            with(sharedPreferences.edit()) {
                putBoolean(PREF_FIRST_RUN, false)
                apply()
            }
        }

        setupUI()
    }

    private fun isUserAuthenticated(): Boolean {
        val email = sharedPreferences.getString("EMAIL_KEY", null) ?: return false
        val db = DbUsers(this, null)
        return db.checkUserExists(email)
    }

    private fun redirectToHome() {
        val intent = Intent(this, Home::class.java)
        startActivity(intent)
        finish()
    }

    private fun setupUI() {
        val linkReg: Button = findViewById(R.id.regist)
        val userEmail: EditText = findViewById(R.id.email)
        val userPass: EditText = findViewById(R.id.pass)
        val buttonEnter: Button = findViewById(R.id.enter)

        linkReg.setOnClickListener {
            startActivity(Intent(this, Registration::class.java))
        }

        buttonEnter.setOnClickListener {
            val email = userEmail.text.toString().trim()
            val pass = userPass.text.toString().trim()

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Заполните все поля!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val db = DbUsers(this, null)
            if (db.getUser(email, pass)) {
                sharedPreferences.edit().apply {
                    putString("EMAIL_KEY", email)
                    apply()
                }
                startActivity(Intent(this, Home::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                })
                finish()
            } else {
                Toast.makeText(this, "Неверный email или пароль", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun checkPermissions() {
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
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Разрешение получено", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Разрешение отклонено", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
} 