package com.example.book

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class Registration : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_registration)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val linkAuth: Button = findViewById(R.id.enter)
        val userEmail: EditText = findViewById(R.id.email)
        val userPass: EditText = findViewById(R.id.pass)
        val buttonReg: Button = findViewById(R.id.regist)
        linkAuth.setOnClickListener{
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
        buttonReg.setOnClickListener{
            val email = userEmail.text.toString().trim()
            val pass = userPass.text.toString().trim()

            if(email == "" || pass == "")
                Toast.makeText(this, "Заполните все поля!", Toast.LENGTH_LONG).show()
            else{
                val user = User(email, pass)

                val db = DbUsers(this, null)
                db.addUser(user)
                Toast.makeText(this, "Добро пожаловать в семью!", Toast.LENGTH_LONG).show()

                userEmail.text.clear()
                userPass.text.clear()

                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            }
        }
    }
}