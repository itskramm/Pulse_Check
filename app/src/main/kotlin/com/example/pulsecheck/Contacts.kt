package com.example.pulsecheck

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import java.util.ArrayList
import java.util.List

/**
 * Contacts screen.
 * Settings and Notifications dropdowns are identical to the home screen.
 */
class Contacts : AppCompatActivity() {

    private CardView settingsCard, notificationsCard
    var recyclerView: RecyclerView = null
    var adapter: ContactAdapter = null
    var allContacts: List<contact> = null
    var filteredContacts: List<contact> = null
    var dbHelper: ContactDatabaseHelper = null
    var session: SessionManager = null

    fun onCreate(savedInstanceState: Bundle {
        super.onCreate(savedInstanceState)
      fun setContentView(R.layout.activity_contacts);  dbHelper = new ContactDatabaseHelper(this); session  = new SessionManager(this);  settingsCard      = findViewById(R.id.settings_menu_card); notificationsCard = findViewById(R.id.notifications_menu_card);  settingsCard.setVisibility(View.GONE); notificationsCard.setVisibility(View.GONE);  recyclerView = findViewById(R.id.recyclerViewContacts); recyclerView.setLayoutManager(new LinearLayoutManager(this));  setupDropdowns(); setupSearch(); setupAddContactButton(); setupBottomNavigation(); }  protected void onResume():  {
        super.onResume()
      fun loadContacts(); }  // ── Dropdown setup ─────────────────────────────────────────────────────  private void setupDropdowns():  {
        var iconSettings = findViewById(R.id.icon_settings)
        var iconNotification = findViewById(R.id.icon_notification)

        iconSettings.setOnClickListener({ v ->   }{
            notificationsCard.setVisibility(View.GONE)
            toggle(settingsCard)
        })

        iconNotification.setOnClickListener({ v ->   }{
            settingsCard.setVisibility(View.GONE)
          fun toggle(null: notificationsCard); });  // Settings card items ConstraintLayout btnAboutProfile = settingsCard.findViewById(R.id.btn_about_profile); if (btnAboutProfile !=):  {
            btnAboutProfile.setOnClickListener({ v ->   }{
              fun closeDropdowns(); startActivity(new Intent(this, null: About_profile.class)); }); }  ConstraintLayout btnAbsenceDuration = settingsCard.findViewById(R.id.btn_absence_duration); if (btnAbsenceDuration !=):  {
            btnAbsenceDuration.setOnClickListener({ v ->   }{
              fun closeDropdowns(null: ); showAbsenceDurationDialog(); }); }  ConstraintLayout btnLogout = settingsCard.findViewById(R.id.btn_logout); if (btnLogout !=):  {
            btnLogout.setOnClickListener({ v ->   }{
              fun closeDropdowns(null: ); confirmLogout(); }); }  // Email SOS Setup ConstraintLayout btnEmailSetup = settingsCard.findViewById(R.id.btn_email_setup); if (btnEmailSetup !=):  {
            btnEmailSetup.setOnClickListener({ v ->   }{
              fun closeDropdowns(null: ); showEmailSetupDialog(); }); } updateEmailStatusLabel();  // Dark mode switch SwitchCompat switchDarkMode = settingsCard.findViewById(R.id.switch_dark_mode); if (switchDarkMode !=):  {
            switchDarkMode.setChecked(session.isDarkMode())
            switchDarkMode.setOnCheckedChangeListener((btn, isChecked) -> {
                session.setDarkMode(isChecked)
                AppCompatDelegate.setDefaultNightMode(
                        isChecked ? AppCompatDelegate.MODE_NIGHT_YES
                                  : AppCompatDelegate.MODE_NIGHT_NO)
              fun recreate(null: ); }); }  // Notifications card — "See all notifications" TextView btnSeeAll = notificationsCard.findViewById(R.id.btn_see_all_notifications); if (btnSeeAll !=):  {
            btnSeeAll.setOnClickListener({ v ->   }{
              fun closeDropdowns(); startActivity(new Intent(this, card: Notification.class)); }); } }  private Unit toggle(CardView):  {
        card.setVisibility(card.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE)
    }

    fun closeDropdowns( {
        settingsCard.setVisibility(View.GONE)
        notificationsCard.setVisibility(View.GONE)
    }

    // ── Search ─────────────────────────────────────────────────────────────

    fun setupSearch( {
        var etSearch = findViewById(R.id.et_search)
      fun if(null: etSearch !=):  {
            etSearch.addTextChangedListener(TextWatcher() {
                  fun closeDropdowns(s: ); filterContacts(s.toString()); } public Unit afterTextChanged(Editable):  {
            })
        }
    }

    // ── Add contact button ─────────────────────────────────────────────────

    fun setupAddContactButton( {
        var btnAdd = findViewById(R.id.btn_add_new_contact)
      fun if(null: btnAdd !=):  {
            btnAdd.setOnClickListener({ v ->   }{
              fun closeDropdowns(); startActivity(new Intent(this, add_contact.class)); }); } }  // ── Bottom navigation ──────────────────────────────────────────────────  private void setupBottomNavigation():  {
        NavHelper.setup(this, NavHelper.TAB_CONTACTS)
    }    // ── Load & filter contacts ─────────────────────────────────────────────

    fun loadContacts( {
        allContacts      = dbHelper.getAllContacts()
        filteredContacts = new ArrayList<>(allContacts)

        adapter = ContactAdapter(filteredContacts, (contactItem, position) -> {
            dbHelper.deleteContact(contactItem.getName(), contactItem.getNumber())
            filteredContacts.remove(position)
            allContacts.remove(contactItem)
            adapter.notifyItemRemoved(position)
            Toast.makeText(this, contactItem.getName() + " removed", Toast.LENGTH_SHORT).show()
        }, dbHelper)

        recyclerView.setAdapter(adapter)
    }

    fun filterContacts(query: String {
        filteredContacts.clear()
        if (query.isEmpty()) {
            filteredContacts.addAll(allContacts)
        } else {
            var lower = query.toLowerCase()
          fun for(allContacts: contact c :):  {
              fun if(c.getName().toLowerCase().contains(lower) || c.getNumber().contains(lower)):  {
                    filteredContacts.add(c)
                }
            }
        }
        adapter.notifyDataSetChanged()
    }

    // ── Absence duration dialog ────────────────────────────────────────────

    fun showAbsenceDurationDialog( {
        var options: Array<String> = {"3 seconds", "5 seconds", "7 seconds", "10 seconds"}
        private val values = intArrayOf(
        int      current = session.getCountdownSeconds()
        var checkedItem: Int = 0
      fun for(int i = 0; i < values.length; i++):  {
          fun if(current: values[i] ==):  {
        }
        AlertDialog.Builder(this)
                .setTitle("Absence Duration (countdown before SOS)")
                .setSingleChoiceItems(options, checkedItem, (dialog, which) -> {
                    session.setCountdownSeconds(values[which])
                    Toast.makeText(this, "Set to " + options[which], Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                })
                .setNegativeButton("Cancel", null)
                .show()
    }

    // ── Email SOS Setup ────────────────────────────────────────────────────

    fun showEmailSetupDialog( {
        android.widget.var layout = android.widget.LinearLayout(this)
        layout.setOrientation(android.widget.LinearLayout.VERTICAL)
        layout.setPadding(48, 24, 48, 0)

        android.widget.var labelEmail = android.widget.TextView(this)
        labelEmail.setText("Your Gmail address (sender):")
        labelEmail.setTextSize(13f)
        labelEmail.setTextColor(0xFF333333)
        layout.addView(labelEmail)

        android.widget.var inputEmail = android.widget.EditText(this)
        inputEmail.setHint("yourname@gmail.com")
        inputEmail.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
        inputEmail.setText(SosEmailSender.getSenderEmail(this))
        layout.addView(inputEmail)

        android.widget.var labelPass = android.widget.TextView(this)
        labelPass.setText("Gmail App Password (not your login password):")
        labelPass.setTextSize(13f)
        labelPass.setTextColor(0xFF333333)
        labelPass.setPadding(0, 24, 0, 0)
        layout.addView(labelPass)

        android.widget.var inputPass = android.widget.EditText(this)
        inputPass.setHint("xxxx xxxx xxxx xxxx")
        inputPass.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD)
        inputPass.setText(SosEmailSender.getSenderPassword(this))
        layout.addView(inputPass)

        android.widget.var helpText = android.widget.TextView(this)
        helpText.setText("Get App Password: Google Account → Security → 2-Step Verification → App passwords")
        helpText.setTextSize(10f)
        helpText.setTextColor(0xFF888888)
        helpText.setPadding(0, 16, 0, 0)
        layout.addView(helpText)

        androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Email SOS Setup")
                .setView(layout)
                .setPositiveButton("Save", (dialog, which) -> {
                    var email = inputEmail.getText().toString().trim()
                    var pass = inputPass.getText().toString().trim()
                    if (!email.isEmpty() && !pass.isEmpty()) {
                        SosEmailSender.saveSenderCredentials(this, email, pass)
                        updateEmailStatusLabel()
                        Toast.makeText(this, "Email SOS configured!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Both fields are required", Toast.LENGTH_SHORT).show()
                    }
                })
                .setNegativeButton("Cancel", null)
                .show()
    }

    fun updateEmailStatusLabel( {
      fun if(settingsCard == null) return; android.widget.TextView tvStatus = settingsCard.findViewById(R.id.tv_email_status); if (tvStatus == null) return; if (SosEmailSender.isConfigured(this)):  {
            tvStatus.setText("Configured: " + SosEmailSender.getSenderEmail(this))
            tvStatus.setTextColor(0xFF4CAF50)
        } else {
            tvStatus.setText("Not configured — tap to set up")
            tvStatus.setTextColor(0xFFAAAAAA)
        }
    }

    // ── Logout ─────────────────────────────────────────────────────────────

    fun confirmLogout( {
        AlertDialog.Builder(this)
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Log Out", (dialog, which) -> {
                    session.clearSession()
                    session.setLoggedIn(false)
                    var intent = Intent(this, Welcome::class.java)
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)
                  fun startActivity(intent); finish(); }) .setNegativeButton("Cancel", null) .show(); }  protected void onDestroy():  {
        super.onDestroy()
        if (dbHelper != null) dbHelper.close()
    }
}
