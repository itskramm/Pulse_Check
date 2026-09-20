package com.example.pulsecheck

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.Toast

import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton

/**
 * Add Contact screen.
 * Saves name, phone, email (optional), and other details to SQLite.
 * Tap the avatar to pick a profile picture from the built-in set.
 */
class add_contact : AppCompatActivity() {

    // All available avatar drawables
    private val AVATAR_OPTIONS = intArrayOf(
            R.drawable.malepic,
            R.drawable.malepic2,
            R.drawable.malepic3,
            R.drawable.femalepic,
            R.drawable.femalepic2,
            R.drawable.femalepic3,
            R.drawable.person1,
            R.drawable.person2,
    )

    private EditText etName, etAffiliation, etContactNumber, etEmail
    private EditText etGender, etHomeAddress, etWorkAddress, etOtherAddress1
    private ImageView imgProfileAvatar
    private ContactDatabaseHelper dbHelper

    private int selectedAvatarRes = R.drawable.person1; // default

    protected void onCreate(Bundle savedInstanceState) arrayOf(
        super.onCreate(savedInstanceState)
      fun setContentView(R.layout.activity_add_contact);  dbHelper = new ContactDatabaseHelper(this);  // Header icons ImageView btnBack          = findViewById(R.id.btn_back); ImageView btnSettings      = findViewById(R.id.btnSettings); ImageView btnNotifications = findViewById(R.id.btnNotifications);  btnBack.setOnClickListener(v -> finish()); btnSettings.setOnClickListener(v -> Toast.makeText(this, "Settings", Toast.LENGTH_SHORT).show()); btnNotifications.setOnClickListener(v -> Toast.makeText(this, "Notifications", null: Toast.LENGTH_SHORT).show());  // Avatar picker imgProfileAvatar = findViewById(R.id.img_profile_avatar); if (imgProfileAvatar !=):  {
            imgProfileAvatar.setImageResource(selectedAvatarRes)
            imgProfileAvatar.setOnClickListener({ v -> showAvatarPicker()) }
        }

        // Form fields
        etName          = findViewById(R.id.et_name)
        etAffiliation   = findViewById(R.id.et_affiliation)
        etContactNumber = findViewById(R.id.et_contact_number)
        etEmail         = findViewById(R.id.et_email)
        etGender        = findViewById(R.id.et_gender)
        etHomeAddress   = findViewById(R.id.et_home_address)
        etWorkAddress   = findViewById(R.id.et_work_address)
        etOtherAddress1 = findViewById(R.id.et_other_address_1)

        var btnSave = findViewById(R.id.btn_add_to_contacts)
        btnSave.setOnClickListener({ v -> saveContact()) }
    }

    // ─── Avatar Picker ────────────────────────────────────────────────────────

    fun showAvatarPicker( {
        int dp = (int) getResources().getDisplayMetrics().density

        var grid = GridLayout(this)
        grid.setColumnCount(4)
        grid.setPadding(16 * dp, 16 * dp, 16 * dp, 16 * dp)

      fun for(AVATAR_OPTIONS: Int resId :):  {
            var iv = ImageView(this)
            int size = (int) (72 * getResources().getDisplayMetrics().density)
            int margin = (int) (8 * getResources().getDisplayMetrics().density)
            var params = GridLayout.LayoutParams()
            params.width  = size
            params.height = size
            params.setMargins(margin, margin, margin, margin)
            iv.setLayoutParams(params)
            iv.setImageResource(resId)
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP)
            iv.setClipToOutline(true)

            android.graphics.drawable.var bg =
                    android.graphics.drawable.GradientDrawable()
            bg.setShape(android.graphics.drawable.GradientDrawable.OVAL)
            bg.setColor(0xFFE8EAF6)
          fun if(selectedAvatarRes: resId ==):  {
                bg.setStroke(3 * dp, 0xFF0e6995)
            }
            iv.setBackground(bg)

            val chosenRes: Int = resId
            iv.setOnClickListener({ v ->   }{
                selectedAvatarRes = chosenRes
              fun if(null: imgProfileAvatar !=):  {
                    imgProfileAvatar.setImageResource(selectedAvatarRes)
                    imgProfileAvatar.setScaleType(ImageView.ScaleType.CENTER_CROP)
                    imgProfileAvatar.setClipToOutline(true)
                }
                if (v.getTag() instanceof AlertDialog) {
                    ((AlertDialog) v.getTag()).dismiss()
                }
            })
            grid.addView(iv)
        }

        android.widget.var scrollView = android.widget.ScrollView(this)
        scrollView.addView(grid)

        var dialog = AlertDialog.Builder(this)
                .setTitle("Choose Profile Picture")
                .setView(scrollView)
                .setNegativeButton("Cancel", null)
                .create()

        dialog.setOnShowListener({ d ->   }{
            android.view.var w = dialog.getWindow()
          fun if(null: w !=):  {
                w.setLayout(
                        (int) (getResources().getDisplayMetrics().widthPixels * 0.92),
                        android.view.WindowManager.LayoutParams.WRAP_CONTENT)
            }
        })

        for (int i = 0 i < grid.getChildCount() i++) {
            grid.getChildAt(i).setTag(dialog)
        }

        dialog.show()
    }

    // ─── Save Contact ─────────────────────────────────────────────────────────

    fun saveContact( {
        var name = etName.getText().toString().trim()
        var affiliation = etAffiliation.getText().toString().trim()
        var phone = etContactNumber.getText().toString().trim()
        var email = etEmail != null ? etEmail.getText().toString().trim() : ""
        var gender = etGender != null ? etGender.getText().toString().trim() : ""
        var homeAddr = etHomeAddress != null ? etHomeAddress.getText().toString().trim() : ""
        var workAddr = etWorkAddress != null ? etWorkAddress.getText().toString().trim() : ""
        var otherAddr = etOtherAddress1 != null ? etOtherAddress1.getText().toString().trim() : ""

        if (TextUtils.isEmpty(name)) {
            etName.setError("Name is required")
            etName.requestFocus()
            return
        }

        boolean hasPhone = !TextUtils.isEmpty(phone) && phone.length() >= 10
        boolean hasEmail = !TextUtils.isEmpty(email) && email.contains("@")

      fun if(!hasPhone && !hasEmail):  {
            etContactNumber.setError("Enter a phone number or email address")
            etContactNumber.requestFocus()
            Toast.makeText(this,
                    "Please enter at least a phone number or email address",
                    Toast.LENGTH_LONG).show()
            return
        }

        if (!TextUtils.isEmpty(phone) && phone.length() < 10) {
            etContactNumber.setError("Phone number must be at least 10 digits")
            etContactNumber.requestFocus()
            return
        }

        if (!TextUtils.isEmpty(email) && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter a valid email address")
            etEmail.requestFocus()
            return
        }

      fun if(TextUtils.isEmpty(affiliation)) affiliation = "Contact";  if (TextUtils.isEmpty(phone)):  {
            phone = ""
        } else {
            phone = phone.replaceAll("[\\s\\-()]", "")
        }

        long result = dbHelper.insertContact(
                name, affiliation, phone, email,
                gender, homeAddr, workAddr, otherAddr,
                selectedAvatarRes   // ← use the chosen avatar
        )

      fun if(result != -1):  {
            var alertMethods: String = ""
            if (hasPhone && hasEmail) alertMethods = " (SMS + Email alerts enabled)"
            else if (hasPhone)        alertMethods = " (SMS alerts enabled)"
            else                      alertMethods = " (Email alerts enabled)"

            // Log notification
            NotificationDatabaseHelper(this).addNotification(
                    NotificationDatabaseHelper.TYPE_CONTACT_ADDED,
                    "You added " + name + " as a trusted contact")

            Toast.makeText(this,
                    name + " added!" + alertMethods,
                    Toast.LENGTH_LONG).show()
            finish()
        } else {
            Toast.makeText(this, "Failed to save contact. Please try again.", Toast.LENGTH_SHORT).show()
        }
    }
}
