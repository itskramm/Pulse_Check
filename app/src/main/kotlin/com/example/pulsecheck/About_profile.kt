package com.example.pulsecheck

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class About_profile : AppCompatActivity() {
    private lateinit var session: SessionManager
    private lateinit var userDb: UserDatabaseHelper
    private lateinit var name: EditText
    private lateinit var email: EditText
    private lateinit var gender: EditText
    private lateinit var permanentAddress: EditText
    private lateinit var workAddress: EditText
    private lateinit var otherAddress: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about_profile)
        session = SessionManager(this)
        userDb = UserDatabaseHelper(this)

        findViewById<ImageView>(R.id.btnProfileBack)?.setOnClickListener { finish() }
        findViewById<ImageView>(R.id.ivProfileAvatar)?.setImageResource(session.getUserAvatar())
        name = findViewById(R.id.etDisplayName)
        email = findViewById(R.id.etEmail)
        gender = findViewById(R.id.etGender)
        permanentAddress = findViewById(R.id.etPermanentAddress)
        workAddress = findViewById(R.id.etWorkAddress)
        otherAddress = findViewById(R.id.etOtherAddresses)
        loadProfile()
        findViewById<TextView>(R.id.btnCreateProfile)?.setOnClickListener { saveProfile() }
    }

    private fun loadProfile() {
        val profile = userDb.getProfile(session.getUserPhone())
        name.setText(profile[0].ifEmpty { session.getUserName() })
        email.setText(profile[1].ifEmpty { session.getUserEmail() })
        gender.setText(profile[2])
        permanentAddress.setText(profile[3])
        workAddress.setText(profile[4])
        otherAddress.setText(profile[5])
    }

    private fun saveProfile() {
        val displayName = name.text.toString().trim()
        if (displayName.isEmpty()) {
            name.error = "Display name is required"
            return
        }
        userDb.updateProfile(
            session.getUserPhone(),
            displayName,
            email.text.toString().trim(),
            gender.text.toString().trim(),
            permanentAddress.text.toString().trim(),
            workAddress.text.toString().trim(),
            otherAddress.text.toString().trim()
        )
        session.saveUserName(displayName)
        session.saveUserEmail(email.text.toString().trim())
        Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show()
        finish()
    }
}
