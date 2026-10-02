package com.example.pulsecheck

import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class add_contact : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_contact)
        val db = ContactDatabaseHelper(this)
        val name = findViewById<EditText>(R.id.et_name)
        val affiliation = findViewById<EditText>(R.id.et_affiliation)
        val phone = findViewById<EditText>(R.id.et_contact_number)
        val gender = findViewById<EditText>(R.id.et_gender)
        val email = findViewById<EditText>(R.id.et_email)
        val homeAddress = findViewById<EditText>(R.id.et_home_address)
        val workAddress = findViewById<EditText>(R.id.et_work_address)
        val otherAddress = findViewById<EditText>(R.id.et_other_address_1)
        findViewById<TextView>(R.id.btn_add_to_contacts).setOnClickListener {
            if (name.text.toString().trim().isEmpty() || phone.text.toString().trim().isEmpty()) {
                Toast.makeText(this, "Name and phone are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            db.insertContact(
                name.text.toString().trim(),
                affiliation.text.toString().trim(),
                phone.text.toString().trim(),
                email.text.toString().trim(),
                gender.text.toString().trim(),
                homeAddress.text.toString().trim(),
                workAddress.text.toString().trim(),
                otherAddress.text.toString().trim(),
                R.drawable.person1
            )
            Toast.makeText(this, "Contact added", Toast.LENGTH_SHORT).show()
            finish()
        }
        findViewById<View>(R.id.btn_back)?.setOnClickListener { finish() }
    }
}
