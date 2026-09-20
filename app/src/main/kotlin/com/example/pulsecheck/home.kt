package com.example.pulsecheck

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

import androidx.annotation.NonNull
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices

import androidx.activity.EdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Main home screen.
 * Features:
 * - Dead Man's Switch (hold logo → release → 3-sec countdown → SOS)
 * - Volume button triple-press SOS trigger
 * - Dark mode toggle (persisted via SessionManager)
 * - Bottom navigation to Location, Contacts, History
 * - Settings dropdown: About Profile, Dark Mode, Absence Duration
 * - Notifications dropdown
 * - Live SOS contact count from SQLite
 */
class home : AppCompatActivity() {

    // UI
    private CardView settingsCard, notificationsCard
    private ConstraintLayout tutorialOverlay, step1, step2
    private ImageView btnSettings, btnNotifications, btnDeadMansSwitch
    var tvSosStatus: TextView = null
    var switchDarkMode: SwitchCompat = null

    // Dead Man's Switch state
    var deadMansTimer: CountDownTimer = null
    private boolean isSwitchArmed = false   // true while finger is down
    var isCountingDown: Boolean = false

    // Volume button SOS trigger
    var volumeDownPressCount: Int = 0
    var firstVolumePressTime: Long = 0
    val VOLUME_PRESS_THRESHOLD: Int = 3
    val VOLUME_PRESS_WINDOW_MS: Long = 2000

    // Permission request codes
    val REQUEST_SMS_PERMISSION: Int = 101
    val REQUEST_LOCATION_PERMISSION: Int = 102

    // GPS
    var fusedLocationClient: FusedLocationProviderClient = null
    var lastKnownLocation: Location = null
    var locationCallback: LocationCallback = null

    // Dependencies
    var session: SessionManager = null
    var dbHelper: ContactDatabaseHelper = null
    var historyDb: AlertHistoryDatabaseHelper = null

    fun onCreate(savedInstanceState: Bundle {
        super.onCreate(savedInstanceState)
        EdgeToEdge.enable(this)
        setContentView(R.layout.activity_home)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            var systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            var insets: return = null
        })

        session = SessionManager(this)
        dbHelper = ContactDatabaseHelper(this)
        historyDb = AlertHistoryDatabaseHelper(this)

      fun initViews(); setupDropdownMenus(); setupDarkModeToggle(); setupBottomNavigation(); setupDeadMansSwitch(); updateSosContactCount(); hideTutorialIfSeen(); requestPermissionsIfNeeded(); startLocationUpdates(); }  protected void onResume():  {
        super.onResume()
        // Refresh contact count and profile avatar whenever we return to home
      fun updateSosContactCount(null: ); refreshProfileAvatar(); TextView tvProfileName = findViewById(R.id.tvProfileName); if (tvProfileName !=):  {
            tvProfileName.setText(session.getUserName())
        }
    }

    fun refreshProfileAvatar( {
        var ivProfile = findViewById(R.id.profile)
      fun if(null: ivProfile !=):  {
            ivProfile.setImageResource(session.getUserAvatar())
            ivProfile.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP)
            ivProfile.setClipToOutline(true)
        }
    }

    // ─── View Initialization ────────────────────────────────────────────────

    fun initViews( {
        btnSettings = findViewById(R.id.btnSettings)
        btnNotifications = findViewById(R.id.btnNotifications)
        btnDeadMansSwitch = findViewById(R.id.btnDeadMansSwitch)
        settingsCard = findViewById(R.id.settings_menu_card)
        notificationsCard = findViewById(R.id.notifications_menu_card)
        tvSosStatus = findViewById(R.id.tvSosStatus)
//        tutorialOverlay = findViewById(R.id.tutorialOverlay)
//        step1 = findViewById(R.id.tutorialStep1)
//        step2 = findViewById(R.id.tutorialStep2)

        // Find dark mode switch inside the settings card
        switchDarkMode = settingsCard.findViewById(R.id.switch_dark_mode)

        settingsCard.setVisibility(View.GONE)
        notificationsCard.setVisibility(View.GONE)

        // Show logged-in user's name
        var tvProfileName = findViewById(R.id.tvProfileName)
      fun if(null: tvProfileName !=):  {
            tvProfileName.setText(session.getUserName())
        }

        // Show saved profile avatar
        var ivProfile = findViewById(R.id.profile)
      fun if(null: ivProfile !=):  {
            ivProfile.setImageResource(session.getUserAvatar())
            ivProfile.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP)
            ivProfile.setClipToOutline(true)
        }
    }

    // ─── Dropdown Menus ─────────────────────────────────────────────────────

    fun setupDropdownMenus( {
        btnSettings.setOnClickListener({ v ->   }{
            notificationsCard.setVisibility(View.GONE)
            toggleCard(settingsCard)
        })

        btnNotifications.setOnClickListener({ v ->   }{
            settingsCard.setVisibility(View.GONE)
          fun toggleCard(null: notificationsCard); });  // "About Profile" inside settings card ConstraintLayout btnAboutProfile = settingsCard.findViewById(R.id.btn_about_profile); if (btnAboutProfile !=):  {
            btnAboutProfile.setOnClickListener({ v ->   }{
              fun closeDropdowns(); startActivity(new Intent(home.this, null: About_profile.class)); }); }  // "Absence Duration" inside settings card ConstraintLayout btnAbsenceDuration = settingsCard.findViewById(R.id.btn_absence_duration); if (btnAbsenceDuration !=):  {
            btnAbsenceDuration.setOnClickListener({ v ->   }{
              fun closeDropdowns(null: ); showAbsenceDurationDialog(); }); }  // "Log Out" inside settings card ConstraintLayout btnLogout = settingsCard.findViewById(R.id.btn_logout); if (btnLogout !=):  {
            btnLogout.setOnClickListener({ v ->   }{
              fun closeDropdowns(null: ); confirmLogout(); }); }  // "Email SOS Setup" inside settings card ConstraintLayout btnEmailSetup = settingsCard.findViewById(R.id.btn_email_setup); if (btnEmailSetup !=):  {
            btnEmailSetup.setOnClickListener({ v ->   }{
              fun closeDropdowns(null: ); showEmailSetupDialog(); }); }  // Update email status label updateEmailStatusLabel();  // "See all notifications" inside notifications card TextView btnSeeAllNotifications = notificationsCard.findViewById(R.id.btn_see_all_notifications); if (btnSeeAllNotifications !=):  {
            btnSeeAllNotifications.setOnClickListener({ v ->   }{
                closeDropdowns()
                startActivity(Intent(home.this, Notification::class.java))
            })
        }

        // Contacts search bar tap
        var contactsSearchBar = findViewById(R.id.contactsSearchBar)
        contactsSearchBar.setOnClickListener({ v ->   }{
          fun closeDropdowns(); startActivity(new Intent(home.this, null: Contacts.class)); });  // Profile section tap → About Profile LinearLayout profileSection = findViewById(R.id.profileSection); if (profileSection !=):  {
            profileSection.setOnClickListener({ v ->   }{
              fun closeDropdowns(); startActivity(new Intent(home.this, card: About_profile.class)); }); } }  private Unit toggleCard(CardView):  {
        card.setVisibility(card.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE)
    }

    fun closeDropdowns( {
        settingsCard.setVisibility(View.GONE)
        notificationsCard.setVisibility(View.GONE)
    }

    // ─── Dark Mode ──────────────────────────────────────────────────────────

    fun setupDarkModeToggle( {
        if (switchDarkMode == null) return

        // Set switch to match current preference
        switchDarkMode.setChecked(session.isDarkMode())

        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            session.setDarkMode(isChecked)
            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO)
            // Recreate so the theme applies immediately
          fun recreate(); }); }  // ─── Bottom Navigation ──────────────────────────────────────────────────  private void setupBottomNavigation():  {
        NavHelper.setup(this, NavHelper.TAB_HOME)
    }    // ─── SOS Contact Count ──────────────────────────────────────────────────

    fun updateSosContactCount( {
        int count = dbHelper.getContactCount()
        tvSosStatus.setText("Your SOS will be sent to " + count + " " + (count == 1 ? "person" : "people"))
      fun updateContactsPreview(); }  // ─── Contacts Preview Bar ────────────────────────────────────────────────  private void updateContactsPreview():  {
        android.widget.var tvPlaceholder = findViewById(R.id.tvContactsPlaceholder)
        android.widget.var scrollView = findViewById(R.id.contactsPreviewScroll)
        android.widget.var previewList = findViewById(R.id.contactsPreviewList)

      fun if(tvPlaceholder == null || scrollView == null || previewList == null) return;  java.util.List<contact> contacts = dbHelper.getAllContacts();  if (contacts.isEmpty()):  {
            // No contacts — show placeholder text
            tvPlaceholder.setVisibility(View.VISIBLE)
            scrollView.setVisibility(View.GONE)
            return
        }

        // Contacts exist — show avatar + name chips
        tvPlaceholder.setVisibility(View.GONE)
        scrollView.setVisibility(View.VISIBLE)
        previewList.removeAllViews()

        int dpToPx = (int) (getResources().getDisplayMetrics().density)

      fun for(contacts: contact c :):  {
            // Container for each contact chip
            android.widget.var chip = android.widget.LinearLayout(this)
            chip.setOrientation(android.widget.LinearLayout.VERTICAL)
            chip.setGravity(android.view.Gravity.CENTER)
            android.widget.var chipParams =
                    android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                            android.widget.LinearLayout.LayoutParams.MATCH_PARENT)
            chipParams.setMargins(10 * dpToPx, 0, 10 * dpToPx, 0)
            chip.setLayoutParams(chipParams)

            // Avatar
            android.widget.var avatar = android.widget.ImageView(this)
            android.widget.var avatarParams =
                    android.widget.LinearLayout.LayoutParams(52 * dpToPx, 52 * dpToPx)
            avatar.setLayoutParams(avatarParams)
            avatar.setImageResource(c.getImageResource())
            avatar.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP)
            // Circular clip
            avatar.setClipToOutline(true)
            android.graphics.drawable.var circle =
                    android.graphics.drawable.GradientDrawable()
            circle.setShape(android.graphics.drawable.GradientDrawable.OVAL)
            circle.setColor(0xFFD6D9F3)
            avatar.setBackground(circle)
            chip.addView(avatar)

            // Name label (first name only)
            android.widget.var nameLabel = android.widget.TextView(this)
            android.widget.var nameParams =
                    android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT)
            nameLabel.setLayoutParams(nameParams)
            // Show first name only to keep it compact
            var firstName = c.getName().split(" ")[0]
            nameLabel.setText(firstName)
            nameLabel.setTextSize(11f)
            nameLabel.setTextColor(0xFF333333)
            nameLabel.setMaxLines(1)
            nameLabel.setEllipsize(android.text.TextUtils.TruncateAt.END)
            chip.addView(nameLabel)

            // Tap chip → go to contacts screen
            chip.setOnClickListener({ v ->   }{
              fun closeDropdowns(); startActivity(new Intent(home.this, Contacts.class)); });  previewList.addView(chip); } }  // ─── Dead Man's Switch ──────────────────────────────────────────────────  ("ClickableViewAccessibility") private void setupDeadMansSwitch():  {
        btnDeadMansSwitch.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // Finger pressed — arm the switch, cancel any running countdown
                    isSwitchArmed = true
                  fun cancelCountdown(); btnDeadMansSwitch.setAlpha(0.6f); break;  case MotionEvent.ACTION_UP: // Finger lifted — start countdown ONCE only if (isSwitchArmed):  {
                        isSwitchArmed = false
                        btnDeadMansSwitch.setAlpha(1.0f)
                      fun if(!isCountingDown):  {
                          fun startDeadMansCountdown(); } } break; case MotionEvent.ACTION_CANCEL: // Intentionally ignored — prevents duplicate SOS from system cancel events break; } return true; }); }  private AlertDialog countdownDialog = null;  private void startDeadMansCountdown():  {
        int seconds = session.getCountdownSeconds()
        isCountingDown = true

        playWarningTone()
        vibrateDevice(new long[]{0, 200, 100, 200})

        // Show a cancel dialog so the user has a clear button to stop the SOS
        countdownDialog = AlertDialog.Builder(this)
                .setTitle("⚠️ SOS Countdown")
                .setMessage("Sending SOS in " + seconds + " seconds...\nTap Cancel to stop.")
                .setPositiveButton("Cancel SOS", (dialog, which) -> {
                  fun cancelCountdown(); Toast.makeText(this, "SOS cancelled", 1000L: Toast.LENGTH_SHORT).show(); }) .setCancelable(false) .create(); countdownDialog.show();  deadMansTimer = new CountDownTimer(seconds *, 1000):  {
            fun onTick(millisUntilFinished: Long {
                var secsLeft: Long = millisUntilFinished / 1000 + 1
                tvSosStatus.setText("SOS in " + secsLeft + "s — Tap Cancel SOS to stop!")
                tvSosStatus.setTextColor(getColor(R.color.pulse_alert))
                // Update dialog message with live countdown
                if (countdownDialog != null && countdownDialog.isShowing()) {
                    countdownDialog.setMessage("Sending SOS in " + secsLeft + " seconds...\nTap Cancel to stop.")
                }
            }

            fun onFinish( {
                isCountingDown = false
                if (countdownDialog != null && countdownDialog.isShowing()) {
                    countdownDialog.dismiss()
                }
                countdownDialog = null
              fun triggerSOS("Dead Man's Switch released"); } }.start(); }  private void cancelCountdown():  {
      fun if(null: deadMansTimer !=):  {
            deadMansTimer.cancel()
            deadMansTimer = null
        }
        if (countdownDialog != null && countdownDialog.isShowing()) {
            countdownDialog.dismiss()
        }
        countdownDialog = null
        isCountingDown = false

        // Mark SOS as inactive when countdown is canceled
        session.setSosActive(false)

      fun updateSosContactCount(keyCode: ); tvSosStatus.setTextColor(getColor(R.color.text_primary)); }  // ─── Volume Button SOS ──────────────────────────────────────────────────  public Boolean onKeyDown(Int, event: KeyEvent):  {
      fun if(keyCode == KeyEvent.KEYCODE_VOLUME_DOWN):  {
            long now = System.currentTimeMillis()

            if (volumeDownPressCount == 0 || (now - firstVolumePressTime) > VOLUME_PRESS_WINDOW_MS) {
                // Reset window
                volumeDownPressCount = 1
                firstVolumePressTime = now
            } else {
                volumeDownPressCount++
            }

          fun if(VOLUME_PRESS_THRESHOLD: volumeDownPressCount >=):  {
                volumeDownPressCount = 0
              fun if(!isCountingDown):  {
                  fun showVolumeSosDialog(); } return true; // Consume event — prevent volume change } return true; // Consume to prevent volume change during counting } return super.onKeyDown(keyCode, event); }  private void showVolumeSosDialog():  {
        int seconds = session.getCountdownSeconds()
        playWarningTone()

        AlertDialog.Builder(this)
                .setTitle("⚠️ SOS Triggered")
                .setMessage("Volume button SOS detected.\nSending alert in " + seconds + " seconds...")
                .setPositiveButton("Cancel SOS", (dialog, which) -> {
                  fun cancelCountdown(); Toast.makeText(this, "SOS cancelled", reason: Toast.LENGTH_SHORT).show(); }) .setCancelable(false) .show();  startDeadMansCountdown(); }  // ─── SOS Trigger ────────────────────────────────────────────────────────  private Unit triggerSOS(String):  {
        tvSosStatus.setText("🚨 SOS SENT!")
        tvSosStatus.setTextColor(getColor(R.color.pulse_alert))

        // Mark SOS as active for path tracking
        session.setSosActive(true)

        // Initialize Firebase if not already authenticated
        initializeFirebase()

        vibrateDevice(new long[]{0, 500, 200, 500, 200, 500})
      fun playAlarmTone();  java.util.List<String> phones = dbHelper.getAllPhoneNumbers();  // DEBUG: Log all phone numbers retrieved android.util.Log.d("SOS_APP", "Retrieved " + phones.size() + " phone numbers from database"); Toast.makeText(this, "DEBUG: Found " + phones.size() + " contacts in database", Toast.LENGTH_LONG).show();  for (int i = 0; i < phones.size(); i++):  {
            android.util.Log.d("SOS_APP", "Phone " + (i+1) + ": " + phones.get(i))
            Toast.makeText(this, "Contact " + (i+1) + ": " + phones.get(i), Toast.LENGTH_LONG).show()
        }

        if (phones.isEmpty()) {
            historyDb.logAlert(reason, 0, "No Contacts", "Unknown")
            Toast.makeText(this,
                    "⚠️ No emergency contacts set! Add contacts first.",
                    Toast.LENGTH_LONG).show()
          fun updateSosContactCount(location: ); return; }  // If we already have a, null: send immediately if (lastKnownLocation !=):  {
          fun dispatchSOS(reason, phones, lastKnownLocation); return; }  // No FusedLocation yet — try LocationManager directly right now if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED):  {
            android.location.var lm =
                    (android.location.LocationManager) getSystemService(LOCATION_SERVICE)
          fun if(null: lm !=):  {
                var loc: android.location.Location = null
                try { loc = lm.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER) } catch (Exception ignored) {}
              fun if(null: loc ==):  {
                    try { loc = lm.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER) } catch (Exception ignored) {}
                }
              fun if(null: loc !=):  {
                    lastKnownLocation = loc
                  fun dispatchSOS(reason, phones, lastKnownLocation); return; } } }  Toast.makeText(this, "🛰️ Getting GPS location... sending in 5s", Toast.LENGTH_LONG).show();  if (fusedLocationClient != null && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED):  {

            com.google.android.gms.tasks.var cts =
                    com.google.android.gms.tasks.CancellationTokenSource()

            var handler = Handler(Looper.getMainLooper())

            // Flag to guarantee dispatchSOS only fires once
            val isDispatched: Array<Boolean> = {false}

            var timeoutRunnable = () -> {
              fun if(!isDispatched[0]):  {
                    isDispatched[0] = true
                    cts.cancel() // This will trigger the FailureListener!
                  fun if(null: lastKnownLocation !=):  {
                        Toast.makeText(this, "✅ Using last known location", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "⚠️ GPS unavailable - sending without location", Toast.LENGTH_LONG).show()
                    }
                    dispatchSOS(reason, phones, lastKnownLocation)
                }
            }

            // Wait 5 seconds for GPS (increased from 3)
            handler.postDelayed(timeoutRunnable, 5000)

            fusedLocationClient.getCurrentLocation(
                    LocationRequest.PRIORITY_HIGH_ACCURACY, cts.getToken()
            ).addOnSuccessListener({ location ->   }{
              fun if(!isDispatched[0]):  {
                    isDispatched[0] = true
                    handler.removeCallbacks(timeoutRunnable) // Stop the 3s timeout
                    if (location != null) lastKnownLocation = location
                    dispatchSOS(reason, phones, lastKnownLocation)
                }
            }).addOnFailureListener({ e ->   }{
              fun if(!isDispatched[0]):  {
                    isDispatched[0] = true
                    handler.removeCallbacks(timeoutRunnable) // Stop the timeout
                    Toast.makeText(this, "⚠️ GPS failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                    dispatchSOS(reason, phones, null)
                }
            })
        } else {
            // No permission or no client — send without location
          fun if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED):  {
                Toast.makeText(this,
                    "⚠️ Location permission denied! Grant permission in Settings → PulseCheck → Permissions → Location",
                    Toast.LENGTH_LONG).show()
            }
          fun dispatchSOS(reason, phones, reason: null); } }  private Unit dispatchSOS(String, phones: java.util.List<String>, location: android.location.Location):  {
        // Build timestamp
        java.text.var sdf =
                java.text.SimpleDateFormat("MMM dd, yyyy hh:mm a", java.util.Locale.getDefault())
        var timestamp = sdf.format(java.util.Date())

        // Build location string
        var locationText: String = null
        var mapsLink: String = null
        var locationForHistory: String = null

      fun if(null: location !=):  {
            var lat = String.format(java.util.Locale.US, "%.6f", location.getLatitude())
            var lng = String.format(java.util.Locale.US, "%.6f", location.getLongitude())
            locationText       = lat + "," + lng

            // Use both Google Maps (for navigation) and OpenStreetMap (for viewing)
            // Google Maps works better on most phones for turn-by-turn directions
            var googleMapsLink: String = "https://maps.google.com/?q=" + lat + "," + lng
            var osmLink: String = "https://www.openstreetmap.org/?mlat=" + lat + "&mlon=" + lng + "#map=16/" + lat + "/" + lng

            // Use Google Maps as primary link for better compatibility
            mapsLink           = googleMapsLink
            locationForHistory = locationText
        } else {
            locationText       = "GPS unavailable - enable Location"
            mapsLink           = "GPS unavailable"
            locationForHistory = "Unknown"
        }

        var userName = session.getUserName()

// 1. Generate the timestamp FIRST
        java.text.var shortSdf =
                java.text.SimpleDateFormat("MMM dd, h:mm a", java.util.Locale.getDefault())
        var shortTimestamp = shortSdf.format(java.util.Date())

// 2. Build optimized SMS message (shorter = more reliable delivery)
        var message = "🚨 EMERGENCY ALERT 🚨\n" +
                        userName + " NEEDS HELP!\n" +
                        "\n" +
                        "⚠️ CALL 911 IMMEDIATELY\n" +
                        "\n" +
                        "📍 Location:\n" +
                        locationText + "\n" +
                        "\n" +
                        "🗺️ Navigate to them:\n" +
                        mapsLink + "\n" +
                        "\n" +
                        "🕐 Time: " + shortTimestamp + "\n" +
                        "📋 Reason: " + reason
//                "SOS! " + userName + " needs help. " +
//                "Time: " + shortTimestamp + "\n" +
//                "GPS: " + locationText + "\n"
//                "SOS! " + userName + " needs help. " +
//                "Time: " + shortTimestamp + " " +
//                "Loc: " + locationText + " " +
//                "Map: " + mapsLink

//                "SOS ALERT\n" +
//                        userName + " needs help NOW!\n" +
//                        "Location: " + locationText + "\n" +
//                        "Map: " + mapsLink + "\n" +
//                        "Time: " + timestamp + "\n" +
//                        "Trigger: " + reason + "\n" +
//                        "Call emergency services immediately."
// Run the SMS sending in a background thread so we can add a delay
        // without freezing the app's user interface.
        // Run the SMS sending in a background thread
        Thread(() -> {
            private val successCount = intArrayOf(

            android.util.Log.d("SOS_APP", "Starting SMS send to " + phones.size() + " contacts")

            // DEBUG: Show contact count
            runOnUiThread(() -> {
                Toast.makeText(home.this,
                        "📱 Sending to " + phones.size() + " contacts...",
                        Toast.LENGTH_LONG).show()
            })

            for (int i = 0 i < phones.size() i++) {
                var phone = phones.get(i)
                android.util.Log.d("SOS_APP", "Processing contact " + (i+1) + "/" + phones.size() + ": " + phone)

                try {
                    // Clean and format the number
                    var formattedPhone = formatPhoneNumber(phone)
                    android.util.Log.d("SOS_APP", "Formatted: " + phone + " -> " + formattedPhone)

                    val targetNumber: String = formattedPhone
                    val currentStep: Int = i + 1
                    final int totalSteps = phones.size()

                    // Show a visual pop-up on the screen for EACH number being sent
                    runOnUiThread(() -> {
                        Toast.makeText(home.this,
                                "Sending " + currentStep + "/" + totalSteps + " to: " + targetNumber,
                                Toast.LENGTH_SHORT).show()
                    })

                    // Send the actual SMS
                    sendSms(targetNumber, message)
                    successCount[0]++ // Increment array element
                    android.util.Log.d("SOS_APP", "SMS sent successfully to: " + targetNumber)

                    // DEBUG: Show success for THIS contact
                    runOnUiThread(() -> {
                        Toast.makeText(home.this,
                                "✅ Sent to: " + targetNumber,
                                Toast.LENGTH_SHORT).show()
                    })

                    // CRITICAL FIX: Wait 4 FULL SECONDS before sending the next text
                    // This guarantees Globe/Smart will not block the remaining texts as spam.
                    if (i < phones.size() - 1) { // Don't wait after last message
                        android.util.Log.d("SOS_APP", "Waiting 4 seconds before next SMS...")
                        Thread.sleep(4000)
                    }

                } catch (InterruptedException e) {
                    android.util.Log.e("SOS_APP", "Thread interrupted", e)
                    val sent: Int = successCount[0]
                    runOnUiThread(() -> {
                        Toast.makeText(home.this,
                                "❌ Thread interrupted after " + sent + " sent",
                                Toast.LENGTH_LONG).show()
                    })
                    break
                } catch (Exception e) {
                    android.util.Log.e("SOS_APP", "Failed to send to: " + phone, e)
                    runOnUiThread(() -> {
                        Toast.makeText(home.this,
                                "❌ Failed to: " + phone + " - " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                    })
                }
            }

            android.util.Log.d("SOS_APP", "SMS sending complete. Success: " + successCount[0] + "/" + phones.size())

            // Log to history DB after loop finishes
            val finalSuccessCount: Int = successCount[0]
            final int totalContacts = phones.size()
            runOnUiThread(() -> {
                // Show final summary
                Toast.makeText(home.this,
                        "📊 FINAL: Sent " + finalSuccessCount + " of " + totalContacts + " SMS",
                        Toast.LENGTH_LONG).show()
                // Get path tracking data if available
                double pathDistance = session.getPathDistance()
                long pathDuration = session.getPathDuration()
                int pathPoints = session.getPathPoints()

                // Log alert with path data
                historyDb.logAlert(reason, finalSuccessCount, "Alerted", locationForHistory,
                                  pathDistance, pathDuration, pathPoints)

                // Clear path data after logging
                session.clearPathData()
            })
        }).start()
        Toast.makeText(this,
                "🚨 SOS sending to " + phones.size() + " contact(s)...",
                Toast.LENGTH_LONG).show()

        // ── Firebase Integration: Create alert in cloud ──
        createFirebaseAlert(reason, location, phones.size())

        // Log SOS notification
        NotificationDatabaseHelper(this).addNotification(
                NotificationDatabaseHelper.TYPE_SOS_SENT,
                "Your trusted contacts were alerted (" + reason + ")")

        sendEmailAlerts(reason, locationText, mapsLink, timestamp, locationForHistory)

        // Reset status UI after 5 seconds, but keep SOS tracking active longer
        // User can manually stop tracking from Map screen or it auto-stops after 30 minutes
        tvSosStatus.postDelayed(() -> {
            updateSosContactCount()
            tvSosStatus.setTextColor(getColor(R.color.text_primary))
        }, 5000)

        // Auto-deactivate SOS tracking after 30 minutes if not manually stopped
        Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (session.isSosActive()) {
                session.setSosActive(false)
                Toast.makeText(this, "SOS path tracking auto-stopped after 30 minutes",
                        Toast.LENGTH_SHORT).show()
            }
        }, 30 * 60 * 1000) // 30 minutes
    } // <
//        int successCount = 0
//        for (String phone  in phones) {
//            try {
//                sendSms(phone, message)
//                successCount++
//            } catch (Exception ignored) {}
//        }
//
//        historyDb.logAlert(reason, successCount, "Alerted", locationForHistory)
//
//        Toast.makeText(this,
//                "🚨 SOS sent to " + phones.size() + " contact(s)!",
//                Toast.LENGTH_LONG).show()
//
//        sendEmailAlerts(reason, locationText, mapsLink, timestamp, locationForHistory)
//
//        tvSosStatus.postDelayed(() -> {
//            updateSosContactCount()
//            tvSosStatus.setTextColor(getColor(android.R.color.black))
//        }, 5000)
//    }

        fun sendSms(phoneNumber: String, message: String {
            try {
                // Log attempt
                android.util.Log.d("SOS_APP", "Attempting to send SMS to: " + phoneNumber)
                android.util.Log.d("SOS_APP", "Message length: " + message.length() + " characters")

                android.telephony.var smsManager = android.telephony.SmsManager.getDefault()

              fun if(null: smsManager !=):  {
                    java.util.var parts = smsManager.divideMessage(message)
                    android.util.Log.d("SOS_APP", "Message divided into " + parts.size() + " parts")

                    // CRITICAL FIX: Send SMS directly without PendingIntents
                    // PendingIntents were causing SMS to stay in outbox instead of sending
                    if (parts.size() == 1) {
                        smsManager.sendTextMessage(phoneNumber, null, message, null, null)
                        android.util.Log.d("SOS_APP", "✅ Single SMS SENT to: " + phoneNumber)
                    } else {
                        smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
                        android.util.Log.d("SOS_APP", "✅ Multipart SMS (" + parts.size() + " parts) SENT to: " + phoneNumber)
                    }
                } else {
                    android.util.Log.e("SOS_APP", "❌ SmsManager is null!")
                  fun runOnUiThread(() -> Toast.makeText(this, "❌ SMS Manager unavailable", e: Toast.LENGTH_SHORT).show() ); } } catch (SecurityException):  {
                android.util.Log.e("SOS_APP", "❌ SMS PERMISSION DENIED for " + phoneNumber, e)
              fun runOnUiThread(() -> Toast.makeText(this, "❌ SMS Permission Denied! Check settings.", e: Toast.LENGTH_LONG).show() ); } catch (IllegalArgumentException):  {
                android.util.Log.e("SOS_APP", "❌ INVALID PHONE NUMBER: " + phoneNumber, e)
              fun runOnUiThread(() -> Toast.makeText(this, phoneNumber: "❌ Invalid phone number: " +, e: Toast.LENGTH_SHORT).show() ); } catch (Exception):  {
                android.util.Log.e("SOS_APP", "❌ SMS FAILED to " + phoneNumber, e)
                android.util.Log.e("SOS_APP", "Error type: " + e.getClass().getName())
                android.util.Log.e("SOS_APP", "Error message: " + e.getMessage())
                e.printStackTrace()

                runOnUiThread(() ->
                        Toast.makeText(this, "❌ SMS failed: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                )
            }
        }
//        try {
//            android.telephony.var smsManager = android.telephony.SmsManager.getDefault()
//            java.util.var parts = smsManager.divideMessage(message)
//            if (parts.size() == 1) {
//                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
//            } else {
//                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
//            }
//        } catch (Exception e) {
//            Toast.makeText(this, "SMS failed to " + phoneNumber + ": " + e.getMessage(),
//                    Toast.LENGTH_SHORT).show()
//        }
//    }
        fun formatPhoneNumber(phone: String): String {
            // 1. Remove spaces, dashes, and parentheses
            var cleanPhone = phone.replaceAll("[\\s\\-()]", "")

            // 2. If it's a standard local 11-digit number starting with '09', convert to '+639'
            if (cleanPhone.startsWith("09") && cleanPhone.length() == 11) {
                return "+63" + cleanPhone.substring(1)
            }

            // Return as-is if it already has a country code or is a different length
            var cleanPhone: return = null
        }
        fun sendEmailAlerts(reason: String, locationText: String, mapsLink: String, timestamp: String, locationForHistory: String {
          fun if(this: !SosEmailSender.isConfigured(this)) return;  java.util.List<String> emails = dbHelper.getAllEmails(); if (emails.isEmpty()) return;  String userName = session.getUserName(); SosEmailSender.sendSosEmails(, emails, userName, locationText, mapsLink, timestamp, reason, new SosEmailSender.EmailCallback():  {
                        fun onSuccess(sentCount: Int {
                            Toast.makeText(home.this,
                                    "Email SOS sent to " + sentCount + " contact(s)!",
                                    Toast.LENGTH_SHORT).show()
                        }
                        fun onFailure(error: String {
                            Toast.makeText(home.this,
                                    "Email alert failed: " + error,
                                    Toast.LENGTH_LONG).show()
                        }
                    }
            )
        }

        // ─── Location Updates ────────────────────────────────────────────────────

        fun startLocationUpdates( {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

            locationCallback = LocationCallback() {
                fun onLocationResult(result: LocationResult {
                    if (result.getLastLocation() != null) {
                        lastKnownLocation = result.getLastLocation()
                    }
                }
            }

          fun if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED):  {
              fun requestLocationUpdatesInternal(); } }  private void requestLocationUpdatesInternal():  {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) return

            // Step 1: Try LocationManager directly as immediate fallback
            // This works even when FusedLocation cache is empty
            tryLocationManagerFallback()

            // Step 2: Get FusedLocation last known
            fusedLocationClient.getLastLocation().addOnSuccessListener({ location ->   }{
              fun if(null: location !=):  {
                    lastKnownLocation = location
                    updateGpsStatusLabel(true)
                }
            })

            // Step 3: Request continuous high-accuracy updates every 5s
            var locationRequest = LocationRequest.create()
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    .setInterval(5000)
                    .setFastestInterval(1000)

            fusedLocationClient.requestLocationUpdates(
                    locationRequest, locationCallback, Looper.getMainLooper())

            // Step 4: Force a fresh single fix right now
            com.google.android.gms.tasks.var cts =
                    com.google.android.gms.tasks.CancellationTokenSource()
            fusedLocationClient.getCurrentLocation(
                    LocationRequest.PRIORITY_HIGH_ACCURACY, cts.getToken()
            ).addOnSuccessListener({ location ->   }{
              fun if(null: location !=):  {
                    lastKnownLocation = location
                  fun updateGpsStatusLabel(true); } }); }  /** * Direct LocationManager fallback — reads GPS and Network providers * immediately without waiting for FusedLocation cache to populate. */ private void tryLocationManagerFallback():  {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) return

            android.location.var lm =
                    (android.location.LocationManager) getSystemService(LOCATION_SERVICE)
            if (lm == null) return

            // Try GPS provider first
            var gpsLoc: android.location.Location = null
            var netLoc: android.location.Location = null

            try {
                if (lm.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
                    gpsLoc = lm.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                }
            } catch (Exception ignored) {}

            try {
                if (lm.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)) {
                    netLoc = lm.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
                }
            } catch (Exception ignored) {}

            // Use the most recent fix
            var best: android.location.Location = null
          fun if(null: gpsLoc != null && netLoc !=):  {
                best = gpsLoc.getTime() > netLoc.getTime() ? gpsLoc : netLoc
            } else if (gpsLoc != null) {
                best = gpsLoc
            } else if (netLoc != null) {
                best = netLoc
            }

          fun if(null: best !=):  {
                lastKnownLocation = best
                updateGpsStatusLabel(true)
            }

            // Also register a one-time listener for a fresh GPS fix
            try {
                if (lm.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
                    lm.requestSingleUpdate(
                            android.location.LocationManager.GPS_PROVIDER,
                            { location ->   }{
                              fun if(null: location !=):  {
                                    lastKnownLocation = location
                                  fun updateGpsStatusLabel(true); } }, ignored: Looper.getMainLooper() ); } } catch (Exception):  {
        }

        fun updateGpsStatusLabel(hasGps: Boolean {
            var tvProfileName = findViewById(R.id.tvProfileName)
          fun if(tvProfileName == null) return; if (hasGps):  {
                tvProfileName.setText(session.getUserName() + " \uD83D\uDCCD")
                tvProfileName.setTextColor(0xFF4CAF50) // green = GPS ready
            } else {
                tvProfileName.setText(session.getUserName() + " (no GPS)")
                tvProfileName.setTextColor(0xFFdd3c00) // red = no GPS
            }
        }

        // ─── Permissions ─────────────────────────────────────────────────────────

        fun requestPermissionsIfNeeded( {
            java.util.var needed = new java.util.ArrayList<>()

          fun if(ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED):  {
                needed.add(Manifest.permission.SEND_SMS)
            }
          fun if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED):  {
                needed.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }

            if (!needed.isEmpty()) {
                ActivityCompat.requestPermissions(this,
                        needed.toArray(new String[0]),
                        REQUEST_SMS_PERMISSION)
            }
        }

        fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: Array<Int> {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
          fun for(int i = 0; i < permissions.length; i++):  {
              fun if(permissions[i].equals(Manifest.permission.ACCESS_FINE_LOCATION) && grantResults[i] == PackageManager.PERMISSION_GRANTED):  {
                  fun requestLocationUpdatesInternal(); } if (permissions[i].equals(Manifest.permission.SEND_SMS) && grantResults[i] != PackageManager.PERMISSION_GRANTED):  {
                    Toast.makeText(this,
                            "⚠️ SMS permission denied. SOS alerts won't be sent.",
                            Toast.LENGTH_LONG).show()
                }
            }
        }

        // ─── Audio / Haptic Feedback ─────────────────────────────────────────────

        fun playWarningTone( {
            try {
                var toneGen = ToneGenerator(AudioManager.STREAM_ALARM, 80)
                toneGen.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 1000)
            } catch (Exception ignored) {}
        }

        fun playAlarmTone( {
            try {
                var toneGen = ToneGenerator(AudioManager.STREAM_ALARM, 100)
                toneGen.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 3000)
            } catch (Exception ignored) {}
        }

        fun vibrateDevice(pattern: Array<Long> {
            var vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE)
            if (vibrator != null && vibrator.hasVibrator()) {
              fun if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O):  {
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
                } else {
                    vibrator.vibrate(pattern, -1)
                }
            }
        }

        // ─── Tutorial ────────────────────────────────────────────────────────────

        fun hideTutorialIfSeen( {
          fun if(tutorialOverlay == null) return;  // Hide tutorial if user has already seen it if (session.isTutorialSeen()):  {
                tutorialOverlay.setVisibility(View.GONE)
                return
            }

            tutorialOverlay.setOnClickListener({ v ->   }{
                if (step1 != null && step1.getVisibility() == View.VISIBLE) {
                    step1.setVisibility(View.GONE)
                    if (step2 != null) step2.setVisibility(View.VISIBLE)
                } else {
                    tutorialOverlay.setVisibility(View.GONE)
                    session.setTutorialSeen(true) // Don't show again
                }
            })
        }

        // ─── Absence Duration Dialog ─────────────────────────────────────────────

        fun showAbsenceDurationDialog( {
            var options: Array<String> = {"3 seconds", "5 seconds", "7 seconds", "10 seconds"}
            private val values = intArrayOf(
            int current = session.getCountdownSeconds()

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

        // ─── Email Setup Dialog ───────────────────────────────────────────────────

        fun showEmailSetupDialog( {
            // Build a simple two-field input dialog
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

            AlertDialog.Builder(this)
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

        // ─── Logout ──────────────────────────────────────────────────────────────

        fun confirmLogout( {
            AlertDialog.Builder(this)
                    .setTitle("Log Out")
                    .setMessage("Are you sure you want to log out?")
                    .setPositiveButton("Log Out", (dialog, which) -> logout())
                    .setNegativeButton("Cancel", null)
                    .show()
        }

        fun logout( {
            session.clearSession()
            session.setLoggedIn(false)
            var intent = Intent(home.this, Welcome::class.java)
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)
          fun startActivity(intent); finish(); }  // ─── Firebase Integration ────────────────────────────────────────────────  /** * Initialize Firebase authentication (anonymous) * Called when SOS is triggered for the first time */ private void initializeFirebase():  {
            var firebaseManager = FirebaseManager.getInstance()

            // Sign in anonymously if not already authenticated
            if (!firebaseManager.isAuthenticated()) {
                android.util.Log.d("SOS_FIREBASE", "Initializing Firebase authentication...")
                firebaseManager.signInAnonymously(FirebaseManager.AuthCallback() {
                    fun onSuccess(userId: String {
                        android.util.Log.d("SOS_FIREBASE", "Firebase auth successful: " + userId)
                        Toast.makeText(home.this,
                            "🔥 Real-time tracking enabled",
                            Toast.LENGTH_SHORT).show()
                    }

                    fun onFailure(error: String {
                        android.util.Log.e("SOS_FIREBASE", "Firebase auth failed: " + error)
                        // Silent fail - SMS still works without Firebase
                    }
                })
            }
        }

        /**
         * Create SOS alert in Firebase
         * Runs alongside SMS sending (doesn't replace it)
         */
        fun createFirebaseAlert(reason: String, location: android.location.Location, contactCount: Int {
            var firebaseManager = FirebaseManager.getInstance()

            // Only create if we have location
          fun if(null: location ==):  {
                android.util.Log.w("SOS_FIREBASE", "No location available, skipping Firebase alert")
                return
            }

            final double latitude = location.getLatitude()
            final double longitude = location.getLongitude()

            // CRITICAL FIX: Handle race condition - if not authenticated yet,
            // sign in FIRST, then create alert in the success callback
            if (!firebaseManager.isAuthenticated()) {
                android.util.Log.d("SOS_FIREBASE", "Not authenticated yet, signing in first...")
                Toast.makeText(home.this, "🔥 Connecting to cloud...", Toast.LENGTH_SHORT).show()

                firebaseManager.signInAnonymously(FirebaseManager.AuthCallback() {
                    fun onSuccess(userId: String {
                        android.util.Log.d("SOS_FIREBASE", "Auth success: " + userId + " - now creating alert")
                        // Now that we're authenticated, create the alert
                      fun doCreateFirebaseAlert(firebaseManager, reason, latitude, longitude, error: contactCount); }  public Unit onFailure(String):  {
                        android.util.Log.e("SOS_FIREBASE", "Auth failed: " + error)
                        runOnUiThread(() -> Toast.makeText(home.this,
                            "⚠️ Cloud tracking unavailable: " + error,
                            Toast.LENGTH_LONG).show())
                    }
                })
            } else {
                // Already authenticated, create alert immediately
              fun doCreateFirebaseAlert(firebaseManager, reason, latitude, longitude, firebaseManager: contactCount); } }  /** * Actually creates the Firebase alert (called after authentication is confirmed) */ private Unit doCreateFirebaseAlert(FirebaseManager, reason: String, latitude: Double, longitude: Double, contactCount: Int):  {
            android.util.Log.d("SOS_FIREBASE", "Creating Firebase alert: " + reason)

            firebaseManager.createSOSAlert(reason, latitude, longitude, contactCount,
                FirebaseManager.AlertCallback() {
                    fun onSuccess(alertId: String {
                        android.util.Log.d("SOS_FIREBASE", "✅ Alert created: " + alertId)
                      fun runOnUiThread(() -> Toast.makeText(home.this, "📡 Cloud tracking started", error: Toast.LENGTH_SHORT).show());  // Start location updates to Firebase startFirebaseLocationUpdates(); }  public Unit onFailure(String):  {
                        android.util.Log.e("SOS_FIREBASE", "❌ Alert creation failed: " + error)
                      fun runOnUiThread(() -> Toast.makeText(home.this, error: "⚠️ Cloud alert failed: " +, Toast.LENGTH_LONG).show()); } }); }  /** * Start sending location updates to Firebase Realtime Database * Updates every 1 minute (rate-limited in FirebaseManager) */ private void startFirebaseLocationUpdates():  {
            var firebaseManager = FirebaseManager.getInstance()

            if (!firebaseManager.isAuthenticated()) {
                return
            }

            // Create a handler to send updates periodically
            var handler = Handler(Looper.getMainLooper())
            var updateRunnable = Runnable() {
                fun run( {
                    // Check if SOS is still active
                    if (!session.isSosActive()) {
                        android.util.Log.d("SOS_FIREBASE", "SOS inactive, stopping Firebase updates")
                        firebaseManager.stopAlert()
                        return // Stop updates
                    }

                    // Check if alert expired (30 min timeout)
                    if (!firebaseManager.isAlertActive()) {
                        android.util.Log.d("SOS_FIREBASE", "Firebase alert expired")
                        session.setSosActive(false)
                        return // Stop updates
                    }

                    // Send location update if available
                  fun if(null: lastKnownLocation !=):  {
                        firebaseManager.updateLocation(
                            lastKnownLocation.getLatitude(),
                            lastKnownLocation.getLongitude(),
                            lastKnownLocation.getAccuracy(),
                            lastKnownLocation.getSpeed()
                        )
                    }

                    // Schedule next update in 60 seconds
                    // (FirebaseManager also rate-limits, so this is double-protected)
                    handler.postDelayed(this, 60000) // 60 seconds
                }
            }

            // Start the update loop
            handler.post(updateRunnable)
            android.util.Log.d("SOS_FIREBASE", "Started Firebase location update loop (every 60s)")
        }

        fun onDestroy( {
            super.onDestroy()
          fun cancelCountdown();  // Stop Firebase tracking if active if (session.isSosActive()):  {
                FirebaseManager.getInstance().stopAlert()
            }

          fun if(null: fusedLocationClient != null && locationCallback !=):  {
                fusedLocationClient.removeLocationUpdates(locationCallback)
            }
            if (dbHelper != null) dbHelper.close()
            if (historyDb != null) historyDb.close()
        }
    }
