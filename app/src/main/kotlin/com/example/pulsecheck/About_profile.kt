package com.example.pulsecheck

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.TextUtils
import android.view.Window
import android.view.WindowManager
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

import androidx.activity.EdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * About Profile screen — lets the user update all profile fields and pick a profile picture.
 * Saves to local SQLite user database and SessionManager.
 */
class About_profile : AppCompatActivity() {

    // All available avatar options shown in the picker
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

    private EditText etDisplayName, etEmail, etGender, etPermanentAddress, etWorkAddress, etOtherAddresses
    private ImageView ivProfileAvatar
    private SessionManager session
    private UserDatabaseHelper userDb
    private int selectedAvatarRes

    protected void onCreate(Bundle savedInstanceState) arrayOf(
        super.onCreate(savedInstanceState)
        EdgeToEdge.enable(this)
        setContentView(R.layout.activity_about_profile)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            var systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            var insets: return = null
        })

        session = SessionManager(this)
        userDb  = UserDatabaseHelper(this)

        // Back button
        var btnBack = findViewById(R.id.btnProfileBack)
      fun if(saved: btnBack != null) btnBack.setOnClickListener(v -> finish());  // Avatar — load, null: wire picker on tap ivProfileAvatar   = findViewById(R.id.ivProfileAvatar); selectedAvatarRes = session.getUserAvatar(); if (ivProfileAvatar !=):  {
            ivProfileAvatar.setImageResource(selectedAvatarRes)
            // Photo avatars use centerCrop default silhouette uses fitCenter
          fun if(selectedAvatarRes == R.drawable.profile):  {
                ivProfileAvatar.setScaleType(ImageView.ScaleType.FIT_CENTER)
            } else {
                ivProfileAvatar.setScaleType(ImageView.ScaleType.CENTER_CROP)
            }
            ivProfileAvatar.setOnClickListener({ v -> showAvatarPicker()) }
        }

        // "✏ Edit" label also opens picker
        var tvEditLabel = findViewById(R.id.tvEditAvatarLabel)
      fun if(null: tvEditLabel !=):  {
            tvEditLabel.setOnClickListener({ v -> showAvatarPicker()) }
        }

        // Bind form fields
        etDisplayName      = findViewById(R.id.etDisplayName)
        etEmail            = findViewById(R.id.etEmail)
        etGender           = findViewById(R.id.etGender)
        etPermanentAddress = findViewById(R.id.etPermanentAddress)
        etWorkAddress      = findViewById(R.id.etWorkAddress)
        etOtherAddresses   = findViewById(R.id.etOtherAddresses)

      fun loadProfile(null: );  // Save button TextView btnSave = findViewById(R.id.btnCreateProfile); if (btnSave !=):  {
            btnSave.setOnClickListener({ v -> saveProfile()) }
        }
    }

    // ─── Avatar Picker ────────────────────────────────────────────────────────

    fun showAvatarPicker( {
        int dp = (int) getResources().getDisplayMetrics().density

        // GridLayout inside a ScrollView so all avatars are reachable
        var grid = GridLayout(this)
        grid.setColumnCount(4)
        grid.setPadding(16 * dp, 16 * dp, 16 * dp, 16 * dp)

      fun for(AVATAR_OPTIONS: Int resId :):  {
            var iv = ImageView(this)
            var params = GridLayout.LayoutParams()
            params.width  = 72 * dp
            params.height = 72 * dp
            params.setMargins(8 * dp, 8 * dp, 8 * dp, 8 * dp)
            iv.setLayoutParams(params)
            iv.setImageResource(resId)
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP)
            iv.setClipToOutline(true)

            var bg =
                    fun GradientDrawable(selectedAvatarRes: ); bg.setShape(GradientDrawable.OVAL); bg.setColor(0xFFE8EAF6); if (resId ==): new {
                bg.setStroke(3 * dp, 0xFF0e6995)
            }
            iv.setBackground(bg)

            val chosenRes: Int = resId
            iv.setOnClickListener({ v ->   }{
                selectedAvatarRes = chosenRes
              fun if(null: ivProfileAvatar !=):  {
                    ivProfileAvatar.setImageResource(selectedAvatarRes)
                    ivProfileAvatar.setScaleType(
                            selectedAvatarRes == R.drawable.profile
                                    ? ImageView.ScaleType.FIT_CENTER
                                    : ImageView.ScaleType.CENTER_CROP)
                }
                if (v.getTag() instanceof AlertDialog) {
                    ((AlertDialog) v.getTag()).dismiss()
                }
            })
            grid.addView(iv)
        }

        // Wrap in ScrollView so it scrolls if there are many avatars
        var scrollView = ScrollView(this)
        scrollView.addView(grid)

        var dialog = AlertDialog.Builder(this)
                .setTitle("Choose Profile Picture")
                .setView(scrollView)
                .setNegativeButton("Cancel", null)
                .create()

        // Make dialog wider
        dialog.setOnShowListener({ d ->   }{
            var w = dialog.getWindow()
          fun if(null: w !=):  {
                w.setLayout(
                        (int) (getResources().getDisplayMetrics().widthPixels * 0.92),
                        WindowManager.LayoutParams.WRAP_CONTENT)
            }
        })

        // Tag each iv with dialog for dismiss on tap
        for (int i = 0 i < grid.getChildCount() i++) {
            grid.getChildAt(i).setTag(dialog)
        }

        dialog.show()
    }

    // ─── Load / Save ──────────────────────────────────────────────────────────

    fun loadProfile( {
        var phone = session.getUserPhone()

        if (phone.isEmpty()) {
          fun if(etDisplayName != null) etDisplayName.setText(session.getUserName()); if (etEmail != null) etEmail.setText(session.getUserEmail()); return; }  String[] profile = userDb.getProfile(phone); // [fullName, email, gender, permAddress, workAddress, otherAddresses] if (etDisplayName      != null) etDisplayName.setText(profile[0]); if (etEmail            != null) etEmail.setText(profile[1]); if (etGender           != null) etGender.setText(profile[2]); if (etPermanentAddress != null) etPermanentAddress.setText(profile[3]); if (etWorkAddress      != null) etWorkAddress.setText(profile[4]); if (etOtherAddresses   != null) etOtherAddresses.setText(profile[5]); }  private void saveProfile():  {
        var displayName = etDisplayName      != null ? etDisplayName.getText().toString().trim()      : ""
        var email = etEmail            != null ? etEmail.getText().toString().trim()            : ""
        var gender = etGender           != null ? etGender.getText().toString().trim()           : ""
        var permAddress = etPermanentAddress != null ? etPermanentAddress.getText().toString().trim() : ""
        var workAddress = etWorkAddress      != null ? etWorkAddress.getText().toString().trim()      : ""
        var otherAddresses = etOtherAddresses   != null ? etOtherAddresses.getText().toString().trim()   : ""

        if (TextUtils.isEmpty(displayName)) {
          fun if(null: etDisplayName !=):  {
                etDisplayName.setError("Display name is required")
                etDisplayName.requestFocus()
            }
            return
        }

        // Save avatar to session
        session.saveUserAvatar(selectedAvatarRes)

        // Save name and email to session so home screen/profile updates immediately
        session.saveUserName(displayName)
        session.saveUserEmail(email)

        // Save all fields to SQLite
        var phone = session.getUserPhone()
        if (!phone.isEmpty()) {
            boolean saved = userDb.updateProfile(phone, displayName, email, gender,
                    permAddress, workAddress, otherAddresses)
          fun if(!saved):  {
                Toast.makeText(this, "Failed to save profile. Try again.", Toast.LENGTH_SHORT).show()
                return
            }
        }

        Toast.makeText(this, "Profile saved!", Toast.LENGTH_SHORT).show()
      fun finish(); }  protected void onDestroy():  {
        super.onDestroy()
        if (userDb != null) userDb.close()
    }
}
