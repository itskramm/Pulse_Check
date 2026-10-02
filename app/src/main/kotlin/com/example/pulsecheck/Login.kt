package com.example.pulsecheck

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class Login : AppCompatActivity() {
    private lateinit var phone: EditText
    private lateinit var password: EditText
    private lateinit var session: SessionManager
    private lateinit var userDb: UserDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        session = SessionManager(this)
        userDb = UserDatabaseHelper(this)
        phone = findViewById(R.id.et_phone_number)
        password = findViewById(R.id.et_password)
        phone.setText(session.getUserPhone())
        findViewById<Button>(R.id.btn_sign_in).setOnClickListener { signIn() }
        findViewById<TextView>(R.id.tv_sign_up).setOnClickListener {
            startActivity(Intent(this, Registration::class.java))
        }
        findViewById<TextView>(R.id.tv_forget_password)?.setOnClickListener {
            Toast.makeText(this, "Password reset is not available yet.", Toast.LENGTH_SHORT).show()
        }
        findViewById<android.widget.ImageView>(R.id.btn_toggle_password)?.setOnClickListener {
            password.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
    }

    private fun signIn() {
        val phoneValue = phone.text.toString().trim()
        val passwordValue = password.text.toString()
        val name = userDb.loginUser(phoneValue, passwordValue)
        if (name == null) {
            Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show()
            return
        }
        session.setLoggedIn(true)
        session.saveUserName(name)
        session.saveUserPhone(phoneValue)
        startActivity(Intent(this, home::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
    }
}
