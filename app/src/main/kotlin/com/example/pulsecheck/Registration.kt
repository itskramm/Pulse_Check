package com.example.pulsecheck

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class Registration : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)
        val session = SessionManager(this)
        val userDb = UserDatabaseHelper(this)
        val name = findViewById<EditText>(R.id.et_full_name)
        val phone = findViewById<EditText>(R.id.et_phone_number)
        val email = findViewById<EditText>(R.id.et_email)
        val password = findViewById<EditText>(R.id.et_password)

        findViewById<Button>(R.id.btn_register).setOnClickListener {
            val fullName = name.text.toString().trim()
            val phoneValue = phone.text.toString().trim()
            val emailValue = email.text.toString().trim()
            val passwordValue = password.text.toString()
            if (fullName.isEmpty() || phoneValue.isEmpty() || emailValue.isEmpty() || passwordValue.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            } else if (userDb.registerUser(fullName, phoneValue, emailValue, passwordValue)) {
                session.saveUserPhone(phoneValue)
                session.saveUserEmail(emailValue)
                Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Phone number is already registered", Toast.LENGTH_SHORT).show()
            }
        }
        findViewById<TextView>(R.id.tv_sign_in_link)?.setOnClickListener { finish() }
        findViewById<android.widget.ImageView>(R.id.btn_back)?.setOnClickListener { finish() }
    }
}
