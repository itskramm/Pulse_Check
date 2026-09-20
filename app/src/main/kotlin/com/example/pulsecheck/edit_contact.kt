package com.example.pulsecheck

import android.os.Bundle
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton

class edit_contact : AppCompatActivity() {
    private lateinit var dbHelper: ContactDatabaseHelper
    private var contactId = -1
    private var selectedAvatarRes = R.drawable.person1
    private var imgProfileAvatar: ImageView? = null
    private var etName: EditText? = null
    private var etAffiliation: EditText? = null
    private var etContactNumber: EditText? = null
    private var etEmail: EditText? = null
    private var etGender: EditText? = null
    private var etHomeAddress: EditText? = null
    private var etWorkAddress: EditText? = null
    private var etOtherAddress1: EditText? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_contact)
        dbHelper = ContactDatabaseHelper(this)
        contactId = intent.getIntExtra(EXTRA_CONTACT_ID, -1)
        if (contactId == -1) {
            Toast.makeText(this, "Error: contact not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        findViewById<ImageView>(R.id.btn_back)?.setOnClickListener { finish() }
        imgProfileAvatar = findViewById(R.id.img_profile_avatar)
        imgProfileAvatar?.setOnClickListener { showAvatarPicker() }
        etName = findViewById(R.id.et_name)
        etAffiliation = findViewById(R.id.et_affiliation)
        etContactNumber = findViewById(R.id.et_contact_number)
        etEmail = findViewById(R.id.et_email)
        etGender = findViewById(R.id.et_gender)
        etHomeAddress = findViewById(R.id.et_home_address)
        etWorkAddress = findViewById(R.id.et_work_address)
        etOtherAddress1 = findViewById(R.id.et_other_address_1)
        findViewById<AppCompatButton>(R.id.btn_add_to_contacts)?.apply {
            text = "Save Changes"
            setOnClickListener { saveChanges() }
        }
        findViewById<android.widget.TextView>(R.id.tv_title)?.text = "Edit Contact"
        loadContact()
    }

    private fun loadContact() {
        val data = dbHelper.getContactById(contactId)
        if (data == null) {
            Toast.makeText(this, "Contact not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        listOf(etName, etAffiliation, etContactNumber, etEmail, etGender, etHomeAddress, etWorkAddress, etOtherAddress1)
            .zip(data.drop(1))
            .forEach { (field, value) -> field?.setText(value) }
        selectedAvatarRes = data[9].toIntOrNull() ?: R.drawable.person1
        if (selectedAvatarRes == 0) selectedAvatarRes = R.drawable.person1
        imgProfileAvatar?.apply {
            setImageResource(selectedAvatarRes)
            scaleType = ImageView.ScaleType.CENTER_CROP
            clipToOutline = true
        }
    }

    private fun saveChanges() {
        val name = etName?.text?.toString()?.trim().orEmpty()
        var affiliation = etAffiliation?.text?.toString()?.trim().orEmpty()
        var phone = etContactNumber?.text?.toString()?.trim().orEmpty()
        val email = etEmail?.text?.toString()?.trim().orEmpty()
        val gender = etGender?.text?.toString()?.trim().orEmpty()
        val home = etHomeAddress?.text?.toString()?.trim().orEmpty()
        val work = etWorkAddress?.text?.toString()?.trim().orEmpty()
        val other = etOtherAddress1?.text?.toString()?.trim().orEmpty()
        if (name.isEmpty()) {
            etName?.error = "Name is required"
            etName?.requestFocus()
            return
        }
        val hasPhone = phone.length >= 10
        val hasEmail = email.isNotEmpty() && email.contains("@")
        if (!hasPhone && !hasEmail) {
            etContactNumber?.error = "Enter a phone number or email address"
            etContactNumber?.requestFocus()
            Toast.makeText(this, "Please enter at least a phone number or email address", Toast.LENGTH_LONG).show()
            return
        }
        if (phone.isNotEmpty() && phone.length < 10) {
            etContactNumber?.error = "Phone number must be at least 10 digits"
            etContactNumber?.requestFocus()
            return
        }
        if (email.isNotEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail?.error = "Enter a valid email address"
            etEmail?.requestFocus()
            return
        }
        if (affiliation.isEmpty()) affiliation = "Contact"
        phone = phone.replace(Regex("[\\s\\-()]"), "")
        val rows = dbHelper.updateContact(contactId, name, affiliation, phone, email, gender, home, work, other, selectedAvatarRes)
        if (rows > 0) {
            Toast.makeText(this, "$name updated!", Toast.LENGTH_SHORT).show()
            finish()
        } else Toast.makeText(this, "Failed to update. Try again.", Toast.LENGTH_SHORT).show()
    }

    private fun showAvatarPicker() {
        val dp = resources.displayMetrics.density.toInt()
        val grid = GridLayout(this).apply {
            columnCount = 4
            setPadding(16 * dp, 16 * dp, 16 * dp, 16 * dp)
        }
        AVATAR_OPTIONS.forEach { resource ->
            val image = ImageView(this).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 72 * dp
                    height = 72 * dp
                    setMargins(8 * dp, 8 * dp, 8 * dp, 8 * dp)
                }
                setImageResource(resource)
                scaleType = ImageView.ScaleType.CENTER_CROP
                clipToOutline = true
                background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(0xFFE8EAF6.toInt())
                    if (resource == selectedAvatarRes) setStroke(3 * dp, 0xFF0e6995.toInt())
                }
            }
            image.setOnClickListener {
                selectedAvatarRes = resource
                imgProfileAvatar?.setImageResource(resource)
                (it.tag as? AlertDialog)?.dismiss()
            }
            grid.addView(image)
        }
        val scrollView = android.widget.ScrollView(this).apply { addView(grid) }
        val dialog = AlertDialog.Builder(this).setTitle("Choose Profile Picture").setView(scrollView)
            .setNegativeButton("Cancel", null).create()
        dialog.setOnShowListener {
            dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.92).toInt(),
                android.view.WindowManager.LayoutParams.WRAP_CONTENT)
        }
        for (i in 0 until grid.childCount) grid.getChildAt(i).tag = dialog
        dialog.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::dbHelper.isInitialized) dbHelper.close()
    }

    companion object {
        const val EXTRA_CONTACT_ID = "contact_id"
        private val AVATAR_OPTIONS = intArrayOf(
            R.drawable.malepic, R.drawable.malepic2, R.drawable.malepic3,
            R.drawable.femalepic, R.drawable.femalepic2, R.drawable.femalepic3,
            R.drawable.person1, R.drawable.person2
        )
    }
}
